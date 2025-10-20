package server.serverUtils;

import com.google.protobuf.ByteString;

import common.parsing.protos.ResponseProtos;

public class ResUtils {

    public static ResponseProtos.Response makeResponse(int status, String message, int length, ByteString value) {

        ResponseProtos.Response res = ResponseProtos.Response.newBuilder()
                .setStatus(status)
                .setMessage(message)
                .setLength(length)
                .setValue(value)
                .build();

        return res;
    }

    public static ResponseProtos.Response makeErrorResponse(int status, String message) {

        ResponseProtos.Response res = ResponseProtos.Response.newBuilder()
                .setStatus(status)
                .setMessage(message)
                .build();

        return res;
    }
}
