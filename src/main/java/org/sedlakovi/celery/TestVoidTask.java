package org.sedlakovi.celery;

import org.kohsuke.MetaInfServices;

@MetaInfServices
public class TestVoidTask implements Task {

    public void run(int x, int y) {
        System.out.println(x + y);
    }
}
