package common;

import java.nio.charset.Charset;

public class Constants {
    private Constants() {}  // prevent constructions

    // public static final 
    public static final int MAX_FRAME_LENGTH = 1024*1024; //1MB
    public static final int MAX_CONNECTIONS_PER_TENANT = 100;
    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
    public static final int TOTAL_CACHE_SIZE = 1024*1024 * 500; // 500MB 
    public static final int ACTION_LENGTH = 3;

    public static final int SERVER_PORT = 8080;
    
}

