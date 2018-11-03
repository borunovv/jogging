package com.borunovv.core.hibernate.exception;

import org.springframework.dao.DataAccessException;

public class DataNotFoundException extends DataAccessException {

    public DataNotFoundException(String message) {
        super(message);
    }

    public DataNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}