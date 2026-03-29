package server.daemons;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import common.LogUtil;
import commonCore.CoreConstants;
import server.EchoServer;
import server.rateLimiting.BucketsOwner;

public class BucketDaemon {

    final private int sleepInterval = CoreConstants.BUCKET_REFILL_INTERVAL;
    final private BucketsOwner bucketsOwner;
    final private ScheduledExecutorService scheduler;
    // private volatile boolean running;

    public BucketDaemon(BucketsOwner buckets) {
        this.bucketsOwner = buckets;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {

        this.scheduler.scheduleWithFixedDelay(() -> {
            try {
                this.bucketsOwner.incrementBuckets();
            } catch (Exception e) {
                e.printStackTrace(); 
            }
        }, 0, sleepInterval, TimeUnit.MILLISECONDS);

        if (EchoServer.DEBUG_SERVER)
            LogUtil.log("✅ Started Bucket Daemon");
    }

    public void shutdown() {
        scheduler.shutdownNow();

        if (EchoServer.DEBUG_SERVER)
            LogUtil.log("✅ Shutdown Bucket Daemon");

    }
}
