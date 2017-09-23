package org.sedlakovi.celery.examples;

import org.kohsuke.MetaInfServices;
import org.sedlakovi.celery.Task;

@MetaInfServices
public class TestTask implements Task {

    public int run(int x, int y) {
        return x + y;
    }
}
