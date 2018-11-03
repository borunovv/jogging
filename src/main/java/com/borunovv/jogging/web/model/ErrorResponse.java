package com.borunovv.jogging.web.model;

public class ErrorResponse extends AbstractResponse {
    private String msg;
    private long code;

    public ErrorResponse() {
        super("error");
    }

    public ErrorResponse(String msg, long code) {
        this();
        this.msg = msg;
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    @Override
    public String toString() {
        return "ErrorResponse{" +
                "msg='" + msg + '\'' +
                ", code=" + code +
                "} " + super.toString();
    }
}
