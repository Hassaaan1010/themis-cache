package tenants;

import cache.Cache;

public class Tenant {
    
    private final String hash;

    public Cache myCache;

    private final double currentWeight;
    private double fairShare; 


    public Tenant(double weight, String hash, Cache cache) {
        this.currentWeight = weight;
        this.hash = hash;
        this.fairShare = weight;
        this.myCache = cache;
    }
    
    public double getCurrentWeight() {
        return currentWeight;
    }

    public String getHash() {
        return hash;
    }

    public double getFairShare() {
        return fairShare;
    }

    public void setFairShare(double fairShare) {
        this.fairShare = fairShare;
    }
    
}
