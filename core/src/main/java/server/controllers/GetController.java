package server.controllers;

import common.parsing.protos.RequestProtos.Request;
import common.parsing.protos.ResponseProtos.Response;
import io.netty.channel.ChannelHandlerContext;
import server.controllers.helpers.ResponseBuilders;

public class GetController {

    public static void get(ChannelHandlerContext ctx, Request req) {

        Response res;

        // throw new UnsupportedOperationException("Unimplemented method 'delete'");
        if (!AuthController.authenticateToken(req.getToken())) {
            res = ResponseBuilders.InvalidTokenResponse;
        } else {
            res = Response.newBuilder().build();
        }

        ctx.writeAndFlush(res);
        return;
    }

}