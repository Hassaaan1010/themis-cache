package cache;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import common.LogUtil;
import common.parsing.protos.ResponseProtos.Response;
import queue.CommandQueue;
import queue.interfaces.CacheCommand;
import server.EchoServer;
import tenants.TenantGroup;

public final class CacheEngine {

    private final CommandQueue queue;
    private final TenantGroup tenantGroup;
    private final Map<String, Cache> tenantCachesMap;
    private final Map<String, Integer> tenantFrequencyMap; // this is reset to 0 every window so doesnt need more bits

    private final Thread worker;
    private volatile boolean running;

    public CacheEngine(CommandQueue queue, TenantGroup tenantGroup) {
        this.queue = queue;
        this.tenantGroup = tenantGroup;
        this.tenantCachesMap = tenantGroup.getTenantCacheMap();

        this.tenantFrequencyMap = new HashMap<>();

        for (String tenantId : tenantGroup.getAuthHashesSet()) {
            tenantFrequencyMap.put(tenantId, 0);
        }

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
                CacheCommand cmd = queue.poll();

                String tenantId = cmd.tenantId();

                tenantFrequencyMap.merge(tenantId, 1, Integer::sum);

                Cache tenantCache = tenantCachesMap.get(cmd.tenantId());

                Response res = cmd.execute(tenantCache);

                cmd.channel().writeAndFlush(res);

                tenantGroup.rebalance(Collections.unmodifiableMap(tenantFrequencyMap));

            } catch (InterruptedException e) {
                LogUtil.log("InterruptedException in Cache Engine:", "Error", e);
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
