package server.handler;

import java.nio.charset.StandardCharsets;

import common.LogUtil;
import common.parsing.protos.RequestProtos.Request;
import common.parsing.protos.ResponseProtos.Response;
import common.parsing.protos.RequestProtos.Action;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import server.controllers.AuthController;
import server.controllers.BadRequestController;
import server.controllers.CloseController;
import server.controllers.DelController;
import server.controllers.GetController;
import server.controllers.SetController;

// import models.RequestData;
// import models.ResponseData;

public class EchoServerHandler extends ChannelInboundHandlerAdapter {
    private ByteBuf buf;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        System.out.println("handlerAdded");
        // TODO: Change constant init buffer capacity written as 4
        buf = ctx.alloc().buffer(4); // (4 bytes)
        System.out.println("buffer added: " + buf.toString(StandardCharsets.UTF_8));
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        System.out.println("handlerRemoved");
        buf.release();
        buf = null;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws ChannelException {
        try {
            LogUtil.log("Reached channel read : ", "Got msg from client ", msg.toString());

            Request req;
            Response res = null;

            // Check correct format of request
            if (!(msg instanceof Request)) {
                // throw new ChannelException("Request object is of wrong class :" +
                // msg.getClass());
                res = BadRequestController.invalidRequestClass();

            } else {
                // Cast to Request object from deserialized msg
                req = (Request) msg;

                LogUtil.log("Request received successfully :", "Request", req, "Action:", req.getAction(), "Key: ",
                        req.getKey());

                switch (req.getAction()) {
                    case Action.GET:
                        GetController.get(ctx, req);
                        return;

                    case Action.SET:
                        SetController.set(ctx, req);
                        return;

                    case Action.DEL:
                        DelController.delete(ctx, req);
                        return;

                    case Action.AUTH:
                        res = AuthController.authenticate(req);
                        ctx.writeAndFlush(res);
                        if (res.getStatus() >= 400) {
                            ctx.close();
                        }
                        return;

                    case Action.CLOSE:
                        res = CloseController.close(req);
                        ctx.writeAndFlush(res);
                        if (res.getStatus() == 200){
                            ctx.close();
                        }
                        return;
                        
                    default:
                        res = BadRequestController.invalidRequestMethod();
                        ctx.writeAndFlush(res);
                        return;
                }
            }

        } catch (

        Exception e) {
            LogUtil.log("Channel read error:", "Error", e);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}