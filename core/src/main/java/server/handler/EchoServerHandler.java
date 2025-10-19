package server.handler;

import java.nio.charset.StandardCharsets;

import org.bson.Document;
import org.bson.types.ObjectId;

import common.LogUtil;
import common.parsing.protos.RequestProtos;
import common.parsing.protos.ResponseProtos;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import com.mongodb.client.model.Filters;

import db.MongoService;
// import models.RequestData;
// import models.ResponseData;

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
            RequestProtos.Request req;
            ResponseProtos.Response res = null;

            String message;
            int status;

            // Check correct format of request
            if (!(msg instanceof RequestProtos.Request)) {
                // throw new ChannelException("Request object is of wrong class :" +
                // msg.getClass());
                status = 400;
                message = "Request object is of wrong class :" + msg.getClass();
            } else {
                // Cast to Request object from deserialized msg
                req = (RequestProtos.Request) msg;

                LogUtil.log("Request received successfully :", "Request", req, "Action:", req.getAction(), "Key: ",
                        req.getKey());

                // if Auth type request
                switch (req.getAction()) {
                    case RequestProtos.Action.AUTH:

                        String tenantId = req.getKey();
                        String password = req.getValue().toStringUtf8();

                        Document tenantDocument = MongoService.UserCollection.find(Filters.eq("_id", new ObjectId(tenantId))).first();

                        // Ceck if failed auth against record from db
                        if (tenantDocument == null || !tenantDocument.getString("password").equals(password)) {
                            message = "Auth Failed. Check password.";
                            status = 401;
                            if (tenantDocument == null) {
                                status = 404;
                                message = "Tenant not found";
                            }

                        } else { // Auth success
                            message = "Auth Success. Client can send messages now.";
                            status = 200;
                        }
                        break;

                    default:
                        status = 500;
                        message = "Unexpected server error in channelHandling.";
                        break;
                }

            }
            ;

            res = ResponseProtos.Response.newBuilder()
                    .setStatus(status)
                    .setMessage(message)
                    .setLength(message.length())
                    .build();

            ctx.writeAndFlush(res);

            if (status == 404 || status == 401) {
                ctx.close();
            }

        } catch (

        Exception e) {
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