package com.borunovv.jogging.users.model;

public enum Rights {
    USER,
    MANAGER,
    ADMIN;

    public static Rights fromString(String str) {
        switch (str.toLowerCase()) {
            case "user":
                return USER;
            case "manager":
                return MANAGER;
            case "admin":
                return ADMIN;
            default:
                throw new IllegalArgumentException("Undefined rights '" + str + "'");
        }
    }

    @Override
    public String toString() {
        switch (this) {
            case USER:
                return "user";
            case MANAGER:
                return "manager";
            case ADMIN:
                return "admin";
            default:
                throw new IllegalArgumentException("Unimplemented rights in toString(): '" + this + "'");
        }
    }

    public static final Rights Self = null;
}
