package tenants;

import java.util.Collections;
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

        if (EchoServer.DEBUG_SERVER) LogUtil.log("✅ Create Tenant Group");

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


    public void amortizedEvict() {
        // Either evict a max number of keys or hit a certain size quota
        
    }

    public void stopTheWorldEvent() {

        this.policy.redistribute();

        // TODO: Adjust rate limits and buckets for tenant allocation.
        
        throw new UnsupportedOperationException("Unimplemented method 'rebalance'");
    }

    public int incrementRound() {
        this.completedRounds += 1;
        return this.completedRounds;
    }
}

















// CoreConstants.THRESHOLD_FREQUENCY;

/*
When this function is called, 
get starting weights
get current weights?
get current allocation
get frequency map
get average allocation for tenant... debt/due

rank by debt

let total memory be T

fair constant share be : r1,r2....
current allocation be: a1,a2,....

need final:
new weights w'1, w'2...
multiply by X and distribute

*/