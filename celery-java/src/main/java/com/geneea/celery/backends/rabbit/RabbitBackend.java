package com.geneea.celery.backends.rabbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.geneea.celery.backends.TaskResult;
import com.geneea.celery.spi.Backend;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 *     Backend, in Celery terminology, is a way to deliver task results back to the client.
 * </p>
 * <p>
 *     This one sends the tasks to RabbitMQ routing key specified by the reply-to property. The client should register
 *     a temporary queue with its UUID so the overhead of creating a queue happens once per client.
 * </p>
 */
public class RabbitBackend implements Backend {

    private final Channel channel;
    private final ObjectMapper jsonMapper = new ObjectMapper();

    public RabbitBackend(Channel channel) {
        this.channel = channel;
    }

    @Override
    public ResultsProvider resultsProviderFor(String clientId) throws IOException {
        channel.queueDeclare(clientId, false, false, true,
                ImmutableMap.of("x-expires", 24 * 3600 * 1000));
        RabbitResultConsumer consumer = new RabbitResultConsumer(channel);
        channel.basicConsume(clientId, consumer);
        return consumer;
    }

    public void reportResult(String taskId, String queue, String correlationId, Object result)
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

        channel.basicPublish("", queue, properties, jsonMapper.writeValueAsBytes(res));
    }

    public void reportException(String taskId, String replyTo, String correlationId, Throwable e) throws IOException {
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .correlationId(correlationId)
                .priority(0)
                .deliveryMode(1)
                .contentType("application/json")
                .contentEncoding("utf-8")
                .build();

        Map<String, String> excInfo = new HashMap<>();
        excInfo.put("exc_type", e.getClass().getSimpleName());
        excInfo.put("exc_message", e.getMessage());

        TaskResult res = new TaskResult();
        res.result = excInfo;
        res.taskId = taskId;
        res.status = TaskResult.Status.FAILURE;

        channel.basicPublish("", replyTo, properties, jsonMapper.writeValueAsBytes(res));
    }

    public void close() throws IOException {
        channel.abort();
    }
}
