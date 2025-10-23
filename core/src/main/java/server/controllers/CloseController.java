package server.controllers;

import common.parsing.protos.RequestProtos.Request;
import common.parsing.protos.ResponseProtos.Response;
import server.controllers.helpers.ResponseBuilders;

public class CloseController {

    private static Response makeCloseAcknowledgeResponse(int requestId) {
        Response res = Response.newBuilder()
                .setStatus(200)
                .setMessage("Closed connection successfully.")
                .setResponseId(requestId)
                .build();

        return res;
    }

    public static Response close(Request req) {

        Response res = null;

        if (!AuthController.authenticateToken(req.getToken())) {
            res = makeCloseAcknowledgeResponse(req.getRequestId());

        } else {
            res = ResponseBuilders.makeInvalidTokenResponse(req.getToken(), req.getRequestId());
        }

        return res;
    }

}
