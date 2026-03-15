package server.controllers;

import java.util.HashMap;

import com.google.protobuf.ByteString;

import cache.command.Get;
import common.parsing.protos.RequestProtos.Request;
import io.netty.channel.Channel;

public class GetController {

    public static final HashMap<String, ByteString> Cache = new HashMap<>();

    public static Get get(Channel channel, Request req) {
        return new Get(channel, req.getToken(), req.getRequestId(), req.getKey());
    }

    // static {
    // Cache.put("PING", ByteString.copyFromUtf8("PONG"));
    // }

    // public static Get get(Request req) {

    // res = ResponseBuilders.makeInvalidTokenResponse(req.getRequestId());

    // ByteString val = Cache.get(req.getKey());
    // if (val == null) {
    // res = ResponseBuilders.keyNotFoundResponse(404, "Key not found",
    // req.getRequestId());
    // } else {
    // res = ResponseBuilders.makeGetResponse(200, "Key was found.", val,
    // req.getRequestId());
    // }

    // }
}