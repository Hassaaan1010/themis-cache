package server.handler;
import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelException;
import common.LogUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class EchoServerHandler extends ChannelInboundHandlerAdapter {
    private ByteBuf buf;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        System.out.println("handlerAdded");
        buf = ctx.alloc().buffer(4); // (4 bytes)
        System.out.println("buffer added: "+ buf.toString(StandardCharsets.UTF_8)); 
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        System.out.println("handlerRemoved");
        buf.release();
        buf = null;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws ChannelException{
       try {
            ByteBuf m = (ByteBuf) msg;
            LogUtil.log("Reacher channel read : ", "byteBuf m ",m.toString());

       } catch (Exception e) {
            LogUtil.log("Channel read error:", "something", 123);
       }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}