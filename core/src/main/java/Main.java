import application.Application;


public class Main {
    public static void main(String[] args) throws Exception {
        long startTime = System.currentTimeMillis();
        System.out.println("STARTSSSSS");
        Application app = new Application();
        
        long endTime = System.currentTimeMillis();
        System.out.println("Time to start: " + (startTime - endTime));
        Thread.sleep(1000);
        app.start();
        
        Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));
    }
}