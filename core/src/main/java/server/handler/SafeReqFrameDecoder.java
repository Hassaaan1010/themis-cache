package server.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
// import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import server.serverUtils.EchoException;

import java.util.List;

import common.CommonConstants;
import common.LogUtil;

public class SafeReqFrameDecoder extends ByteToMessageDecoder {
    private final int maxFrameSize;
    private final int minFrameSize;

    public SafeReqFrameDecoder() {
        this.maxFrameSize = CommonConstants.MAX_REQ_SIZE;
        this.minFrameSize = CommonConstants.MIN_REQ_SIZE;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        LogUtil.log("Did we reach the frame length prepend safe decoder?");
        
        // Mark reader index so we can reset if needed
        in.markReaderIndex();
        
        // Read the varint32 length prefix
        // int preIndex = in.readerIndex();
        int length = 0;
        int shift = 0;
        byte tmp;
        
        while (true) {
            if (!in.isReadable()) {
                in.resetReaderIndex();
                return; // Need more data
            }
            
            tmp = in.readByte();
            length |= (tmp & 0x7F) << shift;
            
            if ((tmp & 0x80) == 0) {
                break; // Last byte of varint marked by 0
            }
            
            shift += 7;
            if (shift > 35) {
                // Malformed varint - too many bytes
                in.clear();
                // Likely tampering with SDK internals. Close connection. 
                LogUtil.log("Malformed varint32 frame length. Closing connection");
                ctx.close();
                return;
            }
        }
        
        // Validate length      
        if (length < minFrameSize) {
            in.clear();
            throw new EchoException(400, "Request object too small. Possibly malformed. Length :" + length);
        }
        
        if (length > maxFrameSize) {
            in.clear();
            // Likely tampering with SDK internals. Close connection. 
                LogUtil.log("Request object was too large. Length: " + length + " Closing connection.");
                ctx.close();
                return;
        }
        
        System.out.println("Frame size of Req: " + length);
        
        // Check if we have enough bytes for the full frame
        if (in.readableBytes() < length) {
            // Not enough data yet, reset and wait
            in.resetReaderIndex();
            return;
        }
        
        // Success - extract the frame (WITHOUT the length prefix)
        ByteBuf frame = in.readRetainedSlice(length);
        out.add(frame);
    }
}
