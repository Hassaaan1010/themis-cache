package client;

import client.handler.EchoClientHandler;
import client.parsing.ByteBufClientCodec;
import common.CommonConstants;
import common.LogUtil;
import common.interfaces.Codec;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import models.RequestData;
import models.ResponseData;

public class EchoClient {

    private ChannelFuture channelFuture;
    private NioEventLoopGroup workerGroup;

    public EchoClient() {
    }

    public void start() throws Exception {
        try {
            workerGroup = new NioEventLoopGroup();
            Codec<RequestData, ResponseData> codec = new ByteBufClientCodec();

            Bootstrap b = new Bootstrap();
            b.group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                            .addLast(codec.newDecoder())
                            .addLast(codec.newEncoder())
                            .addLast(new EchoClientHandler());
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
}
