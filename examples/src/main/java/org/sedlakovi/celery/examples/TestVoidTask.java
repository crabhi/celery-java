package org.sedlakovi.celery.examples;

import org.kohsuke.MetaInfServices;
import org.sedlakovi.celery.Task;

@MetaInfServices
public class TestVoidTask implements Task {

    public void run(int x, int y) {
        System.out.println(x + y);
    }
}
