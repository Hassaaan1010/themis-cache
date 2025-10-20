package client;

import client.clientUtils.ErrorInboundHandler;

// import com.google.protobuf.ByteString;

import client.handler.EchoClientHandler;
import client.handler.SafeResFrameDecoder;
// import client.parsing.ByteBufClientCodec;
import client.parsing.ProtobufClientCodec;
import common.CommonConstants;
import common.LogUtil;
import common.interfaces.Codec;
import common.parsing.protos.RequestProtos;
import common.parsing.protos.ResponseProtos;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
// import models.RequestData;
// import models.ResponseData;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

public class EchoClient {

    private ChannelFuture channelFuture;
    private NioEventLoopGroup workerGroup;

    public String token;

    public EchoClient() {
    }

    public void start() throws Exception {
        try {
            workerGroup = new NioEventLoopGroup();
            // Codec<RequestData, ResponseData> codec = new ByteBufClientCodec();
            Codec<RequestProtos.Request, ResponseProtos.Response> codec = new ProtobufClientCodec();

            Bootstrap b = new Bootstrap();
            b.group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                            // .
                            // .addLast(new )
                            .addLast(new SafeResFrameDecoder())
                            .addLast(codec.newDecoder())
                            .addLast(new ProtobufVarint32LengthFieldPrepender())
                            .addLast(codec.newEncoder())
                            .addLast(new EchoClientHandler())
                            .addLast(new ErrorInboundHandler());
                    }
                });

            channelFuture = b.connect(CommonConstants.SERVER_HOST, CommonConstants.SERVER_PORT).sync();
            System.out.println("Client connected to " + CommonConstants.SERVER_HOST + ":" + CommonConstants.SERVER_PORT);

            channelFuture.channel().closeFuture().sync();

        } catch (Exception e) {
            LogUtil.log("Error in Echo Client : ", "Error", e);
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    public void shutdown() {
        System.out.println("Shutting down client gracefully...");
        if (channelFuture != null) {
            channelFuture.channel().close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        EchoClient client = new EchoClient();

        // Hook for Ctrl+C or SIGTERM
        Runtime.getRuntime().addShutdownHook(new Thread(client::shutdown));
        
        client.start();
    }

    // public static void getKey(String key) {
        
    // }

    // public static boolean setKey(String key, Byte[] Value) {
        
    // }

    // public static deleteKey(String key) {

    // }
}
