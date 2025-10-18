package commonCore;


import io.github.cdimascio.dotenv.Dotenv;

public class EnvConfig {
    private static final Dotenv dotenv = Dotenv.load();

    public static final String DB_URI_STRING = dotenv.get("DB_CONNECTION_STRING");
    public static final String DB_NAME = dotenv.get("DB_NAME");
    public static final String API_KEY = dotenv.get("API_KEY");
}