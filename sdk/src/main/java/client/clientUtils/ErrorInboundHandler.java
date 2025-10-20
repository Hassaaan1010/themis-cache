package client.clientUtils;


import common.LogUtil;
import common.parsing.protos.ResponseProtos;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ErrorInboundHandler extends ChannelInboundHandlerAdapter{

    int status = 0;
    String message;

    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws NullPointerException {
        
        LogUtil.log("Error", cause.getMessage());

        cause.printStackTrace();

        status = 500;
        message = "Internal server error. Possibly recieved invalid response.";
        
        LogUtil.log("Error was thrown without new EchoException(int status, String message).", "Error", cause.getMessage());
        cause.printStackTrace();
    

        ResponseProtos.Response res = ReqUtils.makeUpErrorResponse(this.status, this.message);

        ctx.writeAndFlush(res);
    }
}
