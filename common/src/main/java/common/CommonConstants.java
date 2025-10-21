package common;

import java.nio.charset.Charset;

public class CommonConstants {
    private CommonConstants() {
    }

    public static final int SERVER_PORT = 8080;
    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
    public static final String SERVER_HOST = "localhost";
    public static final int BYTE_SIZE = 1024 * 1024;
    public static final int INT_MAX = 5; // int32 fields are 4 bytes max
    public static final int INT_MIN = 1; // int32 may use 1 byte for 0-127
    public static final int PAYLOAD_LIMIT = 32; // MB
    public static final int FRAME_LENGTH_PREPEND = 4;

    // REQUEST CONSTRAINTS
    public static final int MAX_ACTION_SIZE = 1;

    // Token min will be same. token is forced.
    public static final int MAX_TOKEN_SIZE = 36 + 1; // UUiD is 36 chars long, 1 for the varInt used by Protobuf for
                                                     // length of String field

    // Key
    public static final int MAX_KEY_SIZE = 1024; // Arbitrary
    public static final int MIN_KEY_SIZE = 1 + 1; // Key always exists. 1 for the varInt used by Protobuf for length of
                                                  // String field

    // Key len
    public static final int MAX_KEY_LENGTH_SIZE = INT_MAX; // int
    public static final int MIN_KEY_LENGTH_SIZE = INT_MIN;

    // Val
    public static final int MAX_VALUE_SIZE = BYTE_SIZE * PAYLOAD_LIMIT;
    public static final int MIN_VALUE_SIZE = 0; // optional field;

    // Val len
    public static final int MAX_VALUE_LENGTH_SIZE = INT_MAX;
    public static final int MIN_VALUE_LENGTH_SIZE = INT_MIN;

    // Options length
    public static final int MAX_OPTIONS_SIZE = INT_MAX;
    public static final int MIN_OPTIONS_SIZE = INT_MIN;

    public static final int MAX_REQ_SIZE = 0 +
            FRAME_LENGTH_PREPEND +
            MAX_ACTION_SIZE +
            MAX_TOKEN_SIZE +
            MAX_KEY_SIZE + 1 +
            MAX_KEY_LENGTH_SIZE +
            MAX_VALUE_SIZE +
            MAX_VALUE_LENGTH_SIZE + 
            MAX_OPTIONS_SIZE;
    public static final int MIN_REQ_SIZE = 0 +
            FRAME_LENGTH_PREPEND +
            MAX_ACTION_SIZE +
            MAX_TOKEN_SIZE +
            MIN_KEY_SIZE +
            MIN_KEY_LENGTH_SIZE +
            MIN_VALUE_SIZE +
            MIN_VALUE_LENGTH_SIZE + 
            MIN_OPTIONS_SIZE;

    // RESPONSE CONSTRAINTS. Not needed
    public static final int MAX_STATUS_SIZE = INT_MAX;
    //

    public static final int MAX_MESSAGE = 8192; // Arbitrary
    //

    public static final int MAX_LENGTH_SIZE = INT_MAX;
    //

    public static final int MAX_VALUES_SIZE = BYTE_SIZE * PAYLOAD_LIMIT;
    //

    public static final int MAX_RES_SIZE = 0 +
            FRAME_LENGTH_PREPEND +
            MAX_STATUS_SIZE +
            MAX_MESSAGE +
            MAX_LENGTH_SIZE +
            MAX_VALUES_SIZE;
            
    public static final int MIN_RES_SIZE = 0 + 
            FRAME_LENGTH_PREPEND + 
            1 + // varInt status
            2 + // 1 + 1 min String message
            1 + // varInt length
            0;  // value

}
