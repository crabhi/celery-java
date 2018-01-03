package org.sedlakovi.celery.spi;

import java.io.IOException;

/**
 * <i>Internal, used by {@link org.sedlakovi.celery.Celery} and implemented by broker providers.</i>
 *
 * <p>
 *     Broker delivers messages to the workers.
 * </p>
 */
public interface Broker {

    /**
     * The client should declare a queue it intends to use. This is a performance optimization so that the broker
     * doesn't need to check the queue exists every time a message is sent.
     *
     * @param name queue name
     * @throws IOException in case of a connection problem
     */
    void declareQueue(String name) throws IOException;

    /**
     * @return message that can be constructed and later sent
     */
    Message newMessage();
}
