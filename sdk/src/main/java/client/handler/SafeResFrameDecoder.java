package client.handler;

import java.util.List;

import common.CommonConstants;
import common.parsing.protos.ResponseProtos;
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
        in.markReaderIndex();
        if (!in.isReadable())
            return;

        // Peek varint length safely
        int frameLength = 0;
        int shift = 0;
        boolean incomplete = false;
        in.markReaderIndex();
        while (true) {
            if (!in.isReadable()) {
                incomplete = true;
                break;
            }
            byte b = in.readByte();
            frameLength |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0)
                break;
            shift += 7;
            if (shift > 35) {
                in.clear();
                ResponseProtos.Response res = makeUpErrorResponse("Malformed varint32 frame length");
                out.add(res);
                return;
            }
        }
        if (incomplete) {
            in.resetReaderIndex();
            return;
        }

        if (frameLength > maxFrameSize) {
            in.skipBytes(frameLength);
            ResponseProtos.Response res = makeUpErrorResponse(
                    "Response frame exceeded max lenght. Length: " + frameLength);
            out.add(res);
        }

        System.out.println("Frame size of Res: " + frameLength);

        // Now slice the frame and pass it downstream
        ByteBuf frame = in.readRetainedSlice(frameLength);
        out.add(frame);

    }

    private static ResponseProtos.Response makeUpErrorResponse(String cause) {
        ResponseProtos.Response res = ResponseProtos.Response.newBuilder()
                .setStatus(500)
                .setMessage("Malformed server response. Cause: " + cause)
                .build();
        return res;
    }
}
