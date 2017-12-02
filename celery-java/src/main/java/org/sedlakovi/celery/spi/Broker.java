package org.sedlakovi.celery.spi;

import java.io.IOException;

/**
 * Created by krab on 22.11.17.
 */
public interface Broker {

    void declareQueue(String name) throws IOException;

    Message newMessage();
}
