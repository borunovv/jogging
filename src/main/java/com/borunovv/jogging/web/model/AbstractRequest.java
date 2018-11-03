package com.borunovv.jogging.web.model;

public abstract class AbstractRequest {
    private String session;

    public AbstractRequest() {
    }

    public AbstractRequest(String session) {
        this.session = session;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    @Override
    public String toString() {
        return "AbstractRequest{" +
                "session='" + session + '\'' +
                '}';
    }
}
