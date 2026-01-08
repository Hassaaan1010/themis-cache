package server.serverUtils;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.Document;

import com.mongodb.client.FindIterable;

import db.MongoService;
import server.controllers.AuthController;

public class BucketsOwner {

    final private int MAX_BUCKET_CAP = 300;
    final private int BUCKET_INCREMENT = 10;

    private HashMap<String, AtomicInteger> tokenBuckets = new HashMap<>();

    // private BiFunction<String, String, String> getHash = (id, password) ->
    // String.valueOf((id + password).hashCode());

    public BucketsOwner() {
        // Initialize
        FindIterable<Document> allTenants = MongoService.UserCollection.find();

        for (Document tenant : allTenants) {
            String hash = AuthController.getHash.apply(
                    tenant.getObjectId("_id").toString(),
                    tenant.getString("password"));

            tokenBuckets.put(hash, new AtomicInteger(MAX_BUCKET_CAP));
        }
    }

    public AtomicInteger getBucketOfTenant(String hash) {
        return tokenBuckets.get(hash);
    }

    public void incrementBuckets() {

        for (String bucket : tokenBuckets.keySet()) {

            // If less than capacity
            if (tokenBuckets.get(bucket).intValue() + BUCKET_INCREMENT < MAX_BUCKET_CAP) {
                tokenBuckets.get(bucket).getAndAdd(BUCKET_INCREMENT);

                // lock one tenant bucket, read and conditionally increment
                // synchronized (tokenBuckets.get(bucket)) {
                // Integer newVal = tokenBuckets.get(bucket) + ;
                // ...
                // }
            }
            // Else, its either close to full or will be filled in next round. Eventual
        }
    }

    public boolean decrementBucket(String token) {
        // Each bucket decrement has to be done in lock since concurrent decrements will
        // corrupt bucket.
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
