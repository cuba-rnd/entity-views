package com.company.playground.views.scan.exception;

/**
 * Created by Aleksey Stukalov on 16/08/2018.
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
