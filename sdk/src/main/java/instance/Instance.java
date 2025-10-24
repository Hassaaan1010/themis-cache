package instance;

import com.google.protobuf.ByteString;

import java.nio.file.Path;

import client.EchoClient;
import common.LogUtil;
import common.parsing.protos.ResponseProtos.Response;

public class Instance {

    public static void main(String[] args) {
        EchoClient cacheClient = new EchoClient();
        ;
        try {
            cacheClient.start();


            if (EchoClient.DEBUG_CLIENT) LogUtil.log("Sending Auth");
            Response authResponse = cacheClient.authenticate();

            // if (EchoClient.DEBUG_CLIENT) LogUtil.log("Receieved Auth", "Auth response", authResponse);

            if (authResponse.getStatus() == 200) {
                if (EchoClient.DEBUG_CLIENT) LogUtil.log("Auth was successful.", "Status:", authResponse.getStatus());
            } else {
                if (EchoClient.DEBUG_CLIENT) LogUtil.log("Response without 200 status.", "Response", authResponse);
                throw new Error("Authentication failed");

            }

            System.out.println("Starting Set operation _____________________________________________");
            /*
             * SET sample
             */
            // String value = "PONG";
            // ByteString payloadValue = ByteString.copyFromUtf8(value);

            Path filePath = Path.of("/home/hassaan/Project/loadTestFiles/100_MB_FILE.bin");
            ByteString payloadValue = EchoClient.readFiletoByteString(filePath);

            Response setResponse = cacheClient.setKey("PING", payloadValue, false, false);

            // Validate response
            if (setResponse.getStatus() == 201) {
                String message = setResponse.getMessage();
                if (EchoClient.DEBUG_CLIENT) LogUtil.log(message, "Response status: ", setResponse.getStatus(), "Set message:", message);
            } else {
                if (EchoClient.DEBUG_CLIENT) LogUtil.log("Response without 201 status.", "Response", setResponse);
                throw new Exception("Set request failed.");
            }

            System.out.println("Starting Get operation _____________________________________________");

            /*
             * GET sample
             */
            Response getResponse = cacheClient.getKey("PING",false,true); // TODO: payload size should be ENUM.

            // Validate response
            if (getResponse.getStatus() == 200) {
                String returnedMessage = getResponse.getMessage();
                if (EchoClient.DEBUG_CLIENT) LogUtil.log("Response received successfully", "Value", returnedMessage);
            } else {
                if (EchoClient.DEBUG_CLIENT) LogUtil.log("Response without 200 status.", "Response", getResponse);

                throw new Exception("Get request failed.");
            }

            LogUtil.log("Completed successfully");
        } catch (Exception e) {
            cacheClient.shutdown();
            if (EchoClient.DEBUG_CLIENT) LogUtil.log("Error in Instance main method.", "Error message", e.getMessage(), "Error", e);
        }

    }

}
