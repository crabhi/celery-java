package org.sedlakovi.celery;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Joiner;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.sedlakovi.celery.spi.Backend;
import org.sedlakovi.celery.spi.Broker;
import org.sedlakovi.celery.spi.Message;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * A client allowing you to submit a task and get a {@link Future} describing the result.
 */
public class Client {
    private final String clientId;
    private final String clientName;
    private final ObjectMapper jsonMapper;
    private final Broker broker;
    private final String queue;
    private final Optional<Backend.ResultsProvider> resultsProvider;

    public Client(Broker broker, String queue) throws IOException {
        this(broker, queue, null);
    }

    public Client(Broker broker, Backend backend) throws IOException {
        this(broker, "celery", backend);
    }

    public Client(Broker broker, String queue, Backend backend) throws IOException {
        this.broker = broker;
        this.queue = queue;
        this.clientId = UUID.randomUUID().toString();
        this.clientName = clientId + "@" + InetAddress.getLocalHost().getHostName();
        this.jsonMapper = new ObjectMapper();

        if (backend == null) {
            resultsProvider = Optional.empty();
        } else {
            resultsProvider = Optional.of(backend.resultsProviderFor(clientId));
        }
    }

    public Client(Broker broker) throws IOException {
        this(broker, "celery");
    }

    public AsyncResult submit(Class<?> taskClass, String method, Object[] args) throws IOException {
        return submit(taskClass.getName() + "#" + method, args);
    }

    public AsyncResult submit(String name, Object[] args) throws IOException {
        String taskId = UUID.randomUUID().toString();

        ArrayNode payload = jsonMapper.createArrayNode();
        ArrayNode argsArr = payload.addArray();
        for (Object arg : args) {
            argsArr.addPOJO(arg);
        }
        payload.addObject();
        payload.addObject()
                .putNull("callbacks")
                .putNull("chain")
                .putNull("chord")
                .putNull("errbacks");

        Message message = broker.newMessage();
        message.setBody(jsonMapper.writeValueAsBytes(payload));
        message.setContentEncoding("utf-8");
        message.setContentType("application/json");

        Message.Headers headers = message.getHeaders();
        headers.setId(taskId);
        headers.setArgsRepr("(" + Joiner.on(", ").join(args) + ")");
        headers.setOrigin(clientName);
        if (resultsProvider.isPresent()) {
            headers.setReplyTo(clientId);
        }

        message.send(queue);

        Future<?> result;
        if (resultsProvider.isPresent()) {
            result = resultsProvider.get().getResult("x");
        } else {
            result = CompletableFuture.completedFuture(null);
        }
        return new AsyncResultImpl(result);
    }

    interface AsyncResult {
        boolean isDone();

        Object get() throws ExecutionException, InterruptedException;
    }

    private class AsyncResultImpl implements AsyncResult {

        private final Future<?> future;

        AsyncResultImpl(Future<?> future) {
            this.future = future;
        }

        @Override
        public boolean isDone() {
            return future.isDone();
        }

        @Override
        public Object get() throws ExecutionException, InterruptedException {
            return future.get();
        }
    }
}
