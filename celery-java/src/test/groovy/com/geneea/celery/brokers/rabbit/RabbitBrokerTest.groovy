package com.geneea.celery.brokers.rabbit

import com.rabbitmq.client.BasicProperties
import com.rabbitmq.client.Channel
import com.geneea.celery.spi.Message
import spock.genesis.Gen
import spock.lang.Specification

class RabbitBrokerTest extends Specification {

    def Channel channel
    def RabbitBroker broker
    def Message message

    def setup() {
        channel = Mock(Channel.class)
        broker = new RabbitBroker(channel)
        message = broker.newMessage()
    }

    def "it should publish messages"() {

        when:
        message.setBody(body)
        message.send()

        then:
        1 * channel.basicPublish("", "celery", _, body);

        where:
        body << ["".bytes, (0..255) as byte[]] + Gen.string.take(5).collect {it.bytes}

    }

    def "it should set content encoding"() {

        when:
        message.setContentEncoding(enc)
        message.send()

        then:
        1 * channel.basicPublish("", "celery", { it.contentEncoding == enc }, _);

        where:
        enc << ["utf-8", "utf-16"]
    }

    def "it should set content type"() {

        when:
        message.setContentType(cType)
        message.send()

        then:
        1 * channel.basicPublish("", "celery", { it.contentType == cType }, _);

        where:
        cType << ["application/json", "application/protobuf"]
    }

    def "it should set reply-to"() {
        def BasicProperties props

        when:
        message.headers.replyTo = clientId
        message.headers.id = messageId
        message.send()

        then:
        1 * channel.basicPublish("", "celery", { props = it }, _);
        props.replyTo == clientId

        where:
        messageId << (0..5).collect({UUID.randomUUID().toString()})
        clientId << Gen.these(UUID.randomUUID().toString()).repeat.take(6)
    }

    def "it should set task id as correlation id"() {
        when:
        message.headers.id = messageId
        message.send()

        then:
        1 * channel.basicPublish("", "celery", { it.correlationId == messageId }, _);

        where:
        messageId << (0..5).collect({UUID.randomUUID().toString()})
    }

    def "it should set reasonable defaults"() {
        def BasicProperties props

        when:
        message.send()

        then:
        1 * channel.basicPublish("", "celery", { props = it }, _);
        props.deliveryMode == 2  // peristent
        props.priority == 0
    }

    def "it should set message headers"() {
        def BasicProperties props

        when:
        message.headers.argsRepr = argsRepr
        message.body = ("[[" + args + "]], {}, {}").getBytes("utf-8")
        message.headers.id = id
        message.headers.origin = clientName
        message.send()

        then:
        1 * channel.basicPublish("", "celery", { props = it }, _);
        props.headers["argsrepr"] == argsRepr
        props.headers["timelimit"] == [null, null]
        props.headers["retries"] == 0
        props.headers["parent_id"] == null
        props.headers["root_id"] == id
        props.headers["kwargsrepr"] == "{}"
        props.headers["expires"] == null
        props.headers["eta"] == null
        props.headers["lang"] == "py"  // sic
        props.headers["group"] == null
        props.headers["origin"] == clientName
        props.headers["id"] == id


        where:
        argsRepr           | args            | clientName
        "[]"               | "[]"            | "x@localhost"
        "[None]"           | "[null]"        | "y@123.1.88.14"
        "[Message(1, 2)]"  | "[[1, 2]]"      | "pepa"
        "[Message(1, 2)]"  | "[\"message\"]" | ""

        id << Gen.using {UUID.randomUUID().toString()}.take(4)
        task << Gen.these("X#y", "").then(Gen.string).take(4)
    }

    def "if not asked, no reply-to should be set"() {
        def BasicProperties props

        when:
        message.headers.origin = clientId
        message.headers.id = messageId
        message.send()

        then:
        1 * channel.basicPublish("", "celery", { props = it }, _);
        props.replyTo == null

        where:
        messageId << (0..5).collect({UUID.randomUUID().toString()})
        clientId << Gen.these(UUID.randomUUID().toString()).repeat.take(6)
    }

    def "it should create new message on each invocation"() {
        def messages = []

        when:
        (1..5).each {
            messages << broker.newMessage()
        }
        then:
        (messages.collect{System.identityHashCode(it)} as Set).size() == 5
    }

    def "it should set task name"() {
        def BasicProperties props

        when:
        message.headers.id = messageId
        message.headers.taskName = task
        message.send()

        then:
        1 * channel.basicPublish("", "celery", { props = it }, _);
        props.headers["task"] == task

        where:
        messageId << (0..5).collect({UUID.randomUUID().toString()})
        task << Gen.these("x.y.z.SomeClass#method", "Cls#method", "xyz").then(Gen.string(50)).take(6)
    }
}
