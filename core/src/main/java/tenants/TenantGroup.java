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
import commonCore.CoreConstants;
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

        for (Document tenantDoc : allTenants) {
            // Make hashList
            String hash = AuthController.getMurmurHash.apply(
                    tenantDoc.getObjectId("_id").toString(),
                    tenantDoc.getString("password"));

            double startingWeight = tenantDoc.getDouble("weight");

            authHashesSet.add(hash);

            // Init tenant
            Tenant tenant = new Tenant(startingWeight, hash);

            tenantsMap.put(hash, tenant);

            tenantCacheMap.put(hash, tenant.getCache());

        }

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
        int maxEvictions = CoreConstants.MAX_TOTAL_AMORTIZED_ROUND_EVICT;

        // Sort by tenant.available ascending order
        ArrayList<Tenant> tenants = new ArrayList<>(tenantsMap.values());
        tenants.sort(Comparator.comparingLong(t -> t.getAvailable()));

        for (Tenant tenant : tenants) {

            String tenantHash = tenant.getHashToken();

            ArrayList<String> coldList = evictablesMap.get(tenantHash);
            Cache tenantCache = this.tenantCacheMap.get(tenantHash);

            while (maxEvictions > 0 && !coldList.isEmpty()) {
                String victim = coldList.remove(0);
                try {
                    tenantCache.remove(victim);
                } catch (Exception e) {
                    if (EchoServer.DEBUG_SERVER)
                        LogUtil.log("Error evicitng a key", "key", victim,
                                tenantCache.getFrequencyCounter().getCount(victim), "Error", e.getMessage());
                    throw new Exception("Error while evicting key");
                }

                maxEvictions--;

            }
        }
    }

    public void stopTheWorldEvent() throws Exception {

        this.policy.redistribute();
        Map<String, ArrayList<String>> evictablesMap = this.policy.getTenantEvictablesMap();

        amortizedEvict(evictablesMap);

    }

    public int incrementRound() {
        this.completedRounds += 1;
        return this.completedRounds;
    }
}