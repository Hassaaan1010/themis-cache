package cache;

import io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueue;
import queue.interfaces.CacheCommand;

public final class CacheEngine {
    private final MpscArrayQueue<CacheCommand> queue;
    private final Thread worker;

    public CacheEngine(int queueSize) {
        this.queue = new MpscArrayQueue<>(queueSize);
        this.worker = new Thread(this::runLoop, "cache-worker");
    }

    public void start() {
        worker.start();
    }
    
    private void runLoop() {
        while (true) {
            CacheCommand cmd = queue.poll();
            if (cmd != null) {
                cmd.execute(); // single-threaded cache access
            }
        }
    }
}
