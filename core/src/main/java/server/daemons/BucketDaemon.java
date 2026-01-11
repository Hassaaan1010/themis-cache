package server.daemons;

import common.LogUtil;
import server.EchoServer;
import server.serverUtils.BucketsOwner;

public class BucketDaemon implements Runnable {

    final private int SLEEP_INTERVAL = 500;
    final private BucketsOwner bucketsOwner;
    final private Thread tapThread;

    private volatile boolean running;

    public BucketDaemon(BucketsOwner buckets) {
        this.bucketsOwner = buckets;
        this.tapThread = new Thread(this, "TapThread");
        this.tapThread.setDaemon(true);
    }


    @Override
    public void run() {

        while (running) {
            try {
                LogUtil.log("Tap Daemon is filling buckets");
                this.bucketsOwner.incrementBuckets();
                Thread.sleep(SLEEP_INTERVAL);
            } catch (InterruptedException e) {
                // interruption is the only sane shutdown signal
                running = false;
                Thread.currentThread().interrupt();
            } catch (Throwable t) {
                if (EchoServer.DEBUG_SERVER) {
                    LogUtil.log("TapDaemon crashed", "ERROR", t.getStackTrace());
                }
            }
        };

    }

    public void start() {
        this.running = true;
        this.tapThread.start();
    }

    public void shutdown() {
        this.running = false;
        this.tapThread.interrupt();
    }
}
