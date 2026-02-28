package server.controllers;

import java.util.Set;
import java.util.function.BiFunction;

import common.LogUtil;
import common.parsing.protos.RequestProtos.Request;
import common.parsing.protos.ResponseProtos.Response;
import server.EchoServer;
import server.controllers.helpers.ResponseBuilders;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import application.AppContext;

import java.nio.charset.StandardCharsets;

public class AuthController {
    
    private static final HashFunction MURMUR_128 = Hashing.murmur3_128();
    public static final BiFunction<String, String, String> getMurmurHash = (id, password) -> (MURMUR_128
            .newHasher()
            .putString(id, StandardCharsets.UTF_8)
            .putString(password, StandardCharsets.UTF_8)
            .hash()
            .toString());
    
    private final Set<String> validHashes;

    // private final TenantGroup tenantGroup;

    public AuthController(AppContext context) {
        // this.tenantGroup = context.getTenantGroup();
        this.validHashes = context.getTenantGroup().getAuthHashesSet();
    }



    public Response authenticate(Request req) {

        String tenantId = req.getKey();
        String password = req.getValue().toStringUtf8();

        String message;
        int status;

        String hash = getMurmurHash.apply(tenantId, password);

        // Ceck if failed auth against record from db
        if (!validHashes.contains(hash)) {
            // Failed auth
            message = "Auth Failed. Check credentials.";
            status = 401;
        } else {
            // Auth success
            status = 200;
            message = hash;

            if (EchoServer.DEBUG_SERVER)
                LogUtil.log("Token produced:", "messagee", message);
        }

        Response res = ResponseBuilders.makeAuthResponse(status, message, req.getRequestId());

        return res;
    }

    public boolean authenticateToken(String token) {
        return validHashes.contains(token);
    }
}
