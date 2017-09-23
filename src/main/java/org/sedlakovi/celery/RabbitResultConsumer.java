package org.sedlakovi.celery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.SettableFuture;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class RabbitResultConsumer extends DefaultConsumer {

    private final Cache<String, SettableFuture<Object>> tasks = CacheBuilder.newBuilder().build();
    private final ObjectMapper jsonMapper = new ObjectMapper();

    public RabbitResultConsumer(Channel channel) {
        super(channel);
    }

    public Future<?> getResult(String taskId) {
        return getFuture(taskId);
    }

    private SettableFuture<Object> getFuture(String taskId) {
        try {
            return tasks.get(taskId, SettableFuture::create);
        } catch (ExecutionException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
            throws IOException {

        TaskResult payload = jsonMapper.readValue(body, TaskResult.class);

        SettableFuture<Object> future = getFuture(payload.taskId);
        boolean setAccepted;
        if (payload.status == TaskResult.Status.SUCCESS) {
            setAccepted = future.set(payload.result);
        } else {
            @SuppressWarnings("unchecked")
            Map<String, String> exc = (Map<String, String>) payload.result;
            setAccepted = future.setException(
                    new WorkerException(exc.get("exc_type"), exc.get("exc_message")));
        }
        assert setAccepted;
    }
}
