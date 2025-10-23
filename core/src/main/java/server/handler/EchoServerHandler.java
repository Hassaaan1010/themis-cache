package server.handler;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import com.google.protobuf.ByteString;

import common.LogUtil;
import common.parsing.protos.RequestProtos.Request;
import common.parsing.protos.ResponseProtos.Response;
import common.parsing.protos.RequestProtos.Action;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import server.EchoServer;
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

    public static final HashMap<String, ByteString> Cache = new HashMap<>();

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        System.out.println("handlerAdded");
        // TODO: Remove? constant init buffer capacity written as 4
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
            if (EchoServer.DEBUG_SERVER) LogUtil.log("Reached channel read.");

            Request req;
            Response res = null;

            // Check correct format of request
            if (!(msg instanceof Request)) {
                // throw new ChannelException("Request object is of wrong class :" +
                
                // res = BadRequestController.invalidRequestClass(Request.newBuilder().set);
                if (EchoServer.DEBUG_SERVER) LogUtil.log("Bad request send. Could not be casted to Request type.");
            } else {
                // Cast to Request object from deserialized msg
                req = (Request) msg;

                if (EchoServer.DEBUG_SERVER) LogUtil.log("Request received successfully :", "Request", req, "Action:", req.getAction(), "Key: ",
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
                        System.out.println("Response was writen and flushed.");
                        if (res.getStatus() >= 400) {
                            ctx.close();
                        }
                        return;

                    case Action.CLOSE:
                        res = CloseController.close(req);
                        ctx.writeAndFlush(res);
                        if (res.getStatus() == 200) {
                            AuthController.removeToken(req.getToken());
                            ctx.close();
                        }
                        return;

                    default:
                        res = BadRequestController.invalidRequestMethod(req.getRequestId());
                        ctx.writeAndFlush(res);
                        return;
                }
            }

        } catch (

        Exception e) {
            if (EchoServer.DEBUG_SERVER) LogUtil.log("Channel read error:", "Error", e);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}