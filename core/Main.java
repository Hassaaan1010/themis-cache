import queue.CommandQueue;
import server.EchoServer;

public class Main {

    public EchoServer nettyServer;
    public CommandQueue queue;
    // public static CacheWorker cacheworker;

    public void main(String[] args) throws Exception {
        
        
        // init
        queue = new CommandQueue();
        
        nettyServer = new EchoServer(queue);
        // cacheWorker = new CacheWorker(queue);...

        
        // Hooks for Ctrl+C or SIGTERM
        Runtime.getRuntime().addShutdownHook(new Thread(nettyServer::shutdown));
        Runtime.getRuntime().addShutdownHook(new Thread(nettyServer.tapDaemon::shutdown));

        // Start 
        nettyServer.start();
        // cacheWorker.start();

    }

}
