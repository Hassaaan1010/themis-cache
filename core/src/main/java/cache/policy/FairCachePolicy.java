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

public class FairCachePolicy implements Policy {

    private final Map<String, Cache> tenantCacheMap;
    private final Map<String, Tenant> tenantMap;
    private final TenantGroup tenantGroup;

    public FairCachePolicy(TenantGroup tenantGroup) {

        this.tenantGroup = tenantGroup;
        this.tenantCacheMap = this.tenantGroup.getTenantCacheMap();
        this.tenantMap = this.tenantGroup.getTenantsMap();
    }

    @Override
    public void redistribute() {

        // Increment completedRounds
        int completedRounds = this.tenantGroup.incrementRound();

        final Short thresholdFrequency = CoreConstants.THRESHOLD_FREQUENCY;

        Map<String, Integer> tenantColdRegion = new HashMap<>();
        Map<String, Integer> tenantDemandedSize = new HashMap<>();
        Map<String, Integer> tenantNetRequirement = new HashMap<>();

        ArrayList<Tenant> lenders = new ArrayList<>();
        ArrayList<Tenant> borrowers = new ArrayList<>();
        ArrayList<Tenant> neutral = new ArrayList<>();

        int distributable = 0;

        // For each tenant:
        // Aprox 20-30ms for 20 tenants, 20k keys per tenant. Through 4 hashes.
        for (String tenantHash : tenantGroup.getTenantCacheMap().keySet()) {

            // Get Tenant
            Tenant tenant = this.tenantMap.get(tenantHash);

            // Update summationAllocation, summationFairShare
            tenant.updateCompletedRoundMetrics(completedRounds);

            // Get Cache
            Cache tenantCache = this.tenantCacheMap.get(tenantHash);

            // Get CMS
            Counter frequencyCounter = tenantCache.getFrequencyCounter();

            // Add to cold region and demanded size maps
            tenantColdRegion.put(tenantHash, 0);
            tenantDemandedSize.put(tenantHash, 0);

            // Get tenant's cached keys
            Set<String> tenantCacheKeys = this.tenantCacheMap.get(tenantHash).getKeySet();

            // Find Cold Region in cache. Loop over cached keys. 
            for (String key : tenantCacheKeys) {

                // Get frequency from Counter
                Short frequency = frequencyCounter.getCount(key);

                // Is cold cached key
                if (frequency < thresholdFrequency) {
                    tenantColdRegion.put(tenantHash, tenantColdRegion.get(tenantHash) + tenantCache.getKeySize(key));
                }
            }

            // Get Demand Tracker of tenant's cache
            DemandTracker demandTracker = tenantCache.getDemandTracker();
            Map<String, ArrayList<Integer>> demandMap = demandTracker.getDemandMap();

            // Find Demand of tenant. Loop over demanded keys
            for (String key : demandMap.keySet()) {

                ArrayList<Integer> metrics = demandTracker.getMetrics(key);
                Short fail_count = demandTracker.getFrequency(key);

                if (fail_count < thresholdFrequency) {
                    tenantDemandedSize.put(tenantHash, tenantDemandedSize.get(tenantHash) + metrics.get(1));
                }
            }

            int netRequirement = tenantDemandedSize.get(tenantHash) - tenantColdRegion.get(tenantHash);

            // Calculate tenant's net requirement
            tenantNetRequirement.put(tenantHash, netRequirement);

            int debt = tenant.getDebt();

            if (debt > 0) {
                borrowers.add(tenant);
            } else if (debt < 0) {
                lenders.add(tenant);
            } else {
                neutral.add(tenant);
            }

            if (netRequirement < 0) { // Cold region is more than demand                      
                distributable += netRequirement;
                tenant.setCurrentTotalAllocation(tenant.getCurrentTotalAllocation() - netRequirement);
            }
        }

        // TODO: removing from distribution should always be a method call that performs a transaction that maintains consistency in metrics.
 
        // Sort Borrowers in desc order of debt. Most advantaged first
        borrowers.sort(Comparator.comparingDouble(Tenant::getAllocationRatio).reversed());

        // Sort Lenders in ascending order of deserved amount. Most disadvantaged first
        lenders.sort(Comparator.comparingDouble(Tenant::getAllocationRatio));

        int lenderDemand = 0;

        for (Tenant lender : lenders) {
            lenderDemand += tenantNetRequirement.get(lender.getHashToken());
        }

        // Lender's demand must be maximally satisfied. But not necessarily completely.
        int lenderQuota = 0;
        
        if (lenderDemand > 0) {
            // Use what is available for distribution
            lenderQuota += useDistributable(distributable, lenderDemand);
            distributable -= lenderQuota;
        }

        if (lenderQuota < lenderDemand) {
            // Prempt from borrowers
            lenderQuota += minMaxPremption(lenderDemand, borrowers);
        }

        // Distribute among lenders
        minMaxDistribution(lenderQuota, lenders);

        if (distributable > 0) {
            // Distribute remaining in neutral tenants.
            distributable -= minMaxDistribution(distributable, neutral);
        }

        
        if (distributable > 0) {
            // Distribute remaining in borrowers
            distributable -= minMaxDistribution(distributable, neutral);
        }

        if (distributable > 0) {
            // Distribute remaining for effeceincy maxing
             
        }


        
        
        
        // TODO: Return ratios for rebalancing
        
    }

    private int minMaxPremption(int requirement, ArrayList<Tenant> premptionCandidates) {

        // Tenants are sorted by highest A/R first.
        
        int prempted = 0;

        // What is the smallest you can prempt
        // Whom should you prempt from?

        return prempted;
    }

    private int minMaxDistribution(int distributable, ArrayList<Tenant> distributionCandidates) {
        int distributed = 0;

        return distributed;
    }

    private int useDistributable(int distributable, int demand) {
        // Return used amount
        return Math.min(distributable,demand);
    }
}

// Find available to Distribute
// Process Tenants
// Distribute stock
// for each tenant if net requirement < 0 add abs(netRequirement) to availableToDistribute;
// for each lender:
// if cold:
// remove cold Region and add to avialableToDistribute;
// subtract tenant.currentAllocation - cold
// evict cold items? 
// Sort by summationAllocation / summationFairShare
// Extract Lenders A/R < 1
// Extract Borrowers A/R > 1
