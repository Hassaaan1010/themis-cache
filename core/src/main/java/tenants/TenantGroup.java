package tenants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bson.Document;

import com.mongodb.client.FindIterable;

import cache.Cache;
import cache.policy.FairCachePolicy;
import cache.policy.Policy;
import common.LogUtil;
import db.MongoService;
import server.EchoServer;
import server.controllers.AuthController;

public class TenantGroup {

    private final HashSet<String> authHashesSet = new HashSet<>();

    private final HashMap<String, Tenant> tenantsMap = new HashMap<>();

    private final HashMap<String, Cache> tenantCacheMap = new HashMap<>();

    private final Policy policy;

    private int completedRounds;

    public TenantGroup(MongoService mongoService) {

        /**
         * Initilize windows at 0 and then increment on starting rebalancing.
         */
        this.completedRounds = 0;

        // Load all tenants from db
        FindIterable<Document> allTenants = mongoService.getAllFromUserCollection();
        double sumOfWeights = 0;
        
        for (Document tenantDoc : allTenants) {
            // Make hashList
            String hash = AuthController.getMurmurHash.apply(
                    tenantDoc.getObjectId("_id").toString(),
                    tenantDoc.getString("password"));

            double startingWeight = tenantDoc.getDouble("weight");
            String name = tenantDoc.getString("tenantName");
            sumOfWeights += startingWeight;

            authHashesSet.add(hash);

            // Init tenant
            Tenant tenant = new Tenant(startingWeight, name, hash);

            tenantsMap.put(hash, tenant);

            tenantCacheMap.put(hash, tenant.getCache());

        }

        assert Math.abs(sumOfWeights - 1.0) < 1e-4;

        // Initialize policy
        this.policy = new FairCachePolicy(this);

        if (EchoServer.DEBUG_SERVER)
            LogUtil.log("✅ Create Tenant Group");

    }

    public Set<String> getAuthHashesSet() {
        return Collections.unmodifiableSet(authHashesSet);
    }

    public Map<String, Cache> getTenantCacheMap() {
        return Collections.unmodifiableMap(tenantCacheMap);
    }

    public HashMap<String, Tenant> getTenantsMap() {
        return tenantsMap;
    }

    public Policy getPolicy() {
        return policy;
    }

    public void amortizedEvict(Map<String, ArrayList<String>> evictablesMap) throws Exception {
        // Either evict a max number of keys or hit a certain size quota
        // int maxEvictions = CoreConstants.MAX_TOTAL_AMORTIZED_ROUND_EVICT;

        // Sort by tenant.available ascending order
        ArrayList<Tenant> tenants = new ArrayList<>(tenantsMap.values());
        tenants.sort(Comparator.comparingLong(t -> t.getAvailable()));

        // Evict until tenants available space is 0 or more

        for (Tenant tenant : tenants) {

            String tenantHash = tenant.getHashToken();

            long available = tenant.getAvailable();

            // Tenant is only using space that they are supposed to inhabit.
            if (available >= 0) {
                continue;
            }

            // ArrayList<String> coldList = evictablesMap.get(tenantHash);
            Cache tenantCache = this.tenantCacheMap.get(tenantHash);
            ArrayList<String> keyList = new ArrayList<>(tenantCache.getKeySet());

            int i = 0;

            while (tenant.getAvailable() < 0) {
                tenantCache.remove(keyList.get(i));
                i++;
            }

            // while (maxEvictions > 0 && !coldList.isEmpty()) {
            //     String victim = coldList.remove(0);
            //     try {
            //         tenantCache.remove(victim);
            //     } catch (Exception e) {
            //         if (EchoServer.DEBUG_SERVER)
            //             LogUtil.log("Error evicitng a key", "key", victim,
            //                     tenantCache.getFrequencyCounter().getCount(victim), "Error", e.getMessage());
            //         throw new Exception("Error while evicting key");
            //     }

            //     maxEvictions--;

            // }
        }
        // 
        // if (maxEvictions > 0.5 * CoreConstants.MAX_TOTAL_AMORTIZED_ROUND_EVICT) {
        //     for (Cache cache : this.tenantCacheMap.values()) {
        //         Counter frequencyCounter = cache.getFrequencyCounter();

        //         for (String key : cache.getKeySet()) {
        //             short freq = frequencyCounter.getCount(key);
        //             if (freq < CoreConstants.THRESHOLD_FREQUENCY) {
        //                 cache.remove(key);
        //                 // TODO: while true consideration for maxEvictions limit.
        //             }
        //         }
        //     }
        // }
    }

    public void stopTheWorldEvent() throws Exception {

        this.policy.redistribute();
        Map<String, ArrayList<String>> evictablesMap = this.policy.getTenantEvictablesMap();

        amortizedEvict(evictablesMap);

    }

    public int getCompletedRounds() {
        return completedRounds;
    }

    public int incrementRound() {
        this.completedRounds += 1;
        return this.completedRounds;
    }
}