package client.handler;


import client.EchoClient;
import common.LogUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ErrorInboundHandler extends ChannelInboundHandlerAdapter{

    int status = 0;
    String message;
    int requestId; // source ?

    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws NullPointerException {
        
        if (EchoClient.DEBUG_CLIENT) LogUtil.log("Error", cause.getMessage());

        cause.printStackTrace();

        status = 500;
        message = "Internal server error. Possibly recieved invalid response.";
        
        if (EchoClient.DEBUG_CLIENT) LogUtil.log("Error was thrown without new EchoException(int status, String message).", "Error", cause.getMessage());
        cause.printStackTrace();
    

        // Removed because requestId can not be extracted from Erroneous response. We back-off and try again. 
        // ResponseProtos.Response res = RequestUtils.makeUpErrorResponse(this.status, this.message, this.requestId);
        // ctx.writeAndFlush(res);

    }
}
