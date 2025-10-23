package server.controllers.helpers;

import common.parsing.protos.ResponseProtos.Response;

import com.google.protobuf.ByteString;

import common.parsing.protos.ResponseProtos;

public class ResponseBuilders {

    public static Response makeInvalidTokenResponse(String token, int requestId) {

        Response res = Response.newBuilder()
                .setStatus(401)
                .setMessage("Invalid token.")
                .setResponseId(requestId)
                .build();

        return res;

    }

    public static ResponseProtos.Response makeResponse(int status, String message, ByteString value, int requestId) {

        ResponseProtos.Response res = ResponseProtos.Response.newBuilder()
                .setStatus(status)
                .setMessage(message)
                .setValue(value)
                .setResponseId(requestId)
                .build();

        return res;
    }

    public static ResponseProtos.Response makeErrorResponse(int status, String message, int requestId) {

        ResponseProtos.Response res = ResponseProtos.Response.newBuilder()
                .setStatus(status)
                .setMessage(message)
                .setResponseId(requestId)
                .build();

        return res;
    }

    public static Response makeAuthResponse(int status, String message, int requestId) {

        Response res = Response.newBuilder()
                .setStatus(status)
                .setMessage(message)
                .setResponseId(requestId)
                .build();

        return res;

    }

    public static Response makeGetResponse(int status, String message, ByteString value, int requestId) {

        Response res = Response.newBuilder()
                .setStatus(status)
                .setMessage(message)
                .setValue(value)
                .setResponseId(requestId)
                .build();

        return res;
    }

    public static Response makeSetResponse(int status, String message, int requestId) {

        Response res = Response.newBuilder()
                .setStatus(status)
                .setMessage(message)
                .setResponseId(requestId)
                .build();

        return res;
    }

    public static Response makeDelResponse(int status, String message, int requestId) {

        Response res = Response.newBuilder()
                .setStatus(status)
                .setMessage(message)
                .setResponseId(requestId)
                .build();

        return res;
    }

    public static Response keyNotFoundResponse(int status, String message, int requestId) {
        Response res = Response.newBuilder()
                .setStatus(status)
                .setMessage(message)
                .setResponseId(requestId)
                .build();

        return res;
    }

}
