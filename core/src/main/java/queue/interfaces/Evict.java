package queue.interfaces;

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
    public void execute() {
        // TODO remove k:v from cache

        Response res = ResponseBuilders.makeDelResponse(200, "OK", reqId);

        channel.writeAndFlush(res);
    }
}
