package server.handler;

import java.nio.charset.StandardCharsets;

import common.LogUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import models.RequestData;
import models.ResponseData;

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
            RequestData req = (RequestData) msg;

            if ("GET".equals(req.getAction()) && "PING".equals(req.getMessage())) {
                ResponseData res = new ResponseData();

                res.setStatus(200);
                res.setLength(4);
                res.setData("PONG");

                ctx.writeAndFlush(res);
            } else {
                throw new ChannelException("Request not as expected" + req);
            }

        } catch (Exception e) {
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