package application;

import cache.CacheEngine;
import commonCore.EnvConfig;
import queue.CommandQueue;
import server.daemons.BucketDaemon;
import server.serverUtils.BucketsOwner;
import tenants.TenantGroup;
import db.MongoService;

public class AppContext {

    // ---- Core Runtime State ----
    private final CommandQueue commandQueue;
    private final TenantGroup tenantGroup;
    private final BucketsOwner bucketsOwner;

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
        
        // ---- Tenant Group
        this.tenantGroup = new TenantGroup(mongoService);
        
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
}