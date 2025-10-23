package server;

import common.CommonConstants;
import common.interfaces.Codec;
import common.parsing.protos.RequestProtos;
import common.parsing.protos.ResponseProtos;
import common.LogUtil;

import db.MongoService;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
// import io.netty.handler.timeout.ReadTimeoutHandler;

// import models.RequestData;
// import models.ResponseData;

import server.handler.EchoServerHandler;
import server.handler.SafeReqFrameDecoder;
// import server.parsing.ByteBufServerCodec;
import server.parsing.ProtobufServerCodec;

public class EchoServer {

    public static final boolean DEBUG_SERVER = false;

    private final int port;
    private ChannelFuture channelFuture;
    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;

    public EchoServer() {
        this.port = CommonConstants.SERVER_PORT;
    }

    public void start() throws Exception {
        try {
            // Force MongoService class to load and initialize DB
            Class.forName("db.MongoService");

            // Initialize Netty

            // Define number of boss vs worker threads
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();

            // Codec<RequestData, ResponseData> codec = new ByteBufServerCodec();
            Codec<RequestProtos.Request, ResponseProtos.Response> codec = new ProtobufServerCodec();

            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    // .addLast(new ReadTimeoutHandler(3))                  // close inactive connections
                                    .addLast(new SafeReqFrameDecoder())                     // inbound frame decoder
                                    .addLast(codec.newDecoder())                            // inbound protobuf decoder
                                    
                                    .addLast(new ProtobufVarint32LengthFieldPrepender())    // outbound length prepend
                                    .addLast(codec.newEncoder())                            // outbound protobuf encoder
                                    .addLast(new EchoServerHandler());                      // inbound business logic
                                    // .addLast(new ErrorInboundHandler());                    // inboundexception handling
                        }
                    });

            channelFuture = b.bind(port).sync();
            if (EchoServer.DEBUG_SERVER) LogUtil.log("Server started:", "Port", port);
            channelFuture.channel().closeFuture().sync();

        } catch (Exception e) {
            if (EchoServer.DEBUG_SERVER) LogUtil.log("Error in Echo Server initialization : ", "Error", e);
        } finally {
            if (EchoServer.DEBUG_SERVER) LogUtil.log("Shutting down server thread groups.");
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public void shutdown() {
        if (EchoServer.DEBUG_SERVER) LogUtil.log("Shutting down server gracefully...");
        if (channelFuture != null) {
            channelFuture.channel().close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        MongoService.closeClient();
    }

    public static void main(String[] args) throws Exception {
        EchoServer server = new EchoServer();

        // Hook for Ctrl+C or SIGTERM
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));

        server.start();
    }
}
