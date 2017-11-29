package org.sedlakovi.celery.examples;

import org.sedlakovi.celery.CeleryTask;

@CeleryTask
public class TestVoidTask {

    public void run(int x, int y) {
        System.out.println("I'm the task that just prints: " + (x + y));
    }
}
