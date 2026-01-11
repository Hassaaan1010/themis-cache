package queue.interfaces;

import com.google.protobuf.ByteString;

import common.parsing.protos.ResponseProtos.Response;
import io.netty.channel.Channel;
import server.controllers.helpers.ResponseBuilders;

public final record Put(
    Channel channel,
    String tenantId,
    int reqId,
    String key,
    ByteString value) implements CacheCommand {

    @Override
    public void execute() {
        // TODO: Set the k:v in caffeine instance

        Response res = ResponseBuilders.makeSetResponse(201, "Done", reqId);
        
        channel.writeAndFlush(res);
    }
    

}
