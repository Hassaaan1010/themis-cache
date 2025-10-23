package instance;

import com.google.protobuf.ByteString;

import client.EchoClient;
import common.LogUtil;
import common.parsing.protos.ResponseProtos.Response;

public class Instance {

    public static void main(String[] args) {
        EchoClient cacheClient = new EchoClient();
        ;
        try {
            cacheClient.start();


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
            String value = "PONG";
            ByteString payloadValue = ByteString.copyFromUtf8(value);

            Response setResponse = cacheClient.setKey("PING", payloadValue, false, false);

            // Validate response
            if (setResponse.getStatus() == 201) {
                String message = setResponse.getMessage();
                LogUtil.log(message, "Response status: ", setResponse.getStatus(), "Set message:", message);
            } else {
                LogUtil.log("Response without 201 status.", "Response", setResponse);
                throw new Exception("Set request failed.");
            }

            System.out.println("Starting Get operation _____________________________________________");

            /*
             * GET sample
             */
            Response getResponse = cacheClient.getKey("PING");

            // Validate response
            if (getResponse.getStatus() == 200) {
                ByteString returnedValue = getResponse.getValue();
                LogUtil.log("Response received successfully", "Value", returnedValue);
            } else {
                LogUtil.log("Response without 200 status.", "Response", getResponse);

                throw new Exception("Get request failed.");
            }

        } catch (Exception e) {
            cacheClient.shutdown();
            LogUtil.log("Error in Instance main method.", "Error message", e.getMessage(), "Error", e);
        }

    }

}
