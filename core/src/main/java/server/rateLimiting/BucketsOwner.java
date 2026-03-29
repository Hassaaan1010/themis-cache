package server.rateLimiting;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.Document;

import com.mongodb.client.FindIterable;

import common.LogUtil;
import db.MongoService;
import server.EchoServer;
import server.controllers.AuthController;
import tenants.Tenant;

public class BucketsOwner {

    final private int TOTAL_BUCKETS_CAP = 1000;
    final private int TOTAL_BUCKET_INCREMENT = 100;
    private Map<String, Tenant> tenantMap;
    private Map<String, Double> tenantWeights;
    private final HashMap<String, AtomicInteger> tokenBuckets = new HashMap<>();

    public BucketsOwner(MongoService mongoService) {
        // Initialize
        FindIterable<Document> allTenants = mongoService.getAllFromUserCollection();

        for (Document tenant : allTenants) {
            String hash = AuthController.getMurmurHash.apply(
                    tenant.getObjectId("_id").toString(),
                    tenant.getString("password"));

            this.tenantWeights = new HashMap<>();

            Double weight = tenant.getDouble("weight");
            tokenBuckets.put(hash, new AtomicInteger((int) Math.floor(TOTAL_BUCKETS_CAP * weight)));
        }
        if (EchoServer.DEBUG_SERVER)
            LogUtil.log("✅ Create Bucket Owner", "buckets", tokenBuckets.toString());
    }

    public AtomicInteger getBucketOfTenant(String hash) {
        return tokenBuckets.get(hash);
    }

    public void setTenantMap(Map<String, Tenant> tenants) {
        this.tenantMap = tenants;
    }

    public void incrementBuckets() {

        long start = System.nanoTime();

        if (EchoServer.DEBUG_SERVER)
            LogUtil.log("Starting Bucket Increment");
        // Update portions in map
        updateWeights();

        for (String tenantHashBucket : tokenBuckets.keySet()) {

            // If less than capacity
            double weight = tenantWeights.get(tenantHashBucket);
            int cap = (int) (TOTAL_BUCKETS_CAP * weight);
            int incrementAmount = Math.min((int) (weight * TOTAL_BUCKET_INCREMENT), cap);
            if (tokenBuckets.get(tenantHashBucket).intValue() + incrementAmount < cap) {
                tokenBuckets.get(tenantHashBucket).getAndAdd(incrementAmount);
            }
        }
        long end = System.nanoTime();

        if (EchoServer.DEBUG_SERVER)
        LogUtil.log("Ended Bucket Increment", "took_ns", end - start, "buckets", tokenBuckets );
    }

    private void updateWeights() {
        for (Tenant tenant : this.tenantMap.values()) {
            this.tenantWeights.put(tenant.getHashToken(), tenant.getCurrentWeight());
        }
    }

    public boolean tryBucketDecrement(String token) {

        // Unrecognized tenant tokens are treated as having max rate limit as 0.
        if (!tokenBuckets.containsKey(token)) {
            return false;
        }

        if (EchoServer.DEBUG_SERVER)
            LogUtil.log("Token received for decrement:", "token", token);
        AtomicInteger bucket = tokenBuckets.get(token);

        // check if tokens not left, return false
        // while (true) {
        synchronized (bucket) {
            int curr = bucket.get();

            if (curr <= 0)
                return false;

            if (bucket.compareAndSet(curr, curr - 1)) {
                return true;
            } else {
                return false;
            }
        }
        // }
    }
}
