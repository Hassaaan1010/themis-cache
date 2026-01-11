package server.serverUtils;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.Document;

import com.mongodb.client.FindIterable;

import common.LogUtil;
import db.MongoService;
import server.EchoServer;
import server.controllers.AuthController;

public class BucketsOwner {

    final private int MAX_BUCKET_CAP = 1000;
    final private int BUCKET_INCREMENT = 100;

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
        if (EchoServer.DEBUG_SERVER) LogUtil.log("Bucket has been initialized","buckets", tokenBuckets.toString());
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

    public boolean tryBucketDecrement(String token) {

        // Unrecognized tenant tokens are treated as having max rate limit as 0.
        if (!tokenBuckets.containsKey(token)) {
            return false; 
        }
        
        if (EchoServer.DEBUG_SERVER) LogUtil.log("Token received for decrement:","token",token);
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

    public Object getBuckets() {
        // TODO Auto-generated method stub
        return tokenBuckets.toString();
        // throw new UnsupportedOperationException("Unimplemented method 'printBucket'");
    }

}
