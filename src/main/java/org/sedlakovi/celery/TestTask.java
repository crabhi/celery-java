package org.sedlakovi.celery;

import org.kohsuke.MetaInfServices;

@MetaInfServices
public class TestTask implements Task {

    public int run(int x, int y) {
        return x + y;
    }
}
