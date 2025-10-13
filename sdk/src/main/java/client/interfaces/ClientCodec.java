package client.interfaces;

import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOutboundHandler;

public interface ClientCodec<Req,Res> {

    /**
     * Returns a new Decoder instance for the pipeline.
     */
    ChannelInboundHandler newDecoder();

    /**
     * Returns a new Encoder instance for the pipeline.
     */
    ChannelOutboundHandler newEncoder();
}
