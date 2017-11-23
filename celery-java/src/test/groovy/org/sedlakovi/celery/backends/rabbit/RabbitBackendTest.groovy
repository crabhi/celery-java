package org.sedlakovi.celery.backends.rabbit

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import spock.genesis.Gen
import spock.lang.Specification


class RabbitBackendTest extends Specification {

    def "Backend should use the RabbitResultConsumer"() {
        def Channel channel = Mock(Channel.class)
        def backend = new RabbitBackend(channel)
        def resultsProvider
        def consumerArg

        when:
        resultsProvider = backend.resultsProviderFor(clientId)

        then:
        1 * channel.queueDeclare(clientId, false, false, true, ["x-expires": 24 * 3600 * 1000])
        1 * channel.basicConsume(clientId, { consumerArg = it })
        resultsProvider == consumerArg

        where:
        clientId << Gen.string(50).take(8)
    }
}

class RabbitResultConsumerTest extends Specification {

    def "Consumer should report result of a task"() {
        def Channel channel = Mock(Channel.class)
        def consumer = new RabbitResultConsumer(channel)
        def result = consumer.getResult(taskId)

        when:
        consumer.handleDelivery(null, null, null, body.bytes)

        then:
        result.isDone() == done
        !result.isDone() || result.get() == expectedResult

        where:
        body                                                                                        | expectedResult | taskId      | done
        '{"children":[], "status": "SUCCESS", "result": 1, "traceback": null, "task_id": "xac"}'    | 1              | "xac"       | true
        '{"children":[], "status": "SUCCESS", "result": "x", "traceback": null, "task_id": "1aa"}'  | "x"            | "1aa"       | true
        '{"children":[], "status": "SUCCESS", "result": "x", "traceback": null, "task_id": "1ac"}'  | null           | "1aa"       | false
        '{"children":[], "status": "SUCCESS", "result": 1, "traceback": null, "task_id": "1ac"}'    | null           | "1aa"       | false
    }
}
