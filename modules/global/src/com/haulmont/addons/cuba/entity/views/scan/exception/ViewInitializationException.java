package com.haulmont.addons.cuba.entity.views.scan.exception;

/**
 * Generic exception that is used to indicate issues in entity views initialization.
 */
public class ViewInitializationException extends RuntimeException {

    public ViewInitializationException() {
    }

    public ViewInitializationException(String message) {
        super(message);
    }

    public ViewInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ViewInitializationException(Throwable cause) {
        super(cause);
    }

    public ViewInitializationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
