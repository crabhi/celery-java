package org.sedlakovi.celery.examples;

import org.sedlakovi.celery.CeleryTask;

@CeleryTask
public class TestTask {

    public int sum(int x, int y) {
        return x + y;
    }
}
