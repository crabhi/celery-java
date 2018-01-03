package org.sedlakovi.celery.backends.rabbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.sedlakovi.celery.backends.TaskResult;
import org.sedlakovi.celery.WorkerException;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

class RabbitResultConsumer extends DefaultConsumer implements RabbitBackend.ResultsProvider {

    private final LoadingCache<String, SettableFuture<Object>> tasks =
            CacheBuilder
                    .newBuilder()
                    .expireAfterWrite(2, TimeUnit.HOURS)
                    .build(new CacheLoader<String, SettableFuture<Object>>() {
                @Override
                public SettableFuture<Object> load(@Nonnull String key) {
                    return SettableFuture.create();
                }
            });
    private final ObjectMapper jsonMapper = new ObjectMapper();

    RabbitResultConsumer(Channel channel) {
        super(channel);
    }

    @Override
    public ListenableFuture<Object> getResult(String taskId) {
        return tasks.getUnchecked(taskId);
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
            throws IOException {

        TaskResult payload = jsonMapper.readValue(body, TaskResult.class);

        SettableFuture<Object> future = tasks.getUnchecked(payload.taskId);
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
