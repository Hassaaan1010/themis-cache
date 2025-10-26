package client;

// import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.google.protobuf.ByteString;
import com.google.protobuf.UnsafeByteOperations;

import client.clientUtils.RequestUtils;

// import com.google.protobuf.ByteString;

import client.handler.EchoClientHandler;
import client.handler.ErrorInboundHandler;
import client.handler.SafeResFrameDecoder;
// import client.parsing.ByteBufClientCodec;
import client.parsing.ProtobufClientCodec;
import common.CommonConstants;
import common.LogUtil;
import common.interfaces.Codec;
import common.parsing.protos.RequestProtos;
import common.parsing.protos.ResponseProtos;
import common.parsing.protos.RequestProtos.Request;
import common.parsing.protos.ResponseProtos.Response;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

public class EchoClient {

    private ChannelFuture channelFuture;
    private NioEventLoopGroup workerGroup;
    public static final boolean DEBUG_CLIENT = false;

    private Bootstrap b;

    private Channel channel;

    private String token;

    public EchoClient() {
        try {
            workerGroup = new NioEventLoopGroup();
            // Codec<RequestData, ResponseData> codec = new ByteBufClientCodec();
            Codec<RequestProtos.Request, ResponseProtos.Response> codec = new ProtobufClientCodec();

            EchoClient clientInstance = this;
            b = new Bootstrap();
            b.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    // .
                                    // .addLast(new )
                                    .addLast(new SafeResFrameDecoder())
                                    .addLast(codec.newDecoder())

                                    .addLast(new ProtobufVarint32LengthFieldPrepender())
                                    .addLast(codec.newEncoder())

                                    .addLast(new EchoClientHandler(clientInstance))
                                    .addLast(new ErrorInboundHandler());
                        }
                    });
            if (EchoClient.DEBUG_CLIENT)
                LogUtil.log("Client instantiated.");

        } catch (Exception e) {
            if (EchoClient.DEBUG_CLIENT)
                LogUtil.log("Error initializing EchoClient.", "Error", e);
            System.exit(1);
        }
    }

    public void start() throws Exception {
        try {

            channelFuture = b.connect(CommonConstants.SERVER_HOST, CommonConstants.SERVER_PORT).sync();
            System.out
                    .println("Client connected to " + CommonConstants.SERVER_HOST + ":" + CommonConstants.SERVER_PORT);

            this.channel = channelFuture.channel();

            // Non-blocking: get notified when channel closes
            this.channel.closeFuture().addListener((_ -> {
                if (EchoClient.DEBUG_CLIENT)
                    LogUtil.log("Channel closed, cleaning up...");
                workerGroup.shutdownGracefully();
            }));

        } catch (Exception e) {
            if (EchoClient.DEBUG_CLIENT)
                LogUtil.log("Error in Echo Client : ", "Error", e);
            workerGroup.shutdownGracefully();
        }
    }

    public void shutdown() {
        try {
            if (this.channel != null) {
                this.channel.close().sync();
            }
            System.out.println("Shutting down client gracefully...");
            if (channelFuture != null) {
                channelFuture.channel().close();
            }
            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
            }

        } catch (Exception e) {
            if (EchoClient.DEBUG_CLIENT)
                LogUtil.log("Error while shutting down.", "Error", e);
            System.exit(1);
        }

    }

    /*
     * User runs program with one thread that reaches line to call .GET() the
     * function is added to stack. Then an integer id is created atomically, then we
     * create a CompletableFuture object with Response for <T> meaning that on
     * completion of the promise a Response type will take the place of the
     * placeholder return value. then this future is mapped into a concurrent
     * Hashmap with id. then Request is created and sent off to server. and the
     * reference to the future is returned. if i get this correctly, this returned
     * reference and the map pointer for the id - both point to the same address.
     * when the placeholder future is returned, the thread that had called the
     * .GET() just waits there until the placeholder is changed to the type of value
     * that is actually expected (Response) meanwhile the channel handler is
     * processing incoming data in the channelRead. when the object is received. it
     * calls the completeFuture method to pop that request from that hashmap, this
     * popped object is ofcourse the address to the placeholder where a response is
     * being awaited, so then we can call .complete which writes the Response res to
     * the placeholders address. and then the thread can continue on its path...
     */

    private final ConcurrentHashMap<Integer, CompletableFuture<Response>> pendingRequests = new ConcurrentHashMap<>();
    private final AtomicInteger atomicIdGenerator = new AtomicInteger();

    public void completeFuture(Response res) {
        if (EchoClient.DEBUG_CLIENT)
            LogUtil.log("Completing future.", "ResponseId", res.getResponseId());
        CompletableFuture<Response> future = pendingRequests.remove(res.getResponseId());
        if (future != null) {
            future.complete(res);
        }
    }

    {
        /*
         * Successful get returns 200
         */
        // public Response getKey(String key, boolean bigPayload, boolean largePayload)
        // throws Exception {
        // Integer requestId = atomicIdGenerator.getAndIncrement();
        // if (EchoClient.DEBUG_CLIENT)
        // LogUtil.log("Request GET generated.", "Id", requestId);

        // CompletableFuture<Response> future = new CompletableFuture<>();

        // Request req = RequestUtils.makeGetRequest(this.token, key, 0, requestId);
        // Response getResponse;
        // pendingRequests.put(requestId, future);

        // int timeoutCoefficient = 1;
        // if (bigPayload && !largePayload) {
        // timeoutCoefficient = 10;
        // } else if (largePayload && !bigPayload) {
        // timeoutCoefficient = 100;
        // }
        // try {
        // if (EchoClient.DEBUG_CLIENT)
        // LogUtil.log("log timeout", "Timeout duration: ",
        // CommonConstants.GET_REQUEST_TIMEOUT);

        // CompletableFuture<Response> futureWithTimeout = future.completeOnTimeout(
        // RequestUtils.makeUpErrorResponse(504, "Request:" + requestId + "timed out.",
        // requestId),
        // CommonConstants.GET_REQUEST_TIMEOUT * timeoutCoefficient,
        // TimeUnit.MILLISECONDS);

        // channel.writeAndFlush(req);

        // getResponse = futureWithTimeout.get();
        // } catch (Exception e) {
        // if (EchoClient.DEBUG_CLIENT)
        // LogUtil.log("Error occured while trying to get key from cache server.\n",
        // "Error", e);
        // getResponse = RequestUtils.makeUpErrorResponse(500, "Failed to get key.",
        // requestId);

        // } finally {
        // pendingRequests.remove(requestId);
        // }

        // return getResponse;
        // }

        /*
         * Successful set returns 201
         */
        // public Response setKey(String key, ByteString value, boolean encrypt, boolean
        // compress) {

        // // Otions handle
        // int options = 0;
        // if (encrypt) {
        // options |= CommonConstants.encryptOption;
        // }
        // if (compress) {
        // options |= CommonConstants.compressOption;
        // }

        // // Request handle
        // Integer requestId = atomicIdGenerator.getAndIncrement();
        // if (EchoClient.DEBUG_CLIENT)
        // LogUtil.log("Request SET generated.", "Id", requestId);

        // CompletableFuture<Response> future = new CompletableFuture<>();

        // pendingRequests.put(requestId, future);

        // int tempReqId = (int) requestId;
        // if (EchoClient.DEBUG_CLIENT)
        // LogUtil.log("SET method creating request.", "Request id used:", requestId,
        // "Request id had it been int:",
        // tempReqId);
        // Request req = RequestUtils.makeSetRequest(token, key, value, options,
        // requestId);
        // Response setResponse;

        // int timeoutCoefficient = Math.floorDiv(req.getValue().size(), 1024); // 5 ms
        // / kb

        // try {
        // if (EchoClient.DEBUG_CLIENT)
        // LogUtil.log("log timeout", "Timeout duration: ",
        // CommonConstants.SET_REQUEST_TIMEOUT);

        // CompletableFuture<Response> futureWithTimeout = future.completeOnTimeout(
        // RequestUtils.makeUpErrorResponse(504, "Request:" + requestId + "timed out.",
        // requestId),
        // CommonConstants.SET_REQUEST_TIMEOUT * timeoutCoefficient,
        // TimeUnit.MILLISECONDS);

        // if (EchoClient.DEBUG_CLIENT)
        // LogUtil.log("SET Request was flushed. ", "Request id", req.getRequestId());
        // channel.writeAndFlush(req);

        // setResponse = futureWithTimeout.get();

        // } catch (Exception e) {
        // if (EchoClient.DEBUG_CLIENT)
        // LogUtil.log("Error occured while trying to set key to cache server.\n",
        // "Error", e);
        // setResponse = RequestUtils.makeUpErrorResponse(500, "Failed to set key.",
        // requestId);
        // } finally {
        // pendingRequests.remove(requestId);
        // }

        // return setResponse;

        // }

        /*
         * Successful delete returns 204
         */
        // public Response deleteKey(String key) {
        // Integer requestId = atomicIdGenerator.getAndIncrement();
        // if (EchoClient.DEBUG_CLIENT)
        // LogUtil.log("Request DEL generated.", "Id", requestId);

        // CompletableFuture<Response> future = new CompletableFuture<>();
        // pendingRequests.put(requestId, future);

        // Request req = RequestUtils.makeDelRequest(token, key, 0, requestId);
        // Response deleteResponse;

        // try {
        // if (EchoClient.DEBUG_CLIENT)
        // LogUtil.log("log timeout", "Timeout duration: ",
        // CommonConstants.DEL_REQUEST_TIMEOUT);

        // CompletableFuture<Response> futureWithTimeout = future.completeOnTimeout(
        // RequestUtils.makeUpErrorResponse(504, "Request:" + requestId + "timed out.",
        // requestId),
        // CommonConstants.DEL_REQUEST_TIMEOUT,
        // TimeUnit.MILLISECONDS);

        // channel.writeAndFlush(req);

        // deleteResponse = futureWithTimeout.get();

        // } catch (Exception e) {
        // if (EchoClient.DEBUG_CLIENT)
        // LogUtil.log("Error occured while trying to set key to cache server.\n",
        // "Error", e);
        // deleteResponse = RequestUtils.makeUpErrorResponse(500, "Failed to delete
        // key.", requestId);
        // } finally {
        // pendingRequests.remove(requestId);
        // }

        // return deleteResponse;
        // }

        /*
         * Successful authentication returns 200
         */
        // public Response authenticate() {
        // Integer requestId = atomicIdGenerator.getAndIncrement();
        // if (EchoClient.DEBUG_CLIENT)
        // LogUtil.log("Request AUTH generated.", "Id", requestId);
        // // Add listner to awaited address
        // CompletableFuture<Response> future = new CompletableFuture<>();

        // // Put req Id mapping to the address.
        // pendingRequests.put(requestId, future);
        // Request req = RequestUtils.makeAuthRequest(requestId);
        // Response authResponse;

        // try {
        // if (EchoClient.DEBUG_CLIENT)
        // LogUtil.log("log timeout", "Timeout duration: ",
        // CommonConstants.AUTH_REQUEST_TIMEOUT);

        // // Link additional listner to address with a default response if address
        // doesn't
        // // receive valid response until timeout.
        // CompletableFuture<Response> futureWithTimeout = future.completeOnTimeout(
        // RequestUtils.makeUpErrorResponse(504, "Request:" + requestId + "timed out.",
        // requestId),
        // CommonConstants.AUTH_REQUEST_TIMEOUT,
        // TimeUnit.MILLISECONDS);

        // channel.writeAndFlush(req);

        // // Get which ever resolved first to the address
        // authResponse = futureWithTimeout.get();

        // if (authResponse.getStatus() == 200) {
        // this.token = authResponse.getMessage();
        // }

        // } catch (Exception e) {
        // // TODO: handle exception
        // if (EchoClient.DEBUG_CLIENT)
        // LogUtil.log("Error occured while trying to authenticate.\n", "Error", e);
        // authResponse = RequestUtils.makeUpErrorResponse(500, "Failed to
        // authenticate.", requestId);
        // } finally {
        // pendingRequests.remove(requestId);
        // }

        // return authResponse;
        // }

    }

    public static ByteString readFiletoByteString(Path filePath) {
        try {
            // Makes too many god damn copies
            // ByteString payloadValue = ByteString.readFrom(
            //         new BufferedInputStream(Files.newInputStream(filePath)));

            // read file → wrap → send → done. No modifications = perfectly safe
            byte[] data = Files.readAllBytes(filePath);
            ByteString payloadValue = UnsafeByteOperations.unsafeWrap(data);
            
            return payloadValue;
        } catch (Exception e) {
            // TODO: handle exception
            if (EchoClient.DEBUG_CLIENT)
                LogUtil.log("Failed to read file into ByteString.", "Error", e);
            throw new Error("File read failed.");
        }
    }

    private Response executeRequest(String operation, long timeoutMs, Function<Integer, Request> requestBuilder) {

        // Thread safe id generation
        Integer requestId = atomicIdGenerator.getAndIncrement();

        if (EchoClient.DEBUG_CLIENT)
            LogUtil.log("Request " + operation + " generated.", "Id", requestId, "Timeout limit", timeoutMs);

        // Add listner to awaited address
        CompletableFuture<Response> future = new CompletableFuture<>();

        // Add future to pending Hashmap
        pendingRequests.put(requestId, future);

        try {
            // Build the specific request using provided lambda
            Request req = requestBuilder.apply(requestId);

            // Setup timeout
            CompletableFuture<Response> futureWithTimeout = future.completeOnTimeout(
                    RequestUtils.makeUpErrorResponse(504,
                            "Request:" + requestId + " timed out.",
                            requestId),
                    timeoutMs,
                    TimeUnit.MILLISECONDS);

            // Send request
            channel.writeAndFlush(req);

            if (EchoClient.DEBUG_CLIENT) {
                LogUtil.log(operation + " request flushed.", "Request id", requestId);
            }

            // Wait for response
            return futureWithTimeout.get();

        } catch (Exception e) {
            if (DEBUG_CLIENT)
                LogUtil.log("Error in " + operation + " request.", "Error", e);

            return RequestUtils.makeUpErrorResponse(500,
                    "Failed to " + operation.toLowerCase() + ".",
                    requestId);
        } finally {
            pendingRequests.remove(requestId);
        }
    }

    /*
     * Successful get returns 200
     */
    public Response getKey(String key, boolean bigPayload, boolean largePayload) {

        final int option = (bigPayload && !largePayload) ? CommonConstants.bigPayloadOption
                : (largePayload && !bigPayload) ? CommonConstants.largePayloadOption
                        : 0;

        final long timeOut = (bigPayload && !largePayload) ? CommonConstants.GET_REQUEST_TIMEOUT * 10
                : (largePayload && !bigPayload) ? CommonConstants.GET_REQUEST_TIMEOUT * 100
                        : CommonConstants.GET_REQUEST_TIMEOUT;

        return executeRequest(
                "GET",
                timeOut,
                reqId -> RequestUtils.makeGetRequest(token, key, option, reqId));
    }

    /*
     * Successful set returns 201
     */
    public Response setKey(String key, ByteString value, boolean encrypt, boolean compress) {

        final int options = (compress ? CommonConstants.compressOption : 0)
                | (encrypt ? CommonConstants.encryptOption : 0)
                | 0;

        // Add 50 for payloads < 1000
        final long timeOut = 50 +  CommonConstants.SET_REQUEST_TIMEOUT * Math.floorDiv(value.size(), 1000); // K ms per kb

        return executeRequest(
                "SET",
                timeOut,
                reqId -> RequestUtils.makeSetRequest(token, key, value, options, reqId));
    }

    /*
     * Successful delete returns 204
     */
    public Response deleteKey(String key) {

        final int options = 0;

        return executeRequest(
                "DEL",
                CommonConstants.DEL_REQUEST_TIMEOUT,
                reqId -> RequestUtils.makeDelRequest(token, key, options, reqId));
    }

    /*
     * Successful authentication returns 200
     */
    public Response authenticate() {

        Response res =  executeRequest(
                "AUTH",
                CommonConstants.AUTH_REQUEST_TIMEOUT,
                reqId -> RequestUtils.makeAuthRequest(reqId));

        if (res.getStatus() == 200 ){
            token = res.getMessage();
        }

        return res;
    }
}
