package com.geneea.celery;

/**
 * An exception produced by the remote worker.
 */
public class WorkerException extends Exception {
    public WorkerException(String type, String message) {
        super(String.format("%s(%s)", type, message));
    }
}
