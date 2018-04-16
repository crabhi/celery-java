package com.geneea.celery

import org.kohsuke.MetaInfServices
import com.geneea.celery.spi.Broker
import com.geneea.celery.spi.BrokerFactory
import com.geneea.celery.spi.Message

import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeoutException


public class MockBrokerFactory implements BrokerFactory {
    static List<String> queuesDeclared = []

    /**
     * Workaround for the fact that Spock mocks can be created only from the Specification class.
     */
    static List<Message> messages
    static messageNum = 0

    @Override
    Set<String> getProtocols() {
        return ["mock"]
    }

    @Override
    Broker createBroker(URI uri, ExecutorService executor) throws IOException, TimeoutException {
        return new Broker() {
            @Override
            void declareQueue(String name) throws IOException {
                queuesDeclared.add(name)
            }

            /**
             * @param name queue name
             * @param maxPriority the max priority of the queue with priority
             * @throws IOException
             */
            @Override
            void declareQueue(String name, int maxPriority) throws IOException {

            }

            @Override
            Message newMessage() {
                def message = messages[messageNum % messages.size()]
                messageNum++
                return message
            }

            /**
             * @param priority the priority of the message that is executed
             * @return message that can be constructed and later sent
             */
            @Override
            Message newMessage(int priority) {
                return null
            }
        }
    }
}
