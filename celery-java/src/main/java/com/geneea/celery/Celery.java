package com.geneea.celery;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Joiner;
import com.google.common.base.Suppliers;
import lombok.Builder;
import lombok.extern.java.Log;
import com.geneea.celery.backends.CeleryBackends;
import com.geneea.celery.brokers.CeleryBrokers;
import com.geneea.celery.spi.Backend;
import com.geneea.celery.spi.Broker;
import com.geneea.celery.spi.Message;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * A client allowing you to submit a task and get a {@link Future} describing the result.
 */
@Log
public class Celery {
    private final String clientId = UUID.randomUUID().toString();
    private final String clientName = clientId + "@" + getLocalHostName();
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final String queue;

    // Memoized suppliers help us to deal with a connection that can't be established yet. It may fail several times
    // with an exception but when it succeeds, it then always returns the same instance.
    //
    // This is tailored for the RabbitMQ connections - they fail to be created if the host can't be reached but they
    // can heal automatically. If other brokers/backends don't work this way, we might need to rework it.
    private final Supplier<Optional<Backend.ResultsProvider>> resultsProvider;
    private final Supplier<Broker> broker;

    /**
     * Create a Celery client that can submit tasks and get the results from the backend.
     *
     * @param brokerUri connection to broker that will dispatch messages
     * @param backendUri connection to backend providing responses
     * @param queue routing tag (specifies into which Rabbit queue the messages will go)
     */
    @Builder
    private Celery(final String brokerUri,
                   @Nullable final String queue,
                   @Nullable final String backendUri,
                   @Nullable final ExecutorService executor,
                   @Nullable final boolean isPriQueue,
                   @Nullable final int maxPriority) {
        this.queue = queue == null ? "celery" : queue;

        ExecutorService executorService = executor != null ? executor : Executors.newCachedThreadPool();

        broker = Suppliers.memoize(() -> {
            Broker b = CeleryBrokers.createBroker(brokerUri, executorService);
            try {
                if(isPriQueue  && maxPriority != 0){
                    b.declarePriQueue(Celery.this.queue, maxPriority);
                }
                else {
                    b.declareQueue(Celery.this.queue);
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return b;
        });

        resultsProvider = Suppliers.memoize(() -> {
            if (backendUri == null) {
                return Optional.empty();
            }

            Backend.ResultsProvider rp;
            try {
                rp = CeleryBackends.create(backendUri, executorService)
                                   .resultsProviderFor(clientId);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return Optional.of(rp);
        });
    }

    private String getLocalHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return  "unknown";
        }
    }

    /**
     * Submit a Java task for processing. You'll probably not need to call this method. rather use @{@link CeleryTask}
     * annotation.
     *
     * @param taskClass task implementing class
     * @param method method in {@code taskClass} that does the work
     * @param args positional arguments for the method (need to be JSON serializable)
     * @return asynchronous result
     *
     * @throws IOException if the message couldn't be sent
     */
    public AsyncResult<?> submit(Class<?> taskClass, String method, Object[] args) throws IOException {
        return submit(taskClass.getName() + "#" + method, args);
    }

    /**
     * Submit a Java task for processing with priority. You'll probably not need to call this method. rather use @{@link CeleryTask}
     * annotation.
     *
     * @param taskClass task implementing class
     * @param method method in {@code taskClass} that does the work
     * @param priority the priority of the task
     * @param args positional arguments for the method (need to be JSON serializable)
     * @return asynchronous result
     *
     * @throws IOException if the message couldn't be sent
     */
    public AsyncResult<?> submitWithPri(Class<?> taskClass, String method, int priority, Object[] args) throws IOException {
        return submitWithPri(taskClass.getName() + "#" + method, priority, args);
    }

    /**
     * Submit a task by name. A low level method for submitting arbitrary tasks that don't have their proxies
     * generated by @{@link CeleryTask} annotation.
     *
     * @param name task name as understood by the worker
     * @param args positional arguments for the method (need to be JSON serializable)
     * @return asynchronous result
     *
     * @throws IOException if the message couldn't be sent
     */
    public AsyncResult<?> submit(String name, Object[] args) throws IOException {
        // Get the provider early to increase the chance to find out there is a connection problem before actually
        // sending the message.
        //
        // This will help for example in the case when the connection can't be established at all. The connection may
        // still drop after sending the message but there isn't much we can do about it.
        Optional<Backend.ResultsProvider> rp = resultsProvider.get();
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

        Message message = broker.get().newMessage();
        message.setBody(jsonMapper.writeValueAsBytes(payload));
        message.setContentEncoding("utf-8");
        message.setContentType("application/json");

        Message.Headers headers = message.getHeaders();
        headers.setId(taskId);
        headers.setTaskName(name);
        headers.setArgsRepr("(" + Joiner.on(", ").join(args) + ")");
        headers.setOrigin(clientName);
        if (rp.isPresent()) {
            headers.setReplyTo(clientId);
        }

        message.send(queue);

        Future<Object> result;
        if (rp.isPresent()) {
            result = rp.get().getResult(taskId);
        } else {
            result = CompletableFuture.completedFuture(null);
        }
        return new AsyncResultImpl<>(result);
    }

    /**
     * Submit a task by name with priority.
     *
     * @param name task name as understood by the worker
     * @param priority the priority of the message
     * @param args positional arguments for the method (need to be JSON serializable)
     * @return asynchronous result
     * @throws IOException
     */
    public AsyncResult<?> submitWithPri(String name, int priority, Object[] args) throws IOException {
        // Get the provider early to increase the chance to find out there is a connection problem before actually
        // sending the message.
        //
        // This will help for example in the case when the connection can't be established at all. The connection may
        // still drop after sending the message but there isn't much we can do about it.
        Optional<Backend.ResultsProvider> rp = resultsProvider.get();
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

        Message message = broker.get().newMessageWithPriority(priority);
        message.setBody(jsonMapper.writeValueAsBytes(payload));
        message.setContentEncoding("utf-8");
        message.setContentType("application/json");

        Message.Headers headers = message.getHeaders();
        headers.setId(taskId);
        headers.setTaskName(name);
        headers.setArgsRepr("(" + Joiner.on(", ").join(args) + ")");
        headers.setOrigin(clientName);
        if (rp.isPresent()) {
            headers.setReplyTo(clientId);
        }

        System.out.println(queue);
        message.send(queue);

        Future<Object> result;
        if (rp.isPresent()) {
            result = rp.get().getResult(taskId);
        } else {
            result = CompletableFuture.completedFuture(null);
        }
        return new AsyncResultImpl<>(result);
    }

    public interface AsyncResult<T> {
        boolean isDone();

        T get() throws ExecutionException, InterruptedException;
    }

    private class AsyncResultImpl<T> implements AsyncResult<T> {

        private final Future<T> future;

        AsyncResultImpl(Future<T> future) {
            this.future = future;
        }

        @Override
        public boolean isDone() {
            return future.isDone();
        }

        @Override
        public T get() throws ExecutionException, InterruptedException {
            return future.get();
        }
    }
}
