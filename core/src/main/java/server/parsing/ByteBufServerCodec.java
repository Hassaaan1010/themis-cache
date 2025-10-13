package server.parsing;

import java.nio.charset.Charset;
import java.util.List;

import common.CommonConstants;
import common.LogUtil;
import common.interfaces.Codec;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.ReplayingDecoder;
import models.RequestData;

import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToByteEncoder;
import models.ResponseData;

public class ByteBufServerCodec implements Codec<RequestData, ResponseData> {
    private static final Charset charset = CommonConstants.DEFAULT_CHARSET;

    @Override
    public ChannelInboundHandler newDecoder() {
        return new RequestDataDecoder();
    }

    @Override
    public ChannelOutboundHandler newEncoder() {
        return new ResponseDataEncoder();
    }

    private static class RequestDataDecoder extends ReplayingDecoder<RequestData> {

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf input, List<Object> output) throws DecoderException {

            RequestData data = new RequestData();

            try {

                data.setLength(input.readInt());

                data.setAction(input.readCharSequence(CommonConstants.ACTION_LENGTH, charset).toString().toUpperCase());

                CharSequence tempSeq = input.readCharSequence(data.getLength(), charset);
                data.setMessage(tempSeq.toString());
                tempSeq = null;

                output.add(data);

                LogUtil.log("Request data created :", data.toString());

            } catch (Exception e) {
                LogUtil.log("Error occured while decoding exception",
                        "input", input,
                        "data", data.toString(),
                        "Error", e);
            }

        }
    }

    private static class ResponseDataEncoder extends MessageToByteEncoder<ResponseData> {
        @Override
        protected void encode(ChannelHandlerContext ctx, ResponseData msg, ByteBuf out) throws EncoderException {
            try {
                out.writeInt(msg.getStatus());
                out.writeInt(msg.getLength());
                out.writeCharSequence(msg.getData(), charset);
                LogUtil.log("Response:",
                        "msg", msg,
                        "out", out);

            } catch (Exception e) {
                LogUtil.log("Error occured while encoding Response Data object:",
                        "msg", msg,
                        "out", out,
                        "Error", e);
                throw new EncoderException();
            }
        }
    }
}
