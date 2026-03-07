package queue.interfaces;

import cache.Cache;
import common.parsing.protos.ResponseProtos.Response;
import io.netty.channel.Channel;

public sealed interface CacheCommand permits Put, Get, Evict {

    Channel channel();
    int reqId();
    String tenantId();

    public Response execute(Cache tenantCache);
}
