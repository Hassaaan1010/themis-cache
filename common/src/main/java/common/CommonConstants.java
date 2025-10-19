package common;

import java.nio.charset.Charset;

public class CommonConstants {
    private CommonConstants() {
    }

    public static final int ACTION_LENGTH = 3;
    public static final int SERVER_PORT = 8080;
    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
    public static final String SERVER_HOST = "localhost";
    public static final int BYTE_SIZE = 1024 * 1024;
    public static final int INT_MAX = 4; // int32 fields are 4 bytes
    public static final int PAYLOAD_LIMIT = 32; // MB

    // Req constraints
    int MAX_ACTION_SIZE = 1;
    int MAX_TOKEN_SIZE = 36; // UUiD is 36 chars long
    int MIN_TOKEN_SIZE = 36;
    int MAX_KEY_SIZE = 1024;
    int MAX_KEY_LENGTH_SIZE = INT_MAX;
    int MAX_VALUE_SIZE = BYTE_SIZE * PAYLOAD_LIMIT;
    int MAX_VALUE_LENGTH_SIZE = INT_MAX;

    int MAX_REQ_SIZE = MAX_ACTION_SIZE +
            MAX_TOKEN_SIZE +
            MAX_KEY_SIZE +
            MAX_KEY_LENGTH_SIZE +
            MAX_VALUE_SIZE +
            MAX_VALUE_LENGTH_SIZE;

    // Res constraints
    int MAX_STATUS_SIZE = INT_MAX;
    int MAX_MESSAGE = 8192; // Arbitrary
    int MAX_LENGTH_SIZE = INT_MAX;
    int MAX_VALUES_SIZE = BYTE_SIZE * PAYLOAD_LIMIT;

    int MAX_RES_SIZE = MAX_STATUS_SIZE + MAX_MESSAGE + MAX_LENGTH_SIZE + MAX_VALUES_SIZE;

}
