package server.controllers;

import cache.command.Set;
import common.parsing.protos.RequestProtos.Request;
import io.netty.channel.Channel;

public class SetController {

    public static Set set(Channel channel, Request req) {
        Set cmd = new Set(
                channel,
                req.getToken(),
                req.getRequestId(),
                req.getKey(),
                req.getValue());
        return cmd;
    }

}