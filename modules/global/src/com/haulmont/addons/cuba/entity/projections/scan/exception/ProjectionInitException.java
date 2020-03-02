package com.haulmont.addons.cuba.entity.projections.scan.exception;

/**
 * Generic exception that is used to indicate issues in entity projections initialization.
 */
public class ProjectionInitException extends RuntimeException {

    public ProjectionInitException() {
    }

    public ProjectionInitException(String message) {
        super(message);
    }

    public ProjectionInitException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProjectionInitException(Throwable cause) {
        super(cause);
    }

    public ProjectionInitException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
