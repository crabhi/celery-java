package org.sedlakovi.celery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.primitives.Primitives;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


class RabbitMessageConsumer extends DefaultConsumer {

    private final ObjectMapper jsonMapper;
    private final Lock taskRunning = new ReentrantLock();
    private final RabbitBackend backend;

    private static final Logger LOG = Logger.getLogger(RabbitMessageConsumer.class.getName());

    RabbitMessageConsumer(Channel channel, RabbitBackend backend) {
        super(channel);
        this.backend = backend;
        jsonMapper = new ObjectMapper();
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope,
                               AMQP.BasicProperties properties, byte[] body)
            throws IOException {
        taskRunning.lock();
        String taskId = properties.getHeaders().get("id").toString();
        try {
            Stopwatch stopwatch = Stopwatch.createStarted();
            String message = new String(body, properties.getContentEncoding());

            // Most time spent here. Up to 40 ms.
            List<?> payload = jsonMapper.readValue(message, List.class);

            String taskClassName = properties.getHeaders().get("task").toString();
            Optional<Object> result = processTask(
                    taskClassName,
                    (List<?>) payload.get(0));

            LOG.info(String.format("Task %s[%s] succeeded in %s. Result was: %s",
                    taskClassName, taskId, stopwatch, result));

            if (result.isPresent()) {
                backend.reportResult(taskId, properties.getReplyTo(), properties.getCorrelationId(), result.get());
            }

            getChannel().basicAck(envelope.getDeliveryTag(), false);
        } catch (DispatchException e) {
            LOG.log(Level.SEVERE, String.format("Task %s dispatch error", taskId), e.getCause());
            backend.reportException(taskId, properties.getReplyTo(), properties.getCorrelationId(), e);
            getChannel().basicAck(envelope.getDeliveryTag(), false);
        } catch (InvocationTargetException e) {
            LOG.log(Level.WARNING, String.format("Task %s error", taskId), e.getCause());
            backend.reportException(taskId, properties.getReplyTo(), properties.getCorrelationId(), e.getCause());
            getChannel().basicAck(envelope.getDeliveryTag(), false);
        } catch (JsonProcessingException e) {
            LOG.log(Level.SEVERE, String.format("Task %s - %s", taskId, e), e.getCause());
            getChannel().basicNack(envelope.getDeliveryTag(), false, false);
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, String.format("Task %s - %s", taskId, e), e);
            getChannel().basicNack(envelope.getDeliveryTag(), false, false);
        } finally {
            taskRunning.unlock();
        }
    }

    private Optional<Object> processTask(String taskClassName, List<?> args)
            throws DispatchException, InvocationTargetException {

        Task task = TaskRegistry.getTask(taskClassName);

        if (task == null) {
            throw new DispatchException(String.format("Task %s not registered.", taskClassName));
        }

        List<Class<?>> argTypes = args.stream()
                .map(Object::getClass)
                .collect(Collectors.toList());

        Optional<Method> maybeMethod = findRunMethod(task.getClass(), argTypes);

        if (!maybeMethod.isPresent()) {
            throw new DispatchException(String.format("Method run(%s) not present in %s",
                    Joiner.on(",").join(argTypes), taskClassName));
        }
        Method method = maybeMethod.get();

        Object result;
        try {
            result = method.invoke(task, args.toArray());
        } catch (IllegalAccessException e) {
            throw new DispatchException(String.format("Error calling %s", method), e);
        } catch (IllegalArgumentException e) {
            // should not happen because of findRunMethod
            throw new AssertionError(String.format("Error calling %s", method), e);
        }

        if (method.getReturnType().equals(void.class)) {
            assert result == null;
            return Optional.empty();
        } else {
            return Optional.of(result);
        }
    }

    void close() throws IOException {
        getChannel().abort();
        backend.close();
    }

    void join() {
        taskRunning.lock();
    }

    private static Optional<Method> findRunMethod(Class<?> cls, List<Class<?>> args) {
        return Arrays.stream(cls.getDeclaredMethods())
                .filter((m) -> m.getName().equals("run"))
                .filter((m) -> {
                    Class<?>[] parameterTypes = m.getParameterTypes();
                    if (parameterTypes.length != args.size()) {
                        return false;
                    }

                    for (int i = 0; i < args.size(); i++) {
                        if (!Primitives.wrap(parameterTypes[i]).isAssignableFrom(args.get(i))) {
                            return false;
                        }
                    }
                    return true;
                }).findAny();
    }
}
