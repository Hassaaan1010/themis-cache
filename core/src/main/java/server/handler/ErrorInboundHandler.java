package server.handler;

import server.serverUtils.EchoException;

import common.LogUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

@Deprecated
public class ErrorInboundHandler extends ChannelInboundHandlerAdapter{

    int status = 0;
    String message;

    @Deprecated
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws NullPointerException {
        
        LogUtil.log("Error", cause.getMessage());

        cause.printStackTrace();

        if (cause instanceof EchoException e) {
            status = e.getStatus();
            message = e.getMessage();
        } else {
            status = 500;
            message = "Internal server error";
            
            LogUtil.log("Error was thrown without new EchoException(int status, String message).", "Error", cause.getMessage());
            cause.printStackTrace();
        }
     

        // ResponseProtos.Response res = ResponseBuilders.makeErrorResponse(this.status, this.message);

        // ctx.writeAndFlush(res);
    }
}
