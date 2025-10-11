package client;

import client.handler.EchoClientHandler;
import client.parsing.RequestDataEncoder;
import client.parsing.ResponseDataDecoder;
import common.CommonConstants;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;


public class EchoClient {
    private final int port;
    private final String host;

    public EchoClient (int port, String host) {
        this.port = port;
        this.host = host;
    }
    

    public static void main(String[] args) throws Exception {
        
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.handler(new ChannelInitializer<SocketChannel>() {
 
                @Override
                public void initChannel(SocketChannel ch) 
                  throws Exception {
                    ch.pipeline()
                    .addLast(new RequestDataEncoder())
                    .addLast(new ResponseDataDecoder())
                    .addLast(new EchoClientHandler());
                }
            });

            ChannelFuture f = b.connect(CommonConstants.SERVER_HOST, CommonConstants.SERVER_PORT).sync();
            f.channel().closeFuture().sync();

        } catch (Exception e) {

        } finally {
            workerGroup.shutdownGracefully();
        }
    }
}

