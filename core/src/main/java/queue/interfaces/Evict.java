package queue.interfaces;

import cache.Cache;
import common.parsing.protos.ResponseProtos.Response;
import io.netty.channel.Channel;
import server.controllers.helpers.ResponseBuilders;

public final record Evict(
        Channel channel,
        String tenantId,
        int reqId,
        String key
) implements CacheCommand {

    @Override
    public Response execute(Cache tenantCache) {
        // TODO remove k:v from cache

        Response res = ResponseBuilders.makeDelResponse(200, "OK", reqId);

        return res;
    }
}
