package client.handler;

import common.LogUtil;
// import common.parsing.protos.RequestProtos.Request;
import common.parsing.protos.ResponseProtos.Response;

import io.netty.channel.ChannelException;
import io.netty.channel.ChannelHandlerContext;

import client.EchoClient;
// import client.clientUtils.RequestUtils;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class EchoClientHandler extends ChannelInboundHandlerAdapter {

    private final EchoClient client;

    // Constructor ties itself to client instance.
    public EchoClientHandler(EchoClient client) {
        this.client = client;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws ChannelException {
        // Runs on channel initialization.
        try {

            // Request req = RequestUtils.makeAuthRequest();

            // if (EchoClient.DEBUG_CLIENT) LogUtil.log("Attempting auth from channelActive.");

            // ctx.writeAndFlush(req);

            // return;
        } catch (Exception e) {
            if (EchoClient.DEBUG_CLIENT) LogUtil.log("Error in EchoClientHandler ", "Error", e);
            // throw new ChannelException("Error occured in EchoClientHandler");
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            Response res;

            if (msg instanceof Response) {
                res = (Response) msg;
                // Error handling. Possibly decrypt and decompress here
                if (EchoClient.DEBUG_CLIENT) LogUtil.log("ChannelRead received.", "Response Id", res.getResponseId());
                if (EchoClient.DEBUG_CLIENT) LogUtil.log("Response received: ","ResponseId",res.getResponseId());
                client.completeFuture(res);


            } else {
                throw new Exception("Response object of wrong class" + msg.getClass());
            }

        } catch (Exception e) {
            if (EchoClient.DEBUG_CLIENT) LogUtil.log("Error in Client channelRead:", "Response data", (Response) msg);
            ctx.close();
        }
    }

}
