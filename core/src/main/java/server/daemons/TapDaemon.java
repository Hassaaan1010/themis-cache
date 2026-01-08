package server.daemons;

import common.LogUtil;
import server.EchoServer;
import server.serverUtils.BucketsOwner;

public class TapDaemon implements Runnable {

    final private int SLEEP_INTERVAL = 10;
    final private BucketsOwner bucketsOwner;
    final private Thread tapThread;

    private volatile boolean running = true;

    public TapDaemon(BucketsOwner buckets) {
        this.bucketsOwner = buckets;
        this.tapThread = new Thread("TapThread");
        this.tapThread.setDaemon(true);
        this.tapThread.start();
    }

    @Override
    public void run() {

        while (running) {
            try {
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

    public void shutdown() {
        this.running = false;
        this.tapThread.interrupt();
    }
}
