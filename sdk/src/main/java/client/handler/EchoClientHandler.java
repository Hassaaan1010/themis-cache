package client.handler;

import common.LogUtil;
import common.parsing.protos.RequestProtos;
import common.parsing.protos.ResponseProtos;
import commonSDK.EnvConfig;
// import common.parsing.protos.RequestProtos.Request;
import io.netty.channel.ChannelException;
// import io.netty.channel.ChannelFuture;
// import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import com.google.protobuf.ByteString;
import io.netty.channel.ChannelInboundHandlerAdapter;
// import models.RequestData;
// import models.ResponseData;

public class EchoClientHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws ChannelException {
        try {
            // Runs on channel initialization. 
            
            String tenantId = EnvConfig.TENANT_ID;
            String password = EnvConfig.PASSWORD;

            // final String key = "PING";
            RequestProtos.Request req = RequestProtos.Request.newBuilder()
                    .setAction(RequestProtos.Action.AUTH)
                    .setKey(tenantId)
                    .setKeyLength(tenantId.length())
                    .setValue(ByteString.copyFromUtf8(password))
                    .setValueLength(password.length())
                    .build();
            LogUtil.log("Attempting auth from channelActive.");
            
            ctx.writeAndFlush(req);

        } catch (Exception e) {
            LogUtil.log("Error in EchoClientHandler ", "Error", e);
            // throw new ChannelException("Error occured in EchoClientHandler");
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            ResponseProtos.Response res;

            if (msg instanceof ResponseProtos.Response) {
                res = (ResponseProtos.Response) msg;

                LogUtil.log("Response received:", "Response:", msg, "Value: ", res.getValue());

            } else {
                throw new Exception("Response object of wrong class" + msg.getClass());
            }

            LogUtil.log(null, "ResponseData : ", res);
            // ctx.close();
        } catch (Exception e) {
            LogUtil.log("Error in Client channelRead:", "Response data", (ResponseProtos.Response) msg);
            ctx.close();
        }
    }
}
