package queue;

import common.LogUtil;
import commonCore.CoreConstants;
import io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueue;
import queue.interfaces.CacheCommand;
import server.EchoServer;

public class CommandQueue {
    private MpscArrayQueue<CacheCommand> queue;

    public CommandQueue () {
        this.queue = new MpscArrayQueue<>( CoreConstants.QUEUE_CAPACITY);
        if (EchoServer.DEBUG_SERVER) LogUtil.log("✅ Created Command Queue");

    }

    public void enqueue(CacheCommand cmd) {
        queue.offer(cmd);
    }

    public CacheCommand dequeue() {
        return queue.poll();
    }
}   
