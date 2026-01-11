package server.controllers;

import common.parsing.protos.RequestProtos.Request;
import io.netty.channel.Channel;
import queue.interfaces.Put;

public class SetController {

    public static Put set(Channel channel, Request req) {
        Put cmd = new Put(
                channel,
                req.getToken(),
                req.getRequestId(),
                req.getKey(),
                req.getValue());
        return cmd;
    }

}