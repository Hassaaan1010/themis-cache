package cache;

import java.util.Map;

import cache.command.Executable;
import common.LogUtil;
import common.parsing.protos.ResponseProtos.Response;
import queue.CommandQueue;
import server.EchoServer;
import tenants.Tenant;
import tenants.TenantGroup;

public final class CacheEngine {

    private final CommandQueue queue;
    private final TenantGroup tenantGroup;
    private final Map<String, Cache> tenantCachesMap;
    // private final Map<String, Integer> tenantFrequencyMap; // this is reset to 0 every window so doesnt need more bits
    private final Map<String, Tenant> tenantMap;
    private final Thread worker;
    private volatile boolean running;

    public CacheEngine(CommandQueue queue, TenantGroup tenantGroup) {
        this.queue = queue;
        this.tenantGroup = tenantGroup;
        this.tenantCachesMap = tenantGroup.getTenantCacheMap();
        this.tenantMap = tenantGroup.getTenantsMap();

        // this.tenantFrequencyMap = new HashMap<>();
        // for (String tenantId : tenantGroup.getAuthHashesSet()) {
        //     tenantFrequencyMap.put(tenantId, 0);
        // }

        this.worker = new Thread(this::runLoop, "cache-worker");
    }

    public void start() {
        this.running = true;
        worker.start();
        if (EchoServer.DEBUG_SERVER) {
            LogUtil.log("✅ Started Cache Engine");
        }
        if (EchoServer.DEBUG_SERVER) {
            LogUtil.log("Random use of unused vars", "", tenantGroup, "", tenantCachesMap);
        }
    }

    private void runLoop() {
        while (running) {
            try {
                Executable cmd = queue.poll();

                // String tenantId = cmd.tenantId();

                // Increment frequency of requests per tenant. TODO: Remove if unnecessary
                // tenantFrequencyMap.merge(tenantId, 1, Integer::sum);

                Tenant tenant = tenantMap.get(cmd.tenantId());

                Response res = cmd.execute(tenant);

                cmd.channel().writeAndFlush(res);

                tenantGroup.rebalance(tenantGroup);
                

                // if time to evict
                //      evict( n keys or until n amount of space has been emptied )
                // if time to rebalance
                //      rebalance // this is where rebalance HAS to be called. 
                
                
                

                // conditionallyEvict();

                // conditionallyRebalance();
                
            } catch (Exception e) {
                LogUtil.log("Exception in Cache Engine:", "Error", e);
                Thread.currentThread().interrupt();
                break;
            }

            // Amortized Eviction?
        }
    }

    public void shutdown() {
        this.running = false;
        this.worker.interrupt();
        if (EchoServer.DEBUG_SERVER) {
            LogUtil.log("✅ Shutdown Cache Engine");
        }
    }
}
