package client.parsing;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import models.ResponseData;
import common.CommonConstants;
public class ResponseDataDecoder extends ReplayingDecoder<ResponseData>  {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        try {
            
            ResponseData data = new ResponseData();

            data.setStatus(in.readInt());
            data.setLength(in.readInt());
            data.setData(in.readCharSequence(data.getLength(), CommonConstants.DEFAULT_CHARSET).toString());


        } catch (Exception e) {
            
            throw new UnsupportedOperationException("Unimplemented method 'decode'");
        }
    }

}
   