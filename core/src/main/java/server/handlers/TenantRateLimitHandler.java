package server.handlers;

import common.LogUtil;
import common.parsing.protos.RequestProtos.Action;
import common.parsing.protos.RequestProtos.Request;
import common.parsing.protos.ResponseProtos.Response;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import server.EchoServer;
import server.controllers.AuthController;
import server.controllers.helpers.ResponseBuilders;
import server.serverUtils.BucketsOwner;

public class TenantRateLimitHandler extends ChannelInboundHandlerAdapter {

    BucketsOwner tokenBuckets;

    public TenantRateLimitHandler(BucketsOwner buckets) {
        this.tokenBuckets = buckets;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws ChannelException {
        try {
            if (EchoServer.DEBUG_SERVER)
                LogUtil.log("Reached rate limiter.");

            // Check correct format of request
            if (!(msg instanceof Request)) {
                // throw new ChannelException("Request object is of wrong class :" +

                // res = BadRequestController.invalidRequestClass(Request.newBuilder().set);
                if (EchoServer.DEBUG_SERVER)
                    LogUtil.log("Bad request send. Could not be casted to Request type.");
                ReferenceCountUtil.release(msg); // drop msg from buffer

            } else {
               
                // Cast to Request object from deserialized msg
                Request req = (Request) msg;
                String tenantToken;

                // If AUTH req, make token from id, password
                if (req.getAction() == Action.AUTH) {
                    tenantToken = AuthController.getMurmurHash.apply(req.getKey(), req.getValue().toStringUtf8());
                } else {
                    tenantToken = req.getToken();
                }

                // Rate limit check
                if (!this.tokenBuckets.tryBucketDecrement(tenantToken)){
                    Response res = ResponseBuilders.makeRateLimitResponse(req.getRequestId());
                    ctx.writeAndFlush(res);

                    if (EchoServer.DEBUG_SERVER) LogUtil.log("Hit rate limit. Slow down");
                    return;
                    
                } else {
                    // continues to next handler
                    ctx.fireChannelRead(msg);
                }
            }

        } catch (Exception e) {
            if (EchoServer.DEBUG_SERVER) LogUtil.log("Error in Rate limiter: ", "Error", e);
            e.printStackTrace();
        }
    }
}
