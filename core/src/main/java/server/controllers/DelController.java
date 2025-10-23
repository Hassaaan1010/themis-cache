package server.controllers;

import common.parsing.protos.RequestProtos.Request;
import common.parsing.protos.ResponseProtos.Response;
import io.netty.channel.ChannelHandlerContext;
import server.controllers.helpers.ResponseBuilders;

public class DelController {

    public static void delete(ChannelHandlerContext ctx, Request req) {

        Response res;

        if (!AuthController.authenticateToken(req.getToken())) {
            res = ResponseBuilders.makeInvalidTokenResponse(req.getToken(), req.getRequestId());
        } else {
            res = ResponseBuilders.makeDelResponse(204,"Key value pair deleted.",req.getRequestId() );
        }

        ctx.writeAndFlush(res);
        return;
    }

}