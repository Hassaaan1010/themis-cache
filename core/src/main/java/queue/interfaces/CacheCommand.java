package queue.interfaces;

import io.netty.channel.Channel;

public sealed interface CacheCommand permits Put, Get, Evict {

    Channel channel();
    int reqId();
    String tenantId();

    public void execute();
}
