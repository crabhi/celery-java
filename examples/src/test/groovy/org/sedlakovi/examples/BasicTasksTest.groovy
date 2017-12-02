package org.sedlakovi.examples

import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import org.junit.Rule
import org.rnorth.ducttape.unreliables.Unreliables
import org.sedlakovi.celery.Celery
import org.sedlakovi.celery.CeleryWorker
import org.sedlakovi.celery.WorkerException
import org.sedlakovi.celery.backends.rabbit.RabbitBackend
import org.sedlakovi.celery.brokers.rabbit.RabbitBroker
import org.sedlakovi.celery.examples.BadTaskProxy
import org.sedlakovi.celery.examples.TestTask
import org.sedlakovi.celery.examples.TestTaskProxy
import org.sedlakovi.celery.examples.TestVoidTaskProxy
import org.testcontainers.containers.GenericContainer
import spock.genesis.Gen
import spock.lang.Specification

import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit

class BasicTasksTest extends Specification {
    static final int RABBIT_PORT = 5672
    static final int RABBIT_MANAGEMENT_PORT = 15672

    String rabbitUrl(GenericContainer rabbit) {
        ("amqp://guest:guest@"
                + rabbit.getContainerIpAddress()
                + ":"
                + rabbit.getMappedPort(RABBIT_PORT)
                + "/%2F")
    }

    class RabbitWaitStrategy extends GenericContainer.AbstractWaitStrategy {

        @Override
        protected void waitUntilReady() {
            def f = new ConnectionFactory()
            f.uri = rabbitUrl(container)
            Unreliables.retryUntilSuccess(startupTimeout.seconds as int, TimeUnit.SECONDS, {
                f.newConnection(ForkJoinPool.commonPool())
            })
        }
    }

    @Rule
    GenericContainer rabbit = new GenericContainer("rabbitmq:3-management")
            .withExposedPorts(RABBIT_PORT)
            .waitingFor(new RabbitWaitStrategy())

    Celery client
    Thread worker

    def setup() {

        ConnectionFactory factory = new ConnectionFactory()
        factory.uri = rabbitUrl(rabbit)
        factory.sharedExecutor = Executors.newCachedThreadPool()

        Connection connection = factory.newConnection()

        client = new Celery(new RabbitBroker(connection.createChannel()),
                new RabbitBackend(connection.createChannel()))

        worker = Thread.start { it ->
            CeleryWorker.main(["--broker", rabbitUrl(rabbit)] as String[])
        }
    }

    def "We should get the result computed by a basic task"() {
        def result
        when:
        result = TestTaskProxy.with(client).sum(a, b)
        then:
        result.get() == new TestTask().sum(a, b)

        where:
        a << Gen.integer(0, (Integer.MAX_VALUE / 2) as int).take(1)
        b << Gen.integer(0, (Integer.MAX_VALUE / 2) as int).take(1)
    }

    def "The future of a void task should be completed eventually"() {
        def result
        when:
        result = TestVoidTaskProxy.with(client).run(-7, 12)
        then:
        result.get() == null
        result.isDone()
    }

    def "The task returning an exception should report it"() {
        WorkerException exception
        when:
        try {
            task(client).get()
        } catch (ExecutionException t) {
            exception = t.cause
        }
        then:
        exception.message == expectedMessage

        where:
        task                                                         | expectedMessage
        {it -> BadTaskProxy.with(it).throwCheckedException() }       | "Exception(null)"
        {it -> BadTaskProxy.with(it).throwUncheckedException() }     | "RuntimeException(null)"
    }
}
