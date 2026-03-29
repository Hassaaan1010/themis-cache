package cache.policy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cache.Cache;
import cache.demand.DemandTracker;
import cache.frequency.Counter;
import commonCore.CoreConstants;
import tenants.Tenant;
import tenants.TenantGroup;

public class ThemisPolicy implements Policy {

    private final Map<String, Cache> tenantCacheMap;
    private final Map<String, Tenant> tenantMap;
    private final TenantGroup tenantGroup;
    private final Map<String, ArrayList<String>> tenantEvictablesMap;

    public ThemisPolicy(TenantGroup tenantGroup) {

        this.tenantGroup = tenantGroup;
        this.tenantCacheMap = this.tenantGroup.getTenantCacheMap();
        this.tenantMap = this.tenantGroup.getTenantsMap();
        this.tenantEvictablesMap = new HashMap<>();

        for (String tenantHash : this.tenantMap.keySet()) {
            tenantEvictablesMap.put(tenantHash, new ArrayList<>());
        }
    }

    @Override
    public void redistribute() {

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
            tenantColdRegion.put(tenantHash, (long) 0);
            tenantDemandedSize.put(tenantHash, (long) 0);

            // Get tenant's cached keys
            Set<String> tenantCacheKeys = this.tenantCacheMap.get(tenantHash).getKeySet();

            
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
            frequencyCounter.decay();

            // Get Demand Tracker of tenant's cache
            DemandTracker demandTracker = tenantCache.getDemandTracker();
            Map<String, ArrayList<Integer>> demandMap = demandTracker.getDemandMap();

            // Find Demand of tenant. Loop over demanded keys
            for (String key : demandMap.keySet()) {

                ArrayList<Integer> metrics = demandTracker.getMetrics(key);
                Short fail_count = demandTracker.getFrequency(key);
                
                if (fail_count == 0) {
                    demandTracker.stopTracking(key);
                    continue;
                }

                if (fail_count > thresholdFrequency) {
                    tenantDemandedSize.put(tenantHash, tenantDemandedSize.get(tenantHash) + metrics.get(1));
                }

                // Decay fail_count (One key at a time)
                demandTracker.decayKey(key);
                
            }

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
                tenant.setCurrentTotalAllocation(tenant.getCurrentTotalAllocation() - netRequirement);

                // Add to distributable
                distributable += netRequirement;
            }
        }

        // TODO: removing from distribution should always be a method call that performs
        // a transaction that maintains consistency in metrics.

        // Sort Borrowers in desc order of A/R. Most advantaged first
        // May leave out bor rowers with A/R slightly above 1 but not enought to be
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
                Math.min(deserved, tenantDemandedSize.get(lenderHash))
            );
            
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
            distributable -= minMaxByPriority(distributable, lenders, lenderDeservedDemand, lenderDeservedDemand);
        }

        /*
        It is possible that not all of the deserved demand has been satisfied. 
        And in such cases, only those whose demand has been satisfied should be treated as neutral tenant (A/R = 1).
        But since we're maximally using distributable until it is 0, if distributable is not left, we only give what we can to deserving amount and then skip all other demands.
        In case distributable is left, it must NECESSARILY mean that deserved demand was fully satisfied.
         */
        // If distributable is remaining, then deserved demand must have been satisfied.
        if (distributable > 0) {
            // For extra demand of lenders, treat them as neutral. 
            for (Tenant lender : lenders) {
                String tenandHash = lender.getHashToken();
                // Subtract deserved amount from demand
                tenantDemandedSize.put(tenandHash, tenantDemandedSize.get(tenandHash) - lenderDeservedDemand.get(tenandHash));
            }

            // Add to neutrals
            neutral.addAll(lenders);
        }

        // If remaining, distribute in neutral tenants.
        if (distributable > 0) {
            distributable -= minMaxByPriority(distributable, neutral, tenantDemandedSize, tenantNetRequirement);
        }

        // If remaining still, distribute among borrowers.
        if (distributable > 0) {
            distributable -= minMaxByPriority(distributable, borrowers, tenantDemandedSize, tenantNetRequirement);
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

            // TODO: Distribute remaining for effeceincy maxing or by random

        }


        // Update tenant weights
        for (Tenant tenant : tenantMap.values()) {
            double weight = CoreConstants.TOTAL_CACHE_SIZE / tenant.getCurrentTotalAllocation();
            tenant.setCurrentWeight(weight);
        } 

    }

    private long fairCachePremption(long requirement, ArrayList<Tenant> premptionCandidates) {

        // Create copy of premptionCandidates
        ArrayList<Tenant> candidates = new ArrayList<>(premptionCandidates);

        int prempted = 0;

        // If premption has ran through all candidates, premption will end.
        while (prempted < requirement && !candidates.isEmpty()) {
            // Pop highest A/R
            Tenant candidate = premptionCandidates.remove(0);

            prempted += candidate.premptFromDebt(requirement);
        }

        return prempted;
    }

    private long minMaxByPriority(long distributable,
            ArrayList<Tenant> distributionCandidates,
            Map<String, Long> comparatorMap,
            Map<String, Long> demandMap) {

        ArrayList<Tenant> candidates = new ArrayList<>(distributionCandidates);

        candidates
                .stream()
                .filter(t -> comparatorMap.get(t.getHashToken()) > 0)
                .sorted(Comparator.comparingDouble(
                        t -> comparatorMap.get(t.getHashToken())));

        long distributed = 0;

        int candidatesCount = candidates.size();

        while (distributed >= demandMap.get(candidates.get(0).getHashToken())) {
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
        }

        if (!candidates.isEmpty() && distributable > 0) {
            // Divide distributable
            if (distributable > 0) {
                // Give divisible ammount
                long dividend = Math.floorDiv(distributed, candidatesCount);

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
}

