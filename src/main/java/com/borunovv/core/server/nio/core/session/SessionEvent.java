package com.borunovv.core.server.nio.core.session;

import com.borunovv.core.util.Assert;

public final class SessionEvent {

    public enum Type {CLIENT_CONNECTED, CLIENT_DISCONNECTED}

    private ISession session;
    private Type type;
    private String reason;

    private SessionEvent(Type type, ISession session, String reason) {
        Assert.isTrue(type != null, "type is null");
        Assert.isTrue(session != null, "Session is null");

        this.type = type;
        this.session = session;
        this.reason = reason;
    }

    public static SessionEvent makeClientConnected(ISession session) {
        return new SessionEvent(Type.CLIENT_CONNECTED, session, null);
    }

    public static SessionEvent makeClientDisconnected(ISession session, String reason) {
        return new SessionEvent(Type.CLIENT_DISCONNECTED, session, reason);
    }

    public ISession getSession() {
        return session;
    }

    public Type getType() {
        return type;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "SessionEvent{" +
                "type=" + type +
                ", session=" + session +
                ", reason=" + reason +
                '}';
    }
}
