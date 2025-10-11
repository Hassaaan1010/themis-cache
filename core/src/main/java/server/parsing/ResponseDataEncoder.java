package server.parsing;

import java.nio.charset.Charset;

import common.Constants;
import common.LogUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToByteEncoder;
import models.ResponseData;

public class ResponseDataEncoder extends MessageToByteEncoder<ResponseData>{
	
	private Charset charset = Constants.DEFAULT_CHARSET;

	@Override
	protected void encode(ChannelHandlerContext ctx, ResponseData msg, ByteBuf out) throws EncoderException {
		try {
			out.writeInt(msg.getStatus());
			out.writeInt(msg.getLength());
			out.writeCharSequence(msg.getData(), charset);
			LogUtil.log("Response:", 
				"msg" , msg,
						"out" , out
			);

		} catch (Exception e) {
			LogUtil.log("Error occured while encoding Response Data object:", 
				"msg" , msg,
						"out" , out
			);
			throw new EncoderException();
		}
    }   
}