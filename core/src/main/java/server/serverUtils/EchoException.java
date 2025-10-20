package server.serverUtils;

public class EchoException extends RuntimeException {
    private final int status;

    /**
     * Given a status and message, it will build and return a ResponseProto.Response response frame back to client.
     */
    public EchoException(int status, String message){
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
