package com.borunovv.jogging.web.model;

public class SuccessResponse extends AbstractResponse {
    public static SuccessResponse INSTANCE = new SuccessResponse();

    public SuccessResponse() {
         super("success");
    }

    @Override
    public String toString() {
        return "SuccessResponse{} " + super.toString();
    }
}
