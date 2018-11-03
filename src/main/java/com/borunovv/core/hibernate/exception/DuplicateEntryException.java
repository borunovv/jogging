package com.borunovv.core.hibernate.exception;

import org.springframework.dao.DataAccessException;

public class DuplicateEntryException extends DataAccessException {

    public DuplicateEntryException(String message) {
        super(message);
    }

    public DuplicateEntryException(String message, Throwable cause) {
        super(message, cause);
    }
}