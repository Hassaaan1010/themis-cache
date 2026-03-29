package tenants;

import cache.Cache;
import cache.demand.DemandTracker;
import commonCore.CoreConstants;

public class Tenant {

    private final String hashToken;

    private final Cache cache;

    private final double fairShareWeight;
    private double currentWeight;

    private final long fairShareAllocation;
    private long currentTotalAllocation;

    private long summationAllocation;
    private long summationFairShare;

    private long available;

    private final DemandTracker demandTracker;

    public Tenant(double weight, String hashToken) {

        this.cache = new cache.Cache(this);

        // this.currentWeight = weight;
        this.hashToken = hashToken;
        this.fairShareWeight = weight;
        this.currentWeight = weight;

        this.fairShareAllocation = (long) Math.floor(weight * CoreConstants.TOTAL_CACHE_SIZE);
        this.currentTotalAllocation = this.fairShareAllocation;
        this.available = this.currentTotalAllocation;

        // Added post each round
        this.summationFairShare = 0;
        this.summationAllocation = 0;

        this.demandTracker = new DemandTracker();
    }

    // ---- GETTERS ----
    // public double getCurrentWeight() {
    // return currentWeight;
    // }

    public double getFairShareWeight() {
        return fairShareWeight;
    }

    public double getCurrentWeight() {
        return currentWeight;
    }

    public long getCurrentTotalAllocation() {
        return currentTotalAllocation;
    }

    public long getFairShareAllocation() {
        return fairShareAllocation;
    }

    public long getAvailable() {
        return available;
    }

    public long getDebt() {
        return this.summationAllocation - this.summationFairShare;
    }

    public double getAllocationRatio() {
        return this.summationAllocation / this.summationFairShare;
    }

    public void useAvailable(long newUse) throws Exception {
        if (getAvailable() - newUse < 0) {
            throw new Exception("Can not use more than allocated quota.");
        }
        this.available = this.available - newUse;
    }

    public void returnAvailable(long returnUsed) throws Exception {
        if (this.getAvailable() + returnUsed > this.getCurrentTotalAllocation()) {
            throw new Exception("Can not return more than what was taken.");
        }
        this.available = this.available + returnUsed;
    }

    public String getHashToken() {
        return hashToken;
    }

    public Cache getCache() {
        return cache;
    }

    public void setCurrentWeight(double currentWeight) {
        this.currentWeight = currentWeight;
    }

    public DemandTracker getDemandTracker() {
        return demandTracker;
    }

    // ---- SETTERS ----
    public void setCurrentTotalAllocation(long newCurrentAllocation) {
        /**
         * if newAlloc is bigger add difference to available.
         * if newAlloc is smaller, subtract difference from available. Yes. keep it
         * negative
         * 
         * during eviction.
         * when key is evicted,
         * you add to available in both cases
         * 
         * when key is set
         * if negative:
         * unable to set until enough has been freed up (this will happen after a lot of
         * evictions, tryFreeSpace also checks .getAvailable())
         * if positive:
         * use it lol. gc will be eventually achieve consistency in actual RAM.
         */
        long diff = newCurrentAllocation - this.getCurrentTotalAllocation();
        // More or same has been allocated
        // if (diff >= 0){
        // this.available += diff;
        // }
        // Available has been taken away from.
        // else {
        // this.available += diff;
        // }
        this.available += diff;
        this.currentTotalAllocation = newCurrentAllocation;
    }

    public void updateCompletedRoundMetrics(long completedRounds) {
        this.summationAllocation += this.currentTotalAllocation;
        this.summationFairShare += this.fairShareAllocation;
    }

    /**
     * Will not prempt more than debt.
     * 
     * @param demandedAmmount
     * @return
     */
    public long premptFromDebt(long demandedAmmount) {

        // Can not prempt more than what is owed.
        demandedAmmount = Math.min(demandedAmmount, this.getDebt());

        // Leave some space for critical keys.
        long unpremptable = (long) Math.floor(CoreConstants.UNPREMPTABLE_PERCENTAGE * this.getFairShareAllocation());
        long curAllocation = this.getCurrentTotalAllocation();

        if (unpremptable >= curAllocation) {
            return 0;
        } else {
            long premptable = curAllocation - unpremptable;
            return Math.min(premptable, demandedAmmount);
        }
    }
}
