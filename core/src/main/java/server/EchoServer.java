package server;



import application.AppContext;
import common.CommonConstants;
import common.LogUtil;
import common.interfaces.Codec;
import common.parsing.protos.RequestProtos;
import common.parsing.protos.ResponseProtos;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import server.handlers.EchoServerHandler;
import server.handlers.SafeReqFrameDecoder;
import server.handlers.TenantRateLimitHandler;
import server.parsing.ProtobufServerCodec;
import server.serverUtils.BucketsOwner;


public class EchoServer{

    public static final boolean DEBUG_SERVER = true;

    private final AppContext context; 
    private final BucketsOwner tokenBuckets;

    private final int port;
    private ChannelFuture channelFuture;
    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;

    public EchoServer(AppContext context ) throws Exception {
        this.port = CommonConstants.SERVER_PORT;
        
        this.context = context;
        this.tokenBuckets = this.context.getBucketsOwner();
        try {
            // Bootstrap bucket owner  
    
            // Server hooks for shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    
        } catch (Exception e) {
            LogUtil.log("Echo Server Initialization failed.","Exception", e);
        }
    }

    public void start() throws Exception {
        try {

            // // Start Tap Daemon
            // this.tapDaemon.start();

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
                                    
                                    .addLast(new TenantRateLimitHandler(tokenBuckets))
                                    .addLast(new EchoServerHandler(context));               // inbound business logic
                                    // .addLast(new ErrorInboundHandler());                 // inboundexception handling
                        }
                    });

            channelFuture = b.bind(port).sync();
            if (EchoServer.DEBUG_SERVER) LogUtil.log("Server started:", "Port", port);
            channelFuture.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            if (EchoServer.DEBUG_SERVER) LogUtil.log("Error in Echo Server starting : ", "Error", e);
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
    }

}
