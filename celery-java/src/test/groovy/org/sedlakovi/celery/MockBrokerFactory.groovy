package org.sedlakovi.celery

import org.kohsuke.MetaInfServices
import org.sedlakovi.celery.spi.Broker
import org.sedlakovi.celery.spi.BrokerFactory
import org.sedlakovi.celery.spi.Message

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

            @Override
            Message newMessage() {
                def message = messages[messageNum % messages.size()]
                messageNum++
                return message
            }
        }
    }
}
