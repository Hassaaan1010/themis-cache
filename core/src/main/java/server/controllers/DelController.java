package server.controllers;

import common.parsing.protos.RequestProtos.Request;
import io.netty.channel.Channel;
import queue.interfaces.Evict;

public class DelController {

    public static Evict delete( Channel channel, Request req) {

        Evict cmd = new Evict(channel, req.getToken(), req.getRequestId(), req.getKey());
        return cmd;
    }

}