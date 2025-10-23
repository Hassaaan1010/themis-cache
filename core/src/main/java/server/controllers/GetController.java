package server.controllers;

import java.util.HashMap;

import com.google.protobuf.ByteString;

import common.parsing.protos.RequestProtos.Request;
import common.parsing.protos.ResponseProtos.Response;
import io.netty.channel.ChannelHandlerContext;
import server.controllers.helpers.ResponseBuilders;

public class GetController {

    public static final HashMap<String, ByteString> Cache = new HashMap<>();

    static {
        Cache.put("PING", ByteString.copyFromUtf8("PONG"));
    }

    public static void get(ChannelHandlerContext ctx, Request req) {
    Response res;

    if (!AuthController.authenticateToken(req.getToken())) {
        res = ResponseBuilders.makeInvalidTokenResponse(req.getToken(), req.getRequestId());
    } else {
        ByteString val = Cache.get(req.getKey());
        
        if (val == null) {
            res = ResponseBuilders.keyNotFoundResponse(404, "Key not found", req.getRequestId());
        } else {
            res = ResponseBuilders.makeGetResponse(200, "Key was found successfully.", val, req.getRequestId());
        }
    }

    ctx.writeAndFlush(res);
}

}