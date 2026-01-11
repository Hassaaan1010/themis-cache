package queue;

import io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueue;
import queue.interfaces.CacheCommand;

public class CommandQueue {
    private MpscArrayQueue<CacheCommand> queue = new MpscArrayQueue<>( 1000);

    public void enqueue(CacheCommand cmd) {
        queue.offer(cmd);
    }
}   
