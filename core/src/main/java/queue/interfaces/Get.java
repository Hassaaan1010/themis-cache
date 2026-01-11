package queue.interfaces;

// import com.google.protobuf.ByteString;

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
    public void execute() {
        // TODO: Get the ByteString value from caffeine instance

        Response res = ResponseBuilders.makeGetResponse(200, "OK", null, reqId);

        channel.writeAndFlush(res);
    }
}