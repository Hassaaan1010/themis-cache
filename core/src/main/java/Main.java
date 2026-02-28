import application.Application;


public class Main {
    public static void main(String[] args) throws Exception {
        Application app = new Application();

        app.start();
        
        Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));
    }
}