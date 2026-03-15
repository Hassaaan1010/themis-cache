package tenants;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bson.Document;

import com.mongodb.client.FindIterable;

import cache.Cache;
import common.LogUtil;
import db.MongoService;
import server.EchoServer;
import server.controllers.AuthController;

public class TenantGroup {

    private final HashSet<String> authHashesSet = new HashSet<>();

    private final HashMap<String, Tenant> tenantsMap = new HashMap<>();

    private final HashMap<String, Cache> tenantCacheMap = new HashMap<>();

    public TenantGroup(MongoService mongoService) {

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

     public void rebalance(TenantGroup tenantGroup) {

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
        throw new UnsupportedOperationException("Unimplemented method 'rebalance'");
    }
}
