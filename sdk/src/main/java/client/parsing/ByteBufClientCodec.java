package client.parsing;

import java.nio.charset.Charset;
import java.util.List;

import common.CommonConstants;
import common.LogUtil;
import common.interfaces.Codec;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.ReplayingDecoder;
import models.RequestData;
import models.ResponseData;

public class ByteBufClientCodec implements Codec<RequestData, ResponseData> {

    private static Charset charset = CommonConstants.DEFAULT_CHARSET;

    @Override
    public ChannelInboundHandler newDecoder() {
        return new ResponseDataDecoder();
    }

    @Override
    public ChannelOutboundHandler newEncoder() {
        return new RequestDataEncoder();
    }

    private class RequestDataEncoder extends MessageToByteEncoder<RequestData> {
        @Override
        protected void encode(ChannelHandlerContext ctx, RequestData msg, ByteBuf out) throws Exception {
            try {

                out.writeInt(msg.getLength());
                out.writeCharSequence(msg.getAction(), charset);
                out.writeCharSequence(msg.getMessage(), charset);
                LogUtil.log("CLIENT: log from ReqDataEncoder", "ReqData", msg, "Output", out);

            } catch (Exception e) {
                LogUtil.log("Error in Client log from ReqDataEncoder", "RequestData", msg, "Output", out, "Error", e);
                // throw new UnsupportedOperationException("Unimplemented method 'encode'");
            }
        }
    }

    public class ResponseDataDecoder extends ReplayingDecoder<ResponseData> {
        private ResponseData data;

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            try {
                System.out.println("Reached Response Decoder");
                data = new ResponseData();

                data.setStatus(in.readInt());
                data.setLength(in.readInt());
                data.setData(in.readCharSequence(data.getLength(), CommonConstants.DEFAULT_CHARSET).toString());

                out.add(data);

                LogUtil.log(null, "Response Decoded", data);

            } catch (Exception e) {
                LogUtil.log("Error In ResponseDataDecoder:", "ResponseData", data, "Error", e);
                // throw new UnsupportedOperationException("Unimplemented method 'decode'");
            }
        }

    }

}
