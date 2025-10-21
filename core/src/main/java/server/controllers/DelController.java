package server.controllers;

import common.parsing.protos.RequestProtos.Request;
import common.parsing.protos.ResponseProtos.Response;
import io.netty.channel.ChannelHandlerContext;
import server.controllers.helpers.ResponseBuilders;

public class DelController {

    public static void delete(ChannelHandlerContext ctx, Request req) {

        Response res;

        if (!AuthController.authenticateToken(req.getToken())) {
            res = ResponseBuilders.InvalidTokenResponse;
        } else {
            res = Response.newBuilder()
                    .setStatus(200)
                    .setMessage("Deleted" + req.getKey() + "Successfully.")
                    .build();
        }

        ctx.writeAndFlush(res);
        return;
    }

}