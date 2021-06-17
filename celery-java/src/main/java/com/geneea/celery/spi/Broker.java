package com.geneea.celery.spi;

import java.io.IOException;

/**
 * <i>Internal, used by {@link com.geneea.celery.Celery} and implemented by broker providers.</i>
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
     * @param name queue name
     * @param maxPriority the max priority of the queue with priority
     * @throws IOException
     */
    void declareQueue(String name, int maxPriority) throws IOException;

    /**
     * @return message that can be constructed and later sent
     */
    Message newMessage();

    /**
     * @param priority the priority of the message that is executed
     * @return message that can be constructed and later sent
     */
    Message newMessage(int priority);
}
