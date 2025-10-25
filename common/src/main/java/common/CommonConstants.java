package common;

import java.nio.charset.Charset;

public class CommonConstants {
        private CommonConstants() {
        }

        public static final int SERVER_PORT = 8080;
        public static final String SERVER_HOST = "localhost";

        // ========================
        // TIMEOUT CONSTANTS
        // ========================
        private static final int debugTimeoutCoeff = 10000;
        public static final long GET_REQUEST_TIMEOUT = debugTimeoutCoeff * 100;
        public static final long SET_REQUEST_TIMEOUT = debugTimeoutCoeff * 50; // Multiplied by a factor of length of payload. 
        public static final long DEL_REQUEST_TIMEOUT = debugTimeoutCoeff * 50;
        public static final long AUTH_REQUEST_TIMEOUT = debugTimeoutCoeff * 500; // DB read operation.



        public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

        // ========================
        // OPTIONS CONSTANTS
        // ========================
        public static final int encryptOption = 1;              // 0000 0001
        public static final int compressOption = 2;             // 0000 0010
        public static final int bigPayloadOption = 4;           // 0000 0100
        public static final int largePayloadOption = 8;         // 0000 1000

        // ========================
        // BASIC CONSTANTS
        // ========================
        public static final int MEGA_BYTE_SIZE = 1000 * 1000;
        public static final int INT_MAX = 5; // int32 varint: max 5 bytes for large values
        public static final int INT_MIN = 1; // int32 varint: min 1 byte for 0-127
        public static final int PAYLOAD_LIMIT = 101; // MB
        public static final int VARINT_TAG_SIZE = 1; // field tag is 1 byte for field numbers 1-15

        // ========================
        // REQUEST FIELD SIZES
        // ========================

        // Action (enum) - field 1
        // Protobuf: tag (1 byte) + varint value (1 byte for enum 0-4)
        public static final int MAX_ACTION_SIZE = VARINT_TAG_SIZE + INT_MIN;
        public static final int MIN_ACTION_SIZE = VARINT_TAG_SIZE + INT_MIN;

        // Token (string) - field 2
        // Protobuf: tag (1 byte) + length varint (1 byte for 36) + data (36 bytes)
        public static final int MAX_TOKEN_SIZE = VARINT_TAG_SIZE + INT_MIN + 36; // UUID is 36 chars
        public static final int MIN_TOKEN_SIZE = VARINT_TAG_SIZE + INT_MIN + 36; // always 36 chars

        // Key (string) - field 3
        // Protobuf: tag (1 byte) + length varint + data
        public static final int MAX_KEY_SIZE = VARINT_TAG_SIZE + INT_MIN + 1024; // arbitrary max
        public static final int MIN_KEY_SIZE = VARINT_TAG_SIZE + INT_MIN + 1; // min 1 char

        // Value (bytes) - field 4 - OPTIONAL
        // Protobuf: tag (1 byte) + length varint (max 5 bytes for 32MB) + data
        public static final int MAX_VALUE_SIZE = VARINT_TAG_SIZE + INT_MAX + (MEGA_BYTE_SIZE * PAYLOAD_LIMIT);
        public static final int MIN_VALUE_SIZE = 0; // optional, may not exist

        // Options (int32) - field 5 - OPTIONAL but could be 0
        // Protobuf: tag (1 byte) + varint value
        public static final int MAX_OPTIONS_SIZE = VARINT_TAG_SIZE + INT_MAX;
        public static final int MIN_OPTIONS_SIZE = 0; // could be absent (default 0)

        // RequestId (int32) - field 6
        // Protobuf: tag (1 byte) + varint value
        public static final int MAX_REQUEST_ID_SIZE = VARINT_TAG_SIZE + INT_MAX;
        public static final int MIN_REQUEST_ID_SIZE = VARINT_TAG_SIZE + INT_MIN;

        // TOTAL REQUEST SIZE (without frame length prepend)
        public static final int MAX_REQ_SIZE = MAX_ACTION_SIZE +
                        MAX_TOKEN_SIZE +
                        MAX_KEY_SIZE +
                        MAX_VALUE_SIZE +
                        MAX_OPTIONS_SIZE +
                        MAX_REQUEST_ID_SIZE;

        public static final int MIN_REQ_SIZE = MIN_ACTION_SIZE +
                        MIN_TOKEN_SIZE +
                        MIN_KEY_SIZE +
                        MIN_VALUE_SIZE +
                        MIN_REQUEST_ID_SIZE; // options can be absent, but requestId is always there

        // ========================
        // RESPONSE FIELD SIZES
        // ========================

        // Status (int32) - field 1
        // Protobuf: tag (1 byte) + varint value (2 bytes for 200-599)
        public static final int MAX_STATUS_SIZE = VARINT_TAG_SIZE + INT_MIN;
        public static final int MIN_STATUS_SIZE = VARINT_TAG_SIZE + INT_MIN;

        // Message (string) - field 2 - could be empty
        // Protobuf: tag (1 byte) + length varint + data
        public static final int MAX_MESSAGE_SIZE = VARINT_TAG_SIZE + INT_MIN + 8192; // arbitrary max
        public static final int MIN_MESSAGE_SIZE = VARINT_TAG_SIZE + INT_MIN + 0; // empty string

        // Value (bytes) - field 3 - OPTIONAL
        // Protobuf: tag (1 byte) + length varint + data
        public static final int MAX_VALUE_RES_SIZE = VARINT_TAG_SIZE + INT_MAX + (MEGA_BYTE_SIZE * PAYLOAD_LIMIT);
        public static final int MIN_VALUE_RES_SIZE = 0; // optional

        // ResponseId (int32) - field 4
        // Protobuf: tag (1 byte) + varint value
        public static final int MAX_RESPONSE_ID_SIZE = VARINT_TAG_SIZE + INT_MAX;
        public static final int MIN_RESPONSE_ID_SIZE = VARINT_TAG_SIZE + INT_MIN;

        // TOTAL RESPONSE SIZE (without frame length prepend)
        public static final int MAX_RES_SIZE = MAX_STATUS_SIZE +
                        MAX_MESSAGE_SIZE +
                        MAX_VALUE_RES_SIZE +
                        MAX_RESPONSE_ID_SIZE;

        public static final int MIN_RES_SIZE = MIN_STATUS_SIZE +
                        MIN_MESSAGE_SIZE +
                        MIN_RESPONSE_ID_SIZE; // value is optional

        

        {

                // public static final int BYTE_SIZE = 1024 * 1024;
                // public static final int INT_MAX = 5; // int32 fields are 4 bytes max
                // public static final int INT_MIN = 1; // int32 may use 1 byte for 0-127
                // public static final int PAYLOAD_LIMIT = 32; // MB
                // public static final int FRAME_LENGTH_PREPEND = 4;

                // // REQUEST CONSTRAINTS
                // public static final int MAX_ACTION_SIZE = 1;

                // // Token min will be same. token is forced.
                // public static final int MAX_TOKEN_SIZE = 36 + 1; // UUiD is 36 chars long, 1
                // for the varInt used by Protobuf for
                // // length of String field

                // // Key
                // public static final int MAX_KEY_SIZE = 1024; // Arbitrary
                // public static final int MIN_KEY_SIZE = 1 + 1; // Key always exists. 1 for the
                // varInt used by Protobuf for length of
                // // String field

                // // Key len
                // public static final int MAX_KEY_LENGTH_SIZE = INT_MAX; // int
                // public static final int MIN_KEY_LENGTH_SIZE = INT_MIN;

                // // Val
                // public static final int MAX_VALUE_SIZE = BYTE_SIZE * PAYLOAD_LIMIT;
                // public static final int MIN_VALUE_SIZE = 0; // optional field;

                // // Val len
                // public static final int MAX_VALUE_LENGTH_SIZE = INT_MAX;
                // public static final int MIN_VALUE_LENGTH_SIZE = INT_MIN;

                // // Options length
                // public static final int MAX_OPTIONS_SIZE = INT_MAX;
                // public static final int MIN_OPTIONS_SIZE = INT_MIN;

                // public static final int MAX_REQ_SIZE = 0 +
                // FRAME_LENGTH_PREPEND +
                // MAX_ACTION_SIZE +
                // MAX_TOKEN_SIZE +
                // MAX_KEY_SIZE + 1 +
                // // MAX_KEY_LENGTH_SIZE +
                // MAX_VALUE_SIZE +
                // // MAX_VALUE_LENGTH_SIZE +
                // MAX_OPTIONS_SIZE;
                // public static final int MIN_REQ_SIZE = 0 +
                // FRAME_LENGTH_PREPEND +
                // MAX_ACTION_SIZE +
                // MAX_TOKEN_SIZE +
                // MIN_KEY_SIZE +
                // // MIN_KEY_LENGTH_SIZE +
                // MIN_VALUE_SIZE +
                // // MIN_VALUE_LENGTH_SIZE +
                // MIN_OPTIONS_SIZE;

                // // RESPONSE CONSTRAINTS. Not needed
                // public static final int MAX_STATUS_SIZE = INT_MAX;
                // //

                // public static final int MAX_MESSAGE = 8192; // Arbitrary
                // //

                // public static final int MAX_LENGTH_SIZE = INT_MAX;
                // //

                // public static final int MAX_VALUES_SIZE = BYTE_SIZE * PAYLOAD_LIMIT;
                // //

                // public static final int MAX_RES_SIZE = 0 +
                // FRAME_LENGTH_PREPEND +
                // MAX_ACTION_SIZE +
                // MAX_STATUS_SIZE +
                // MAX_MESSAGE +
                // MAX_LENGTH_SIZE +
                // MAX_VALUES_SIZE;

                // public static final int MIN_RES_SIZE = 0 +
                // FRAME_LENGTH_PREPEND +
                // MAX_ACTION_SIZE +
                // 1 + // varInt status
                // 2 + // 1 + 1 min String message
                // 1 + // varInt length
                // 0; // value
        }

}
