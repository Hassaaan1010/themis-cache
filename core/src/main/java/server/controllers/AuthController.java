package server.controllers;

import java.util.HashSet;
import java.util.UUID;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.model.Filters;

import common.parsing.protos.RequestProtos.Request;
import common.parsing.protos.ResponseProtos.Response;
import db.MongoService;

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
        }

        Response res = Response.newBuilder()
                .setStatus(status)
                .setMessage(message)
                .setLength(message.length())
                .build();

        return res;
    }

    public static boolean authenticateToken(String token) {
        return validTokens.contains(token);
    }
}
