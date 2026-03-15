package server.controllers;

import cache.command.Del;
import common.parsing.protos.RequestProtos.Request;
import io.netty.channel.Channel;

public class DelController {

    public static Del delete( Channel channel, Request req) {

        Del cmd = new Del(channel, req.getToken(), req.getRequestId(), req.getKey());
        return cmd;
    }

}