package server.parsing;

import java.nio.charset.Charset;
import java.util.List;

import common.CommonConstants;
import common.LogUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.ReplayingDecoder;
import models.RequestData;

/*ReplayingDecoder allows waiting for byteBuf to get enough frames to read string of specified length */
public class RequestDataDecoder extends ReplayingDecoder<RequestData> {

    private final Charset charset = CommonConstants.DEFAULT_CHARSET;

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
