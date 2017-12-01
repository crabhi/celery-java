package org.sedlakovi.examples

import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import org.junit.Rule
import org.sedlakovi.celery.Celery
import org.sedlakovi.celery.backends.rabbit.RabbitBackend
import org.sedlakovi.celery.brokers.rabbit.RabbitBroker
import org.sedlakovi.celery.examples.TestTaskProxy
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.Wait
import spock.lang.Specification

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BasicTasksTest extends Specification {
    @Rule
    GenericContainer rabbit = new GenericContainer("rabbitmq:3-management").withExposedPorts(5671).waitingFor(Wait.forListeningPort())

    Celery client

    def setup() {
        ConnectionFactory factory = new ConnectionFactory()
        factory.setHost(rabbit.getContainerIpAddress())
        ExecutorService executor = Executors.newCachedThreadPool()
        Connection connection = factory.newConnection(executor)

        client = new Celery(new RabbitBroker(connection.createChannel()),
                new RabbitBackend(connection.createChannel()))
    }

    def "We should get the result computed by a basic task"() {
        def result
        when:
        result = TestTaskProxy.with(client).sum(1, 2)
        then:
        result.get() == 3
    }
}
