package server.controllers;

import common.parsing.protos.ResponseProtos;

public class BadRequestController {

    public static ResponseProtos.Response invalidRequestClass() {

        int status = 400;
        String message = "Request object is of wrong class.";

        ResponseProtos.Response res = ResponseProtos.Response.newBuilder()
                .setStatus(status)
                .setMessage(message)
                .build();
        return res;
    }

    public static ResponseProtos.Response invalidRequestMethod() {

        int status = 400;
        String message = "Requested method was invalid.";

        ResponseProtos.Response res = ResponseProtos.Response.newBuilder()
                .setStatus(status)
                .setMessage(message)
                .build();
        return res;
    }
}
