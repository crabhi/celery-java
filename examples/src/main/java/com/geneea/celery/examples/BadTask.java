package com.geneea.celery.examples;

import com.geneea.celery.CeleryTask;

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
