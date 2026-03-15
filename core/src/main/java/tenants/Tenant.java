package tenants;

import cache.Cache;
import cache.demand.DemandTracker;
import commonCore.CoreConstants;

public class Tenant {
    
    private final String hash;

    private final Cache cache;

    private final double fairShareWeight; 
    private final double currentWeight;
    private final int fairShareAllocation;
    private int currentTotalAllocation;
    private int available;

    
    private final DemandTracker demandTracker;
    
    public Tenant(double weight, String hash) {
        
        this.cache = new cache.Cache(this);

        this.currentWeight = weight;
        this.hash = hash;
        this.fairShareWeight = weight;
        this.fairShareAllocation = (int) Math.floor(weight * CoreConstants.TOTAL_CACHE_SIZE );
        this.available = this.fairShareAllocation;
        
        this.demandTracker = new DemandTracker();
    }
    
    // ---- GETTERS ----
    public double getCurrentWeight() {
        return currentWeight;
    }
    
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
    
    public String getHash() {
        return hash;
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


    
}
