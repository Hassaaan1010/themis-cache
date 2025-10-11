package models;

public class ResponseData {
    private int status;
    private int length;
    private String data;

    public int getStatus() {
        return status;
    }

    public int getLength() {
        return length;
    }

    public String getData() {
        return data;
    }

    public void setStatus(int value) {
        this.status = value;
    } 
    
    public void setLength(int length) {
        this.length = length;
    }

    public void setData(String data) {
        this.data = data;
    }


}