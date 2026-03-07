package queue.interfaces;

// import com.google.protobuf.ByteString;

import cache.Cache;
import common.parsing.protos.ResponseProtos.Response;
import io.netty.channel.Channel;
import server.controllers.helpers.ResponseBuilders;



public final record Get(
        Channel channel,
        String tenantId,
        int reqId,
        String key
) implements CacheCommand {

    @Override
    public Response execute(Cache tenantCache) {
        // TODO: Get the ByteString value from caffeine instance

        Response res = ResponseBuilders.makeGetResponse(200, "OK", null, reqId);

        return res;
    }
}