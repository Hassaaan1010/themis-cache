package server.controllers;

import common.LogUtil;
import common.parsing.protos.RequestProtos.Request;
import common.parsing.protos.ResponseProtos.Response;
import io.netty.channel.ChannelHandlerContext;
import server.controllers.helpers.ResponseBuilders;

public class SetController {

    public static void set(ChannelHandlerContext ctx, Request req) {

        Response res;

        if (!AuthController.authenticateToken(req.getToken())) {
            res = ResponseBuilders.makeInvalidTokenResponse(req.getToken(), req.getRequestId());
        } else {
            GetController.Cache.put(req.getKey(), req.getValue());

            res = ResponseBuilders.makeSetResponse(201, "Key was set successfully.", req.getRequestId());
        }

        ctx.writeAndFlush(res);

        LogUtil.log("SET response was logged and flushed", "Response", res, "RequestId", req.getRequestId(),
                "ResponseId", res.getResponseId());
        return;
    }

}