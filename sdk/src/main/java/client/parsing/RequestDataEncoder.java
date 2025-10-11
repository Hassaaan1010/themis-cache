package client.parsing;
// import RequestData

import java.nio.charset.Charset;

import common.CommonConstants;
import common.LogUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import models.RequestData;


public class RequestDataEncoder extends MessageToByteEncoder<RequestData> {

    private Charset charset = CommonConstants.DEFAULT_CHARSET;

    @Override
    protected void encode(ChannelHandlerContext ctx, RequestData msg, ByteBuf out) throws Exception {
        try {
            
            out.writeInt(msg.getLength());
            out.writeCharSequence(msg.getAction(), charset);
            out.writeCharSequence(msg.getMessage(), charset);
            LogUtil.log("CLIENT: log from ReqDataEncoder", "ReqData",msg, "Output",out);

        } catch (Exception e) {
            LogUtil.log("Error in Client log from ReqDataEncoder", "ReqData",msg, "Output",out);
            throw new UnsupportedOperationException("Unimplemented method 'encode'");
        }
    }
    
}

