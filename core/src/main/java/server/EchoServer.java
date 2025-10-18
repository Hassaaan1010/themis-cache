package server;


import common.CommonConstants;
import common.interfaces.Codec;
import common.parsing.protos.RequestProtos;
import common.parsing.protos.ResponseProtos;
import db.MongoService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
// import models.RequestData;
// import models.ResponseData;
import server.handler.EchoServerHandler;
// import server.parsing.ByteBufServerCodec;
import server.parsing.ProtobufServerCodec;
import common.LogUtil;

public class EchoServer {

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
                                    .addLast(codec.newDecoder())
                                    .addLast(codec.newEncoder())
                                    .addLast(new EchoServerHandler());
                        }
                    });

            channelFuture = b.bind(port).sync();
            LogUtil.log("Server started:", "Port", port);
            channelFuture.channel().closeFuture().sync();

        } catch (Exception e) {
            LogUtil.log("Error in Echo Server initialization : ", "Error", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public void shutdown() {
        LogUtil.log("Shutting down server gracefully...");
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
