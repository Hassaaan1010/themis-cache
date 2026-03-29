package cache.command;

import com.google.protobuf.ByteString;

import cache.utils.Tracer;
import common.parsing.protos.ResponseProtos.Response;
import io.netty.channel.Channel;
import server.controllers.helpers.ResponseBuilders;
import tenants.Tenant;

public final record Get(
        Channel channel,
        String tenantId,
        int reqId,
        String key) implements Executable {

    @Override
    public Response execute(Tenant tenant) {

        Tracer.start("GET " + key);
        ByteString value = tenant.getCache().get(key);
        Response res;

        if (value == null) {
            res = ResponseBuilders.keyNotFoundResponse(reqId, key, reqId);
        } else {
            res = ResponseBuilders.makeGetResponse(200, "OK", value, reqId);
        }

        Tracer.end();
        return res;
    }
}