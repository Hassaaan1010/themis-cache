package client.handler;

import java.util.List;

import common.CommonConstants;
import common.LogUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class SafeResFrameDecoder extends ByteToMessageDecoder {
    // private final int minFrameSize;
    private final int maxFrameSize;

    public SafeResFrameDecoder() {
        // this.minFrameSize = CommonConstants.MIN_RES_SIZE;
        this.maxFrameSize = CommonConstants.MAX_RES_SIZE;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // Mark reader index BEFORE reading varint
        in.markReaderIndex();

        // Read the varint32 length prefix
        int frameLength = 0;
        int shift = 0;
        byte tmp;

        while (true) {
            if (!in.isReadable()) {
                in.resetReaderIndex();
                return; // Need more data
            }

            tmp = in.readByte();
            frameLength |= (tmp & 0x7F) << shift;

            if ((tmp & 0x80) == 0) {
                break; // Last byte of varint
            }

            shift += 7;
            if (shift > 35) {
                in.clear();
                LogUtil.log("Malformed varint32. Closing connection");
                ctx.close();
                return;
            }
        }

        // Validate frame length (optional)
        if (frameLength > maxFrameSize) {
            in.clear();
            LogUtil.log("Response too large. Closing connection.");
            ctx.close();
            return;
        }

        System.out.println("Frame size of Res: " + frameLength);

        // Check if we have enough bytes for the full frame
        if (in.readableBytes() < frameLength) {
            in.resetReaderIndex(); // Reset to BEFORE varint
            return;
        }

        // Extract frame (WITHOUT length prefix)
        ByteBuf frame = in.readRetainedSlice(frameLength);
        out.add(frame);
    }
}
