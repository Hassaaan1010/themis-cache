package commonSDK;

import io.github.cdimascio.dotenv.Dotenv;

public class EnvConfig {
    private static final Dotenv dotenv = Dotenv.load();

    public static final String TENANT_ID = dotenv.get("CACHE_TENANT_ID");
    public static final String TENANT_NAME = dotenv.get("CACHE_TENANT_NAME");
    public static final String PASSWORD = dotenv.get("CACHE_PASSWORD");
}