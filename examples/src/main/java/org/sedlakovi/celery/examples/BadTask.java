package org.sedlakovi.celery.examples;

import org.sedlakovi.celery.CeleryTask;

/**
 * Task that always throws exception.
 */
@CeleryTask
public class BadTask {

    public void throwCheckedException() throws Exception {
        throw new Exception();
    }

    public void throwUncheckedException() {
        throw new RuntimeException();
    }
}
