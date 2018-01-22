package com.geneea.celery;

/**
 * An exception that occurred when trying to figure out the method that should process the task in the worker.
 */
public class DispatchException extends Exception {
    DispatchException(String msg) {
        super(msg);
    }

    DispatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
