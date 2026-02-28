package tenants;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import server.EchoServer;
import server.controllers.AuthController;
import org.bson.Document;

import com.mongodb.client.FindIterable;

import cache.Cache;
import cache.policy.EvictionDecider;
import cache.policy.ThemisPolicy;
import common.LogUtil;
import db.MongoService;

public class TenantGroup {

    private HashSet<String> authHashesSet = new HashSet<>();

    private HashMap<String, Tenant> tenantsMap = new HashMap<>();

    private HashMap<String, Cache> tenantCacheMap = new HashMap<>();

    EvictionDecider policy = new ThemisPolicy();

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

            // Make cache instance
            Cache cache = new cache.Cache(policy);

            // Init tenants
            Tenant tenant = new Tenant(startingWeight, hash, cache);

            tenantsMap.put(hash, tenant);

            tenantCacheMap.put(hash, cache);

        }

        if (EchoServer.DEBUG_SERVER) LogUtil.log("✅ Create Tenant Group");

    }


    public Set<String> getAuthHashesSet() {
        return Collections.unmodifiableSet(authHashesSet);
    }

    public Map<String, Cache> getTenantCacheMap() {
        return Collections.unmodifiableMap(tenantCacheMap);
    }
}
