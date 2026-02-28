package cache;

import java.util.Map;

import common.LogUtil;
import queue.CommandQueue;
import queue.interfaces.CacheCommand;
import server.EchoServer;
import tenants.TenantGroup;

public final class CacheEngine {
    private final CommandQueue queue;
    private final TenantGroup tenantGroup;
    private final Map<String, Cache> tenantCachesMap;

    private final Thread worker;
    private volatile boolean running;

    public CacheEngine(CommandQueue queue, TenantGroup tenantGroup) {
        this.queue = queue;
        this.tenantGroup = tenantGroup;
        this.tenantCachesMap = tenantGroup.getTenantCacheMap();
        this.worker = new Thread(this::runLoop, "cache-worker");
    }

    public void start() {
        this.running = true;
        worker.start();
        if (EchoServer.DEBUG_SERVER)
            LogUtil.log("✅ Started Cache Engine");
        if (EchoServer.DEBUG_SERVER)
            LogUtil.log("Random use of unused vars", "", tenantGroup, "", tenantCachesMap);
    }

    private void runLoop() {
        while (running) {
            CacheCommand cmd = queue.dequeue();

            // LogUtil.log("Random use of unused vars","cmd", cmd);
            // if (cmd != null) {
            // cmd.execute(); // single-threaded cache access
            // }

            // Readjust sizes of each tenants cache by fairness and effeciency

            // Amortized Eviction?

        }
    }

    public void shutdown() {
        this.running = false;
        this.worker.interrupt();
        if (EchoServer.DEBUG_SERVER)
            LogUtil.log("✅ Shutdown Cache Engine");
    }
}
