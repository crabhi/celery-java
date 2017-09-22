package org.sedlakovi.celery;


class DispatchException extends Exception {
    DispatchException(String msg) {
        super(msg);
    }

    DispatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
