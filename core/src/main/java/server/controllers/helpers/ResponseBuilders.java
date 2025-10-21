package server.controllers.helpers;

import common.parsing.protos.ResponseProtos.Response;

public class ResponseBuilders {

    public static Response InvalidTokenResponse = Response.newBuilder()
            .setStatus(401)
            .setMessage("Invalid token.")
            .build();
    
    
            
}
