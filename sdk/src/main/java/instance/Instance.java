package instance;

import com.google.protobuf.ByteString;

import java.nio.file.Path;

import client.EchoClient;
import common.LogUtil;
import common.parsing.protos.ResponseProtos.Response;

public class Instance {

    private static EchoClient cacheClient;

    public static void main(String[] args) {
        ;
        try {
            cacheClient = new EchoClient();

            cacheClient.start();

            System.out.println("Starting Auth operation_____________________________________________");
            LogUtil.log("Sending Auth");
            Response authResponse = cacheClient.authenticate();

            // LogUtil.log("Receieved Auth", "Auth response", authResponse);

            if (authResponse.getStatus() == 200) {
                LogUtil.log("Auth was successful.", "Status:", authResponse.getStatus());
            } else {
                LogUtil.log("Response without 200 status.", "Response", authResponse);
                throw new Error("Authentication failed");

            }

            System.out.println("Starting Set operation _____________________________________________");
            /*
             * SET sample
             */
            // String value = "PONG";
            // ByteString payloadValue = ByteString.copyFromUtf8(value);

            Path filePath = Path.of("/home/hassaan/Project/loadTestFiles/10_MB_FILE.bin");
            ByteString payloadValue = EchoClient.readFiletoByteString(filePath);

            System.out.println("File read to memory.");
            Response setResponse = cacheClient.setKey("PING", payloadValue, false, false);

            // Validate response
            if (setResponse.getStatus() == 201) {
                // String message = setResponse.getMessage();
                // LogUtil.log(message, "Response status: ", setResponse.getStatus(), "Set
                // message:", message);
                LogUtil.log("Response for SET recieved with success.");

            } else {
                LogUtil.log("Response without 201 status.", "Response", setResponse);
                throw new Exception("Set request failed.");
            }

            for (int i = 0; i < 100_000/2; i++) {
                // Thread.sleep(2000);

                callGet("PING");

            }

            LogUtil.log("Completed successfully");

	    for (int i = 0; i < 1_000_000; i++){
	    }
	    LogUtil.log("Looped successfully");

        } catch (Exception e) {
            cacheClient.shutdown();
            LogUtil.log("Error in Instance main method.", "Error message", e.getMessage(), "Error", e);
        }

    }

    private static void callGet(String key) {
        System.out.println("Starting Get operation _____________________________________________");
        /*
         * GET sample
         */
        cacheClient.getKey(key, false, true); // TODO: payload size should be ENUM.

        // Validate response
        /*
	if (getResponse.getStatus() == 200) {
            // String returnedMessage = getResponse.getMessage();
            // LogUtil.log("Response received successfully", "Value", returnedMessage);
            LogUtil.log("Response for GET recieved with success.");

        } else {
            LogUtil.log("Get request failed. Response without 200 status.", "Response", getResponse);
            
            // throw new Exception("Get request failed.");
        }
	*/

    }

}
