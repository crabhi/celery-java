package com.geneea.celery.examples;

import com.geneea.celery.CeleryTask;

@CeleryTask
public class TestTask {

    public int sum(int x, int y) {
        return x + y;
    }
}
