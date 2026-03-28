package server.serverUtils;

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

            tokenBuckets.put(hash, new AtomicInteger(0));
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

        // Update portions in map
        updateWeights();

        for (String tenantHashBucket : tokenBuckets.keySet()) {

            // If less than capacity
            double weight = tenantWeights.get(tenantHashBucket);
            if (tokenBuckets.get(tenantHashBucket).intValue() + (TOTAL_BUCKET_INCREMENT * weight) < TOTAL_BUCKETS_CAP
                    * weight) {
                tokenBuckets.get(tenantHashBucket).getAndAdd((int) Math.floor(TOTAL_BUCKET_INCREMENT * weight));
            }
        }
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
        while (true) {
            int curr = bucket.get();

            if (curr <= 0)
                return false;

            if (bucket.compareAndSet(curr, curr - 1)) {
                return true;
            }
        }
    }
}
