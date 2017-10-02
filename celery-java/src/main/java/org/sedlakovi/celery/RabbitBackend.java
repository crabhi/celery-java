package org.sedlakovi.celery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;

import java.io.IOException;

public class RabbitBackend {
    private final Channel channel;
    private final ObjectMapper jsonMapper;

    public RabbitBackend(Channel channel) {
        this.channel = channel;
        jsonMapper = new ObjectMapper();
    }

    void reportResult(String taskId, String replyTo, String correlationId, Object result)
            throws IOException {

        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .correlationId(correlationId)
                .priority(0)
                .deliveryMode(1)
                .contentType("application/json")
                .contentEncoding("utf-8")
                .build();

        TaskResult res = new TaskResult();
        res.result = result;
        res.taskId = taskId;
        res.status = TaskResult.Status.SUCCESS;

        channel.basicPublish("", replyTo, properties, jsonMapper.writeValueAsBytes(res));
    }

    void reportException(String taskId, String replyTo, String correlationId, Throwable e) throws IOException {
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .correlationId(correlationId)
                .priority(0)
                .deliveryMode(1)
                .contentType("application/json")
                .contentEncoding("utf-8")
                .build();

        TaskResult res = new TaskResult();
        res.result = ImmutableMap.of(
                "exc_type", e.getClass().getSimpleName(),
                "exc_message", e.getMessage());
        res.taskId = taskId;
        res.status = TaskResult.Status.FAILURE;

        channel.basicPublish("", replyTo, properties, jsonMapper.writeValueAsBytes(res));
    }

    void close() throws IOException {
        channel.abort();
    }

    public RabbitResultConsumer createResultConsumer(String clientId) throws IOException {
        channel.queueDeclare(clientId, false, false, true,
                             ImmutableMap.of("x-expires", 24 * 3600 * 1000));
        RabbitResultConsumer consumer = new RabbitResultConsumer(channel);
        channel.basicConsume(clientId, consumer);
        return consumer;
    }
}
