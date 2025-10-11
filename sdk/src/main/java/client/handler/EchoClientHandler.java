package client.handler;

import common.LogUtil;
import io.netty.channel.ChannelException;
// import io.netty.channel.ChannelFuture;
// import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import models.RequestData;
import models.ResponseData;

public class EchoClientHandler extends ChannelOutboundHandlerAdapter{
    public void channelActive(ChannelHandlerContext ctx) throws ChannelException {
        try {
            final String message = "PING";
     
            RequestData msg = new RequestData();
    
            msg.setMessage(message);
            msg.setAction("GET");
            msg.setLength(message.length());
    
            ctx.writeAndFlush(msg);
            // future.addListener(ChannelFutureListener.CLOSE);
            LogUtil.log("Log from EchoClient Handler", "msg", msg);
            
        } catch (Exception e) {
            // TODO: handle exception
            throw new ChannelException("Error occured in EchoClientHandler");
        }
    }   

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println((ResponseData)msg);
        ctx.close();
    }
}
