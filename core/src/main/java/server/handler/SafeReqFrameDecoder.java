package server.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import server.serverUtils.EchoException;

import java.util.List;

import common.CommonConstants;

public class SafeReqFrameDecoder extends ProtobufVarint32FrameDecoder {
    private final int maxFrameSize;
    private final int minFrameSize;

    public SafeReqFrameDecoder() {
        super();
        this.maxFrameSize = CommonConstants.MAX_REQ_SIZE;
        this.minFrameSize = CommonConstants.MIN_REQ_SIZE;
    }

    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        in.markReaderIndex();
        if (!in.isReadable())
            return;

        // Defensive check: if client lied about length (partial or incomplete frame)
        if (in.readableBytes() < minFrameSize) {
            in.resetReaderIndex();
            return; // wait for more bytes
        }

        // Peek varint32 length safely without advancing readerIndex until we know we
        // can handle it
        // we read 1 MSB and if it is 1, there are more bytes to come, else that was
        // last byte of varInt32
        int frameLength = 0;
        int shift = 0;
        while (true) {
            if (!in.isReadable()) {
                in.resetReaderIndex();
                return;
            }
            byte b = in.readByte();
            frameLength |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0)
                break;
            shift += 7;
            // varInt is 1-5 bytes. if > 5*7 : invalid.
            // if (shift > 35) {
            // in.clear();
            // throw new EchoException(400, "Malformed varint32 frame length. Length read: "
            // + frameLength);
            // }
            if (shift > 35) {
                in.skipBytes(in.readableBytes());
                throw new EchoException(400, "Malformed varint32 frame length");
            }

        }

        System.out.println("Frame size of Req: " + frameLength);

        // Defensive check: if declared size > max
        if (in.readableBytes() > maxFrameSize) {
            in.clear();
            throw new EchoException(400, "The payload send was too large and could not be processed.");
        }

        // Now slice the frame and pass it downstream
        ByteBuf frame = in.readRetainedSlice(frameLength);
        out.add(frame);
    }
}
