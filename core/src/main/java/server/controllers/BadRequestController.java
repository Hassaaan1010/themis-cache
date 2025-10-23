package server.controllers;

import common.parsing.protos.ResponseProtos.Response;;

public class BadRequestController {

    public static Response invalidRequestMethod(int requestId) {

        int status = 400;
        String message = "Requested method was invalid.";

        Response res = Response.newBuilder()
                .setStatus(status)
                .setMessage(message)
                .setResponseId(requestId)
                .build();
        return res;
    }
}
