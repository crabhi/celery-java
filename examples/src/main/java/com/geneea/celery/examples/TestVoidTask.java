package com.geneea.celery.examples;

import com.geneea.celery.CeleryTask;

@CeleryTask
public class TestVoidTask {

    public void run(int x, int y) {
        System.out.println("I'm the task that just prints: " + (x + y));
    }
}
