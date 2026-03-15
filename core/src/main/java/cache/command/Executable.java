package cache.command;

import common.parsing.protos.ResponseProtos.Response;
import io.netty.channel.Channel;
import tenants.Tenant;

public sealed interface Executable permits Set, Get, Del {

    Channel channel();
    int reqId();
    String tenantId();

    public Response execute (Tenant tenant) throws Exception;
}
