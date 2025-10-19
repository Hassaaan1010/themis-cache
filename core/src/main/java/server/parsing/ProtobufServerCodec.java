package server.parsing;

import common.parsing.protos.RequestProtos;
import common.parsing.protos.ResponseProtos;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import common.interfaces.Codec;


public class ProtobufServerCodec implements Codec<RequestProtos.Request, ResponseProtos.Response> {

    @Override
    public ChannelInboundHandler newDecoder() {
        // Handles inbound RequestProtos.Request messages
        return new ProtobufDecoder(RequestProtos.Request.getDefaultInstance());
    }

    @Override
    public ChannelOutboundHandler newEncoder() {
        // Handles outbound ResponseProtos.Response messages
        return new ProtobufEncoder();
    }
}