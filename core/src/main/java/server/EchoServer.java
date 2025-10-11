package server;

import common.CommonConstants;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import server.handler.EchoServerHandler;
import server.parsing.RequestDataDecoder;
import server.parsing.ResponseDataEncoder;


public class EchoServer {
    private final int port;

    public EchoServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1); // accept incoming connections
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(); // handle traffic

        // "Parent vs Child in Netty (your FD model)
        // Listening FD (what you’d have with socket() + bind() + listen())
        // → In Netty this is the NioServerSocketChannel (parent channel).
        // → It lives in the boss group event loop (listening thread).
        // → Its only job: accept() incoming TCP connects.
        
        // Accepted FD (what you’d normally get from accept())
        // → In Netty this becomes a SocketChannel (child channel).
        // → Each child lives in the worker group event loops.
        // → It handles the actual read() / write() of data.
        // ";

        try {
            // make a server bootstraper
            ServerBootstrap b = new ServerBootstrap();
            // allocate listener and handler event loop groups to bootstraper
            b.group(bossGroup, workerGroup)
            // specify channel type to use
             .channel(NioServerSocketChannel.class) // TCP channel
            // listner channel (parent) allow to specify handler for accepted channels (children)
             .childHandler(new ChannelInitializer<SocketChannel>() { 
                 @Override
                 protected void initChannel(SocketChannel ch) {
                     ch.pipeline()
                     .addLast(new RequestDataDecoder())
                     .addLast(new ResponseDataEncoder())
                     .addLast(new EchoServerHandler());
                 }
             });

            ChannelFuture f = b.bind(port).sync();
            System.out.println("Server started on port " + port);

            f.channel().closeFuture().sync(); // wait until server socket closed
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        new EchoServer(CommonConstants.SERVER_PORT).start();
    }
}
