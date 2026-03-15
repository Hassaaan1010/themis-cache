package server.handlers;

import java.util.HashMap;

import com.google.protobuf.ByteString;

import application.AppContext;
import cache.command.Executable;
import common.LogUtil;
import common.parsing.protos.RequestProtos.Action;
import common.parsing.protos.RequestProtos.Request;
import common.parsing.protos.ResponseProtos.Response;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import queue.CommandQueue;
import server.EchoServer;
import server.controllers.AuthController;
import server.controllers.BadRequestController;
import server.controllers.CloseController;
import server.controllers.DelController;
import server.controllers.GetController;
import server.controllers.SetController;
import server.controllers.helpers.ResponseBuilders;

// import models.RequestData;
// import models.ResponseData;
public class EchoServerHandler extends ChannelInboundHandlerAdapter {

    private final AppContext context;

    private final CommandQueue cmdQueue;

    private final AuthController authController;

    public EchoServerHandler(AppContext context) {
        this.context = context;
        this.cmdQueue = this.context.getCommandQueue();

        // Init Controllers
        this.authController = new AuthController(context);
    }

    public static final HashMap<String, ByteString> Cache = new HashMap<>();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws ChannelException {
        try {
            if (EchoServer.DEBUG_SERVER) {
                LogUtil.log("Reached channel handler.");
            }

            // Check correct format of request
            if (!(msg instanceof Request)) {
                // res = BadRequestController.invalidRequestClass(Request.newBuilder().set);
                if (EchoServer.DEBUG_SERVER) {
                    LogUtil.log("Bad request sent. Could not be casted to Request type.");
                }
                ctx.close();
            }

            Response res = null;
            Executable cmd = null;

            // Cast to Request object from deserialized msg
            Request req = (Request) msg;
            Channel channel = ctx.channel();

            if (EchoServer.DEBUG_SERVER) {
                LogUtil.log("Request received successfully :", "Request key", req.getKey(), "Action:", req.getAction());
            }

            // Authenticate request
            if (!(req.getAction() == Action.AUTH)) {
                if (!authController.authenticateToken(req.getToken())) {
                    res = ResponseBuilders.makeInvalidTokenResponse(req.getRequestId());
                    ctx.writeAndFlush(res);
                    return;
                }
            }

            // Make cmd or req
            switch (req.getAction()) {
                case Action.GET ->
                    cmd = GetController.get(channel, req);

                case Action.SET ->
                    cmd = SetController.set(channel, req);

                case Action.DEL ->
                    cmd = DelController.delete(channel, req);

                case Action.AUTH ->
                    res = authController.authenticate(req);

                case Action.CLOSE ->
                    res = CloseController.close(req);

                default ->
                    res = BadRequestController.invalidRequestMethod(req.getRequestId());
            }

            // Command exists : GET, SET, DEL
            if (cmd != null) {
                cmdQueue.offer(cmd);
                return;
            }

            if (res == null) {
                throw new IllegalStateException("Response was not generated for action: " + req.getAction());
            }

            // Immediate return for AUTH, CLOSE
            if (req.getAction() == Action.AUTH || req.getAction() == Action.CLOSE) {
                // Flush in all cases
                ctx.writeAndFlush(res);

                // Conditional closing of channel
                if ((req.getAction() == Action.AUTH && res.getStatus() >= 400) // Auth fail
                        || (req.getAction() == Action.CLOSE && res.getStatus() == 200) // Req to close
                        ) {
                    ctx.close();
                } else {
                    LogUtil.log("res != null condition", "Action", req.getAction());
                }

                if (EchoServer.DEBUG_SERVER) {
                    LogUtil.log("Response was writen and flushed.");
                }
            }

        } catch (IllegalStateException e) {
            if (EchoServer.DEBUG_SERVER) {
                LogUtil.log("Channel read error:", "Error", e);
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        if (EchoServer.DEBUG_SERVER) {
            LogUtil.log("handlerAdded");
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        if (EchoServer.DEBUG_SERVER) {
            LogUtil.log("handlerRemoved");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        LogUtil.log("Unhandled exception in pipeline:", "error", cause);
        ctx.close();
    }

}
