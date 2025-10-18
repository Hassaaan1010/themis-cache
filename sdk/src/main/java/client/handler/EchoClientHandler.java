package client.handler;

import common.LogUtil;
import io.netty.channel.ChannelException;
// import io.netty.channel.ChannelFuture;
// import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import models.RequestData;
import models.ResponseData;

public class EchoClientHandler extends ChannelInboundHandlerAdapter{
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws ChannelException {
        try {
            final String message = "PING";
     
            RequestData msg = new RequestData();
    
            msg.setLength(message.length());
            msg.setMessage(message);
            msg.setAction("GET");
            for (int i = 0; i < 10; i++) {
                ctx.write(msg);
            }
            ctx.flush();
            // future.addListener(ChannelFutureListener.CLOSE);
            // LogUtil.log("Log from EchoClient Handler", "msg", msg);
            
        } catch (Exception e) {
            LogUtil.log("Error in EchoClientHandler ", "Error", e);
            // throw new ChannelException("Error occured in EchoClientHandler");
        }
    }   

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            System.out.println("Reached Channel Handler");
            ResponseData resp = (ResponseData) msg;
            LogUtil.log( null, "ResponseData : ",resp);
            // ctx.close();
        } catch (Exception e) {
            LogUtil.log("Error in Client channelRead:", "Response data", (ResponseData) msg);
        }
    }
}
