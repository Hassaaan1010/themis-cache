package common;

import java.nio.charset.Charset;

public class CommonConstants {
    private CommonConstants() {}

    public static final int ACTION_LENGTH = 3;
    public static final int SERVER_PORT = 8080;
    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
    public static final String SERVER_HOST = "localhost";
    public static final int MAX_VALUE_BYTES_MB = 64; // JVM and PROTOBUF support 64MB safely;
    public static final int BYTE_SIZE = 1024 * 1024;
}
