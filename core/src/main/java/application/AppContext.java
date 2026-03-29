package application;

import cache.CacheEngine;
import cache.MemoryManager;
import commonCore.EnvConfig;
import db.MongoService;
import queue.CommandQueue;
import server.daemons.BucketDaemon;
import server.rateLimiting.BucketsOwner;
import tenants.TenantGroup;

public class AppContext {

    // ---- Core Runtime State ----
    private final CommandQueue commandQueue;
    private final TenantGroup tenantGroup;
    private final BucketsOwner bucketsOwner;
    private final MemoryManager memoryManager;

    // ---- Workers ----
    private final BucketDaemon bucketDaemon;
    private final CacheEngine cacheWorker;
    private final MongoService mongoService;

    public AppContext() throws Exception {

        
        // ---- Db Init ----
        this.mongoService = new MongoService(
            EnvConfig.DB_URI_STRING,
            EnvConfig.DB_NAME
        );
        
        // ---- Core single-writer state ----
        this.commandQueue = new CommandQueue();
        
        // ---- Rate limiting ----
        this.bucketsOwner = new BucketsOwner(mongoService);
        this.bucketDaemon = new BucketDaemon(bucketsOwner);
        
        // ---- Tenant Group ----
        this.tenantGroup = new TenantGroup(mongoService);
        this.bucketsOwner.setTenantMap(this.tenantGroup.getTenantsMap());
        
        // ---- Cache Memory ----
        this.memoryManager = new MemoryManager();
        
        // ---- Poller thread ----
        this.cacheWorker = new CacheEngine(commandQueue, tenantGroup);
        
        // NOTE:
        // cacheWorker is the ONLY thing allowed to mutate tenantGroup.
        // No one else touches it.
    }

    // ---- Lifecycle ----

    public void start() {
        bucketDaemon.start();
        cacheWorker.start();
    }

    public void shutdown() {
        bucketDaemon.shutdown();
        cacheWorker.shutdown();
        mongoService.shutdown();
    }

    // ---- Getters (read-only access) ----

    public CommandQueue getCommandQueue() {
        return commandQueue;
    }

    public TenantGroup getTenantGroup() {
        return tenantGroup;
    }

    public BucketsOwner getBucketsOwner() {
        return bucketsOwner;
    }

    public MemoryManager getMemoryManager() {
        return memoryManager;
    }

}