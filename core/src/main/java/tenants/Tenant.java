package tenants;

import cache.Cache;
import cache.demand.DemandTracker;
import commonCore.CoreConstants;

public class Tenant {
    
    private final String hashToken;

    private final Cache cache;

    private final double fairShareWeight; 
    
    private final int fairShareAllocation;
    private int currentTotalAllocation;
    
    private int summationAllocation;
    private int summationFairShare;
    
    private int available;

    
    private final DemandTracker demandTracker;
    
    public Tenant(double weight, String hashToken) {
        
        this.cache = new cache.Cache(this);

        // this.currentWeight = weight;
        this.hashToken = hashToken;
        this.fairShareWeight = weight;

        this.fairShareAllocation = (int) Math.floor(weight * CoreConstants.TOTAL_CACHE_SIZE );
        this.currentTotalAllocation = this.fairShareAllocation;
        this.available = this.currentTotalAllocation;
        
        // Added post each round
        this.summationFairShare = 0;
        this.summationAllocation = 0;
        
        this.demandTracker = new DemandTracker();
    }
    
    // ---- GETTERS ----
    // public double getCurrentWeight() {
    //     return currentWeight;
    // }
    
    public double getFairShareWeight() {
        return fairShareWeight;
    }
    
    public int getCurrentTotalAllocation() {
        return currentTotalAllocation;
    }

    public int getFairShareAllocation() {
        return fairShareAllocation;
    }

    public int getAvailable() {
        return available;
    }

    public int getDebt() {
        return this.summationAllocation - this.summationFairShare;
    }

    public double getAllocationRatio() {
        return this.summationAllocation / this.summationFairShare;
    }

    public void useAvailable(int newUse) throws Exception {
        if (getAvailable() - newUse < 0) {
            throw new Exception("Can not use more than allocated quota.");
        }
        this.available = this.available - newUse;
    }

    public void returnAvailable(int returnUsed) throws  Exception {
        if (getAvailable() + returnUsed > getCurrentTotalAllocation()) {
            throw  new Exception("Can not return more than what was taken.");
        }
        this.available = this.available + returnUsed;
    }
    
    public String getHashToken() {
        return hashToken;
    }
    
    public Cache getCache() {
        return cache;
    }
    
    public DemandTracker getDemandTracker() {
        return demandTracker;
    }
    
    // ---- SETTERS ----    
    public void setCurrentTotalAllocation(int newCurrentAllocation) {
        this.currentTotalAllocation = newCurrentAllocation;
    }
    
    public void updateCompletedRoundMetrics(int completedRounds) {
        this.summationAllocation += this.currentTotalAllocation;
        this.summationFairShare += this.fairShareAllocation;  
    }
   
    public int prempt(int demandedAmmount) {
        int unpremptable = (int) Math.floor(CoreConstants.UNPREMPTABLE_PERCENTAGE * this.getFairShareAllocation());
        int curAllocation = getCurrentTotalAllocation();
        if (unpremptable >= curAllocation) {
            return 0;
        }
        else {
            int premptable = curAllocation - unpremptable;
            return Math.min(premptable, demandedAmmount);
        }
    }
}
