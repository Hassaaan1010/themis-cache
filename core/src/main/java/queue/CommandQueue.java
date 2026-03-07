package queue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import common.LogUtil;
import queue.interfaces.CacheCommand;
import server.EchoServer;

public class CommandQueue {

    // private MpscArrayQueue<CacheCommand> queue;
    private final BlockingQueue<CacheCommand> queue;

    public CommandQueue() {
        // this.queue = new MpscArrayQueue<>( CoreConstants.QUEUE_CAPACITY);
        this.queue = new LinkedBlockingQueue<>();
        if (EchoServer.DEBUG_SERVER) {
            LogUtil.log("✅ Created Command Queue");
        }

    }

    public void offer(CacheCommand cmd) {
        queue.offer(cmd); // Adds to queue and fails silently if queue is full. Ideally for this system queue size should never be the bottle neck
    }

    public CacheCommand poll() throws InterruptedException {
        return queue.take();
    }

}
