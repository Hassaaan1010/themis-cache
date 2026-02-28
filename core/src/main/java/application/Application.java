package application;

import server.EchoServer;

public class Application {

    public final AppContext context;
    public final EchoServer nettyServer;

    public Application() throws Exception {
        this.context = new AppContext();
        this.nettyServer = new EchoServer(context);
    }

    public void start() throws Exception {
        context.start();        // start daemons + poller
        nettyServer.start();    // start netty
    }

    public void shutdown() {
        nettyServer.shutdown();
        context.shutdown();
    }
}