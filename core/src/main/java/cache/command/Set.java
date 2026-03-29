package cache.command;

import com.google.protobuf.ByteString;

import cache.Cache;
import cache.utils.Tracer;
import common.parsing.protos.ResponseProtos.Response;
import io.netty.channel.Channel;
import server.controllers.helpers.ResponseBuilders;
import tenants.Tenant;

public final record Set(
        Channel channel,
        String tenantId,
        int reqId,
        String key,
        ByteString value) implements Executable {

    @Override
    public Response execute(Tenant tenant) throws Exception {

        Tracer.start("SET " + key);

        Cache tenantCache = tenant.getCache();
        boolean success;
        Response res;

        success = tenantCache.set(key, value);

        if (success) {
            res = ResponseBuilders.makeSetResponse(201, "Done", reqId);
        } else {
            res = ResponseBuilders.makeSetResponse(507, key, reqId);
        }

        Tracer.end();

        return res;
    }
}
