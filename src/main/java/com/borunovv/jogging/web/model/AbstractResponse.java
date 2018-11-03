package com.borunovv.jogging.web.model;

public abstract class AbstractResponse {
    private String status;

    public AbstractResponse(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
