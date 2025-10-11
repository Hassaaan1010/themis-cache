package client.parsing;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import models.ResponseData;
import common.CommonConstants;
import common.LogUtil;
public class ResponseDataDecoder extends ReplayingDecoder<ResponseData>  {
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

            LogUtil.log(null,"Response Decoded", data);

        } catch (Exception e) {
            LogUtil.log("Error In ResponseDataDecoder:","ResponseData",data, "Error", e);
            // throw new UnsupportedOperationException("Unimplemented method 'decode'");
        }
    }

}
   