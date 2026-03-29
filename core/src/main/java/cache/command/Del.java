package cache.command;

import cache.utils.Tracer;
import common.parsing.protos.ResponseProtos.Response;
import io.netty.channel.Channel;
import server.controllers.helpers.ResponseBuilders;
import tenants.Tenant;

public final record Del(
        Channel channel,
        String tenantId,
        int reqId,
        String key) implements Executable {

    @Override
    public Response execute(Tenant tenant) throws Exception {

        Tracer.start("DEL " + key);

        tenant.getCache().remove(key);

        Response res = ResponseBuilders.makeDelResponse(200, "OK", reqId);

        Tracer.end();
        return res;
    }
}
