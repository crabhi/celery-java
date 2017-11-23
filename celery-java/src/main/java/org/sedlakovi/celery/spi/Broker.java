package org.sedlakovi.celery.spi;

/**
 * Created by krab on 22.11.17.
 */
public interface Broker {
    Message newMessage();
}
