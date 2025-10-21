package server.controllers;

import common.parsing.protos.RequestProtos.Request;
import common.parsing.protos.ResponseProtos.Response;
import server.controllers.helpers.ResponseBuilders;

public class CloseController {

    private static Response CloseAcknowledgeResponse = Response.newBuilder()
            .setStatus(200)
            .setMessage("Closed connection successfully.")
            .build();

            
    public static Response close(Request req) {

        Response res = null;


        if (!AuthController.authenticateToken(req.getToken())) {
            res = CloseAcknowledgeResponse;

        } else {
            res = ResponseBuilders.InvalidTokenResponse;
        }

        return res;

    }

}
