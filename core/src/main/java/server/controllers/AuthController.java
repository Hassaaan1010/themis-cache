package server.controllers;

import java.util.HashSet;
import java.util.UUID;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.model.Filters;

import common.LogUtil;
import common.parsing.protos.RequestProtos.Request;
import common.parsing.protos.ResponseProtos.Response;
import db.MongoService;
import server.EchoServer;
import server.controllers.helpers.ResponseBuilders;

public class AuthController {

    private static HashSet<String> validTokens = new HashSet<String>();

    public static Response authenticate(Request req) {

        String tenantId = req.getKey();
        String password = req.getValue().toStringUtf8();

        String message;
        int status;

        // Find user in DB
        Document tenantDocument = MongoService.UserCollection.find(Filters.eq("_id", new ObjectId(tenantId))).first();

        // Ceck if failed auth against record from db
        if (tenantDocument == null || !tenantDocument.getString("password").equals(password)) {
            message = "Auth Failed. Check password.";
            status = 401;
            if (tenantDocument == null) {
                status = 404;
                message = "Tenant not found";
            }
            
        // Auth success
        } else { 
            message = UUID.randomUUID().toString();
            status = 200;
            
            validTokens.add(message);
            
            if (EchoServer.DEBUG_SERVER) LogUtil.log("Authentication worked. Your token was added", "Token",message, "ValidTokens", validTokens );
        }

        Response res = ResponseBuilders.makeAuthResponse(status, message, req.getRequestId());

        return res;
    }

    public static void removeToken(String token){ 
        validTokens.remove(token);
        return;
    }

    public static boolean authenticateToken(String token) {
        return validTokens.contains(token);
    }
}
