package cache.policy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import cache.Cache;
import cache.demand.DemandTracker;
import cache.frequency.Counter;
import cache.utils.Tracer;
import common.LogUtil;
import commonCore.CoreConstants;
import tenants.Tenant;
import tenants.TenantGroup;

public class FairCachePolicy implements Policy {

    private final Map<String, Cache> tenantCacheMap;
    private final Map<String, Tenant> tenantMap;
    private final TenantGroup tenantGroup;
    private final Map<String, ArrayList<String>> tenantEvictablesMap;

    public FairCachePolicy(TenantGroup tenantGroup) {

        this.tenantGroup = tenantGroup;
        this.tenantCacheMap = this.tenantGroup.getTenantCacheMap();
        this.tenantMap = this.tenantGroup.getTenantsMap();
        this.tenantEvictablesMap = new HashMap<>();

        for (String tenantHash : this.tenantMap.keySet()) {
            tenantEvictablesMap.put(tenantHash, new ArrayList<>());
        }
    }

    @Override
    @SuppressWarnings("CallToPrintStackTrace")
    public void redistribute() {

        System.out.println(
                "############################################################");

        // Increment completedRounds
        int completedRounds = this.tenantGroup.incrementRound();

        final Short thresholdFrequency = CoreConstants.THRESHOLD_FREQUENCY;

        Map<String, Long> tenantColdRegion = new HashMap<>();
        Map<String, Long> tenantDemandedSize = new HashMap<>();
        Map<String, Long> tenantNetRequirement = new HashMap<>();
        Map<String, Long> lenderDeservedDemand = new HashMap<>();

        ArrayList<Tenant> lenders = new ArrayList<>();
        ArrayList<Tenant> borrowers = new ArrayList<>();
        ArrayList<Tenant> neutral = new ArrayList<>();

        int distributable = 0;

        // Reset evictables
        for (String tenantHash : tenantEvictablesMap.keySet()) {
            tenantEvictablesMap.get(tenantHash).clear();
        }

        Tracer.start("Find Cold/Demand");
        // For each tenant:
        // Aprox 20-30ms for 20 tenants, 20k keys per tenant. Through 4 hashes.
        for (String tenantHash : this.tenantMap.keySet()) {

            // Get Tenant
            Tenant tenant = this.tenantMap.get(tenantHash);

            // Update summationAllocation, summationFairShare
            tenant.updateCompletedRoundMetrics(completedRounds);

            // Get Cache
            Cache tenantCache = this.tenantCacheMap.get(tenantHash);

            // Get CMS
            Counter frequencyCounter = tenantCache.getFrequencyCounter();

            // Add to cold region and demanded size maps
            tenantColdRegion.put(tenantHash, (long) tenant.getAvailable());
            tenantDemandedSize.put(tenantHash, (long) 0);

            // Get tenant's cached keys
            Set<String> tenantCacheKeys = this.tenantCacheMap.get(tenantHash).getKeySet();

            Tracer.start("Find Cold for tenant" + tenantHash);

            // Find Cold Region in cache. Loop over cached keys.
            for (String key : tenantCacheKeys) {

                // Get frequency from Counter
                Short frequency = frequencyCounter.getCount(key);

                // Is cold cached key
                if (frequency < thresholdFrequency) {
                    tenantEvictablesMap.get(tenantHash).add(key);
                    tenantColdRegion.put(tenantHash, tenantColdRegion.get(tenantHash) + tenantCache.getKeySize(key));
                }
            }

            // Decay tenant's frequency counter. (All keys at once)
            Tracer.start("Freq Decay");
            frequencyCounter.decay();
            Tracer.end();

            // Get Demand Tracker of tenant's cache
            DemandTracker demandTracker = tenantCache.getDemandTracker();
            Map<String, ArrayList<Integer>> demandMap = demandTracker.getDemandMap();

            Tracer.start("Find Demand for tenant" + tenantHash);
            ArrayList<String> stopTrackingList = new ArrayList<>();
            // Find Demand of tenant. Loop over demanded keys
            for (String key : demandMap.keySet()) {

                ArrayList<Integer> metrics = demandTracker.getMetrics(key);
                Short fail_count = demandTracker.getFrequency(key);

                if (fail_count == 0) {
                    stopTrackingList.add(key);
                    continue;
                }

                if (fail_count > thresholdFrequency) {
                    tenantDemandedSize.put(tenantHash, tenantDemandedSize.get(tenantHash) + metrics.get(1));
                }

                // Decay fail_count (One key at a time)
                demandTracker.decayKey(key);

            }
            for (String key : stopTrackingList) {
                demandTracker.stopTracking(key);
            }
            Tracer.end();

            // Net requirement is demand - cold region. .: if net req < 0, allocation will
            // be decreases by adding -ve.
            long netRequirement = tenantDemandedSize.get(tenantHash) - tenantColdRegion.get(tenantHash);

            // Calculate tenant's net requirement
            tenantNetRequirement.put(tenantHash, netRequirement);

            long debt = tenant.getDebt();

            if (debt > 0) {
                borrowers.add(tenant);
            } else if (debt < 0) {
                lenders.add(tenant);
            } else {
                neutral.add(tenant);
            }

            if (netRequirement < 0) { // Cold region is more than demand
                // Take from tenants allocation
                tenant.setCurrentTotalAllocation(tenant.getCurrentTotalAllocation() + netRequirement);

                // Add to distributable (net Requirement is -ve for cold tenants)
                distributable += -1 * netRequirement;
            }
        }
        Tracer.end();

        printSnapshot(tenantColdRegion, tenantDemandedSize, tenantNetRequirement, true);

        // --------- DISTRIBUTION LOGIC ----------
        Tracer.start("Redistribution Event");

        // TODO: removing from distribution should always be a method call that performs
        // a transaction that maintains consistency in metrics.

        // Sort Borrowers in desc order of A/R. Most advantaged first
        // May leave out borrowers with A/R slightly above 1 but not enought to be
        // prempted from if demand doesnt require,
        // this is fine because as long as lenders are getting as much as they need when
        // they need,
        // other fairness losses are acceptable.
        borrowers.sort(Comparator.comparingDouble(Tenant::getAllocationRatio).reversed());

        // Sort Lenders in ascending order of A/R. Most disadvantaged first
        lenders.sort(Comparator.comparingDouble(Tenant::getAllocationRatio));

        // Find deserved vs extra demand of lenders.
        for (Tenant lender : lenders) {
            String lenderHash = lender.getHashToken();
            long deserved = -1 * lender.getDebt();
            lenderDeservedDemand.put(
                    lenderHash,
                    Math.min(deserved, tenantDemandedSize.get(lenderHash)));

        }

        long lenderFairDemand = 0;

        for (Tenant lender : lenders) {
            lenderFairDemand += lenderDeservedDemand.get(lender.getHashToken());
        }

        // Lender's demand must be maximally satisfied. But not necessarily completely.

        // If cold region can not satisfy lender's fair demand.
        if (distributable < lenderFairDemand) {
            // Prempt from borrowers
            distributable += fairCachePremption(lenderFairDemand, borrowers);
        }

        // If distributable exists, distribute first among lenders
        if (distributable > 0) {
            distributable -= minMaxDistribute(distributable, lenders, lenderDeservedDemand, lenderDeservedDemand);
        }

        /*
         * It is possible that not all of the deserved demand has been satisfied.
         * And in such cases, only those whose demand has been satisfied should be
         * treated as neutral tenant (A/R = 1).
         * But since we're maximally using distributable until it is 0, if distributable
         * is not left, we only give what we can to deserving amount and then skip all
         * other demands.
         * In case distributable is left, it must NECESSARILY mean that deserved demand
         * was fully satisfied.
         */
        // If distributable is remaining, then deserved demand must have been satisfied.
        if (distributable > 0) {
            // For extra demand of lenders, treat them as neutral.
            for (Tenant lender : lenders) {
                String tenandHash = lender.getHashToken();
                // Subtract deserved amount from demand
                tenantDemandedSize.put(tenandHash,
                        tenantDemandedSize.get(tenandHash) - lenderDeservedDemand.get(tenandHash));
            }

            // Add to neutrals
            neutral.addAll(lenders);
        }

        // If remaining, distribute in neutral tenants.
        if (distributable > 0) {
            distributable -= minMaxDistribute(distributable, neutral, tenantNetRequirement, tenantNetRequirement);
        }

        // If remaining still, distribute among borrowers.
        if (distributable > 0) {
            distributable -= minMaxDistribute(distributable, borrowers, tenantNetRequirement, tenantNetRequirement);
        }

        if (distributable > 0) {
            // By this point lenders could not have DEMANDED more without efficiency loss.
            // Same is the case for neutral tenants
            // It might make sense to distrubte to borrowers who's premption has been maxed
            // out. this may help smooth out fairness enforcement
            // However, if lenders took first from cold region, then by premption, then cold
            // was not suffeceint,
            // and premption only took out what was needed for use. Then this condition
            // might never occur.

            // TODO: Distribute by minmax to those who have less than fair share.

            // Underallocated does not mean lender. Just means r - a > 0.
            ArrayList<Tenant> underAllocated = new ArrayList<>();
            Map<String, Long> tenantUnderAllocation = new HashMap<>();

            for (String tenantHash : this.tenantMap.keySet()) {

                Tenant curTenant = this.tenantMap.get(tenantHash);
                Long allocationDiff = curTenant.getFairShareAllocation() - curTenant.getCurrentTotalAllocation();

                if (allocationDiff > 0) {
                    underAllocated.add(curTenant);
                    tenantUnderAllocation.put(tenantHash, allocationDiff);
                }
            }

            this.minMaxDistribute(distributable, underAllocated, tenantUnderAllocation, tenantUnderAllocation);

        }

        printSnapshot(tenantColdRegion, tenantDemandedSize, tenantNetRequirement, false);

        // Update tenant weights
        for (Tenant tenant : tenantMap.values()) {
            double weight = (double) tenant.getCurrentTotalAllocation() / CoreConstants.TOTAL_CACHE_SIZE;
            tenant.setCurrentWeight(weight);
        }

        Tracer.end();

    }

    private long fairCachePremption(long requirement, ArrayList<Tenant> premptionCandidates) {

        // Create copy of premptionCandidates
        ArrayList<Tenant> candidates = new ArrayList<>(premptionCandidates);

        int prempted = 0;

        // If premption has ran through all candidates, premption will end.
        while (prempted < requirement && !candidates.isEmpty()) {
            // Pop highest A/R
            Tenant candidate = candidates.remove(0);

            prempted += candidate.premptFromDebt(requirement - prempted);

            // requirement -= prempted;
        }

        return prempted;
    }

    private long minMaxDistribute(long distributable,
            ArrayList<Tenant> distributionCandidates,
            Map<String, Long> comparatorMap,
            Map<String, Long> demandMap) {

        if (distributionCandidates.isEmpty()) {
            return 0;
        }

        ArrayList<Tenant> candidates = new ArrayList<>(distributionCandidates);

        // Smallest need first because that is what gets poped off first.
        candidates = candidates.stream()
                .filter(t -> comparatorMap.get(t.getHashToken()) > 0)
                .sorted(Comparator.comparingLong((Tenant t) -> comparatorMap.get(t.getHashToken())))
                .collect(Collectors.toCollection(ArrayList::new));

        long distributed = 0;

        int candidatesCount = candidates.size();

        while (!candidates.isEmpty() && distributable >= demandMap.get(candidates.get(0).getHashToken())) {
            // Smallest demand
            long minDemand = demandMap.get(candidates.get(0).getHashToken());

            // If distributable cannot provide equivalent of smallest demand to all
            if (distributable < candidatesCount * minDemand) {
                break;
            }

            // Give min demand to all.
            for (Tenant tenant : candidates) {
                // Take from distributable
                distributable -= minDemand;

                // Give to tenant
                tenant.setCurrentTotalAllocation(tenant.getCurrentTotalAllocation() + minDemand);

                // Add to distributed count
                distributed += minDemand;
            }

            // Move to next smallest demand.
            candidates.remove(0);
            candidatesCount -= 1;
        }

        if (!candidates.isEmpty() && distributable > 0) {
            // Divide distributable
            if (distributable > 0) {
                // Give divisible ammount
                long dividend = Math.floorDiv(distributable, candidatesCount);

                for (Tenant tenant : candidates) {
                    distributable -= dividend;

                    tenant.setCurrentTotalAllocation(tenant.getCurrentTotalAllocation() + dividend);

                    distributed += dividend;
                }
            }
        }

        return distributed;
    }

    @Override
    public Map<String, ArrayList<String>> getTenantEvictablesMap() {
        return tenantEvictablesMap;
    }

    public void printSnapshot(Map<String, Long> coldRegionMap, Map<String, Long> demandedRegionMap,
            Map<String, Long> netDemandMap, boolean predistribution) {

        for (Tenant tenant : this.tenantMap.values()) {
            String tenantHash = tenant.getHashToken();

            long tenantA = tenant.getSummationAllocation();
            long tenantR = tenant.getSummationFairShare();
            long a = tenant.getCurrentTotalAllocation() + coldRegionMap.get(tenantHash);
            long availableRegion = tenant.getAvailable();
            double ratioA_by_R = tenant.getAllocationRatio();

            int round = this.tenantGroup.getCompletedRounds();
            long coldRegion = coldRegionMap.get(tenantHash);
            long hotRegion = a - coldRegion;
            long demandedRegion = demandedRegionMap.get(tenantHash);
            long netDemand = netDemandMap.get(tenantHash);

            if (predistribution) {

                LogUtil.log("Tenant " + tenant.getName() + " metrics after time window BUT PREDISTRIBUTION " + round,
                        "A of tenant", tenantA,
                        "R of tenant", tenantR,
                        "A/R", ratioA_by_R,
                        "Current allocation (After A,R Update, After cold/demand calculation, before redistribution) (actual current allocation has had cold region removed).",
                        a,
                        "Available", availableRegion,
                        "Cold Region", coldRegion,
                        "Hot Region", hotRegion,
                        "Demanded Region", demandedRegion,
                        "Net demand", netDemand);

            } else {

                LogUtil.log("Tenant " + tenant.getName() + " metrics after time window" + round,
                        "A of tenant", tenantA,
                        "R of tenant", tenantR,
                        "A/R", ratioA_by_R,
                        "Current allocation after redistribution", tenant.getCurrentTotalAllocation(),
                        "Available", availableRegion,
                        "Cold Region", coldRegion,
                        "Hot Region", hotRegion,
                        "Demanded Region", demandedRegion,
                        "Net demand", netDemand);

            }

        }
    }
}
