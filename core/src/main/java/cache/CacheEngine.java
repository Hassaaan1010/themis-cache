package cache;

import java.util.Map;

import cache.command.Executable;
import common.LogUtil;
import common.parsing.protos.ResponseProtos.Response;
import commonCore.CoreConstants;
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

        this.worker = new Thread(this::runEnginePoller, "cache-worker");
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

    private void runEnginePoller() {

        long lastRebalance = System.currentTimeMillis();
        long lastAmortization = System.currentTimeMillis();

        while (running) {
            try {
                Executable cmd = queue.poll();

                // String tenantId = cmd.tenantId();
                // tenantFrequencyMap.merge(tenantId, 1, Integer::sum);

                Tenant tenant = tenantMap.get(cmd.tenantId());

                Response res = cmd.execute(tenant);

                cmd.channel().writeAndFlush(res);



                // ----- SCHEDULED WORK ----- 
                /**
                 * Scheduled work can be held awaiting indefinitely if the queue is empty. 
                 * This is a necessary trade off to prevent spin wait on the queue and since there is no eviction or rebalancing pressure if no one is active, this has effectively no downside.
                 */
                
                long now = System.currentTimeMillis();

                // amortized eviction (small continuous work)
                if (now - lastAmortization >= CoreConstants.AMORTIZATION_WINDOW) {
                    tenantGroup.amortizedEvict(tenantGroup.getPolicy().getTenantEvictablesMap());   // evict small amount
                    lastAmortization = now;
                }

                // periodic stop-the-world rebalance
                if (now - lastRebalance >= CoreConstants.REBALANCING_WINDOW) {
                    tenantGroup.stopTheWorldEvent();
                    lastRebalance = now;
                }
                
            } catch (Exception e) {
                LogUtil.log("Exception in Cache Engine:", "Error", e);
                Thread.currentThread().interrupt();
                break;
            }

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
