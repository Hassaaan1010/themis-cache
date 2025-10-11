package models;

public class RequestData {
    private int length;
    private String message;
    private String action;


    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String token) {
        this.message = token;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}



