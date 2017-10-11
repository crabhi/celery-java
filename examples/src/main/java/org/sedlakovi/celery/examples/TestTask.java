package org.sedlakovi.celery.examples;

import org.sedlakovi.celery.Task;

@Task
public class TestTask {

    public int sum(int x, int y) {
        return x + y;
    }
}
