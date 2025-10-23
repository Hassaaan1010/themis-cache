package client.clientUtils;

import common.parsing.protos.ResponseProtos.Response;
import commonSDK.EnvConfig;

import com.google.protobuf.ByteString;

import common.parsing.protos.RequestProtos.Action;
import common.parsing.protos.RequestProtos.Request;

public class RequestUtils {

    public static Response makeUpErrorResponse(int status, String message, int requestId) {

        Response res = Response.newBuilder()
                .setStatus(status)
                .setMessage(message)
                .setResponseId(requestId)
                .build();

        return res;
    }

    public static Request makeAuthRequest(int requestId) {

        String tenantId = EnvConfig.TENANT_ID;
        String password = EnvConfig.PASSWORD;

        Request req = Request.newBuilder()
                .setAction(Action.AUTH) // 1 + 1 feild prefix
                .setKey(tenantId) // 24 + 1 + 1 feild prefix
                .setToken("012345678901234567890123456789012345") // 36 size token always expected.
                .setValue(ByteString.copyFromUtf8(password)) // 15 + 1 feild prefix
                .setRequestId(requestId)
                .build();

        return req;
    }

    public static Request makeGetRequest(String token, String key, int options, int requestId) {

        Request req = Request.newBuilder()
                .setAction(Action.GET)
                .setToken(token)
                .setKey(key)
                .setOptions(options)
                .setRequestId(requestId)
                .build();

        return req;
    }

    public static Request makeSetRequest(String token, String key, ByteString value, int options, int requestId) {

        // TODO: Validate request.
        
        // ByteString convertedValue = (ByteString) value;

        Request req = Request.newBuilder()
                .setAction(Action.SET)
                .setToken(token)
                .setKey(key)
                .setValue(value)
                .setRequestId(requestId)
                .build();

        return req;
    }

    public static Request makeDelRequest(String token, String key, int option, int requestId) {
        Request req = Request.newBuilder()
                .setAction(Action.DEL)
                .setToken(token)
                .setKey(key)
                .setOptions(0)
                .setRequestId(requestId)
                .build();

        return req;
    }
}
