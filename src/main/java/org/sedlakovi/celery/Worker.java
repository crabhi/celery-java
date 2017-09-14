
package org.sedlakovi.celery;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeCreator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import com.google.common.primitives.Primitives;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Worker {

    private static class Args {

        @Parameter(names = "--queue", description = "Celery queue to watch")
        private String queue = "celery";

        @Parameter(names = "--concurrency", description = "Number of concurrent tasks to process")
        private int numWorkers = 2;
    }

    private static final Map<String, Task> TASKS = Streams
            .stream(ServiceLoader.load(Task.class))
            .collect(ImmutableMap.toImmutableMap((v) -> v.getClass().getName(), Function.identity()));

    public static void main(String[] argv) throws Exception {
        Args args = new Args();
        JCommander.newBuilder()
                .addObject(args)
                .build()
                .parse(argv);

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection(Executors.newCachedThreadPool());

        final List<RabbitMessageConsumer> consumers = new ArrayList<>();

        for (int i = 0; i < args.numWorkers; i++) {
            final Channel channel = connection.createChannel();
            channel.queueDeclare(args.queue, true, false, false, null);
            RabbitBackend backend = new RabbitBackend(connection);
            RabbitMessageConsumer consumer = new RabbitMessageConsumer(channel, backend);
            channel.basicConsume(args.queue, false, "", true, false, null, consumer);
            consumers.add(consumer);
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                for (RabbitMessageConsumer consumer : consumers) {
                    try {
                        consumer.getChannel().abort();
                        consumer.join();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        System.out.println(String.format("Started consuming tasks from queue %s.", args.queue));
        System.out.println("Known tasks:");
        for (String taskName : TASKS.keySet()) {
            System.out.print("  - ");
            System.out.println(taskName);
        }
    }

    private static class RabbitBackend {
        private final Connection connection;
        private final ObjectMapper jsonMapper;

        RabbitBackend(Connection connection) {
            this.connection = connection;
            jsonMapper = new ObjectMapper();
        }

        private void reportResult(String taskId, Object result) {
            System.out.printf("Reporting result %s\n", result);

            ObjectNode o = jsonMapper.createObjectNode();
            o.putArray("children");
            o.putNull("traceback");
            o.putPOJO("result", result);
            o.put("task_id", taskId);
            o.put("status", "SUCCESS");
            // {"children": [], "traceback": null, "result": 3, "task_id": "93b51f54-8dde-4ab3-8226-04fb0afa546c", "status": "SUCCESS"}
        }

        private void reportException(Throwable e) {
            // e.printStackTrace();
            // throw new NotImplementedException();
        }
    }

    private static class RabbitMessageConsumer extends DefaultConsumer {

        private final ObjectMapper jsonMapper;
        private Lock taskRunning = new ReentrantLock();
        private static final Logger LOG = Logger.getLogger(RabbitMessageConsumer.class.getName());
        private final RabbitBackend backend;

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
                        (List<Object>) payload.get(0));

                LOG.info(String.format("Task %s succeeded in %s. Result was: %s", taskId, stopwatch, result));

                if (result.isPresent()) {
                    backend.reportResult(taskId, result.get());
                }

                getChannel().basicAck(envelope.getDeliveryTag(), false);
            } catch (Worker.DispatchException e) {
                LOG.log(Level.SEVERE, String.format("Task %s dispatch error", taskId), e.getCause());
                getChannel().basicNack(envelope.getDeliveryTag(), false, false);
            } catch (InvocationTargetException e) {
                LOG.log(Level.WARNING, String.format("Task %s error", taskId), e.getCause());
                backend.reportException(e.getCause());
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

        private Optional<Object> processTask(String taskClassName, List<Object> args)
                throws Worker.DispatchException, InvocationTargetException {

            Task task = Worker.TASKS.get(taskClassName);

            if (task == null) {
                throw new Worker.DispatchException(String.format("Task %s not registered.", taskClassName));
            }

            List<Class<?>> argTypes = args.stream()
                    .map(Object::getClass)
                    .collect(Collectors.toList());

            Optional<Method> maybeMethod = findMethod(task.getClass(), "run", argTypes);

            if (!maybeMethod.isPresent()) {
                throw new Worker.DispatchException(String.format("Method run(%s) not present in %s",
                        Joiner.on(",").join(argTypes), taskClassName));
            }
            Method method = maybeMethod.get();

            Object result;
            try {
                result = method.invoke(task, args.toArray());
            } catch (IllegalAccessException e) {
                throw new Worker.DispatchException(String.format("Error calling %s", method), e);
            } catch (IllegalArgumentException e) {
                // should not happen because of findMethod
                throw new AssertionError(String.format("Error calling %s", method), e);
            }

            if (method.getReturnType().equals(void.class)) {
                assert result == null;
                return Optional.empty();
            } else {
                return Optional.of(result);
            }
        }

        void join() {
            taskRunning.lock();
        }

        private static Optional<Method> findMethod(Class<?> cls, String name, List<Class<?>> args) {
            return Arrays.stream(cls.getDeclaredMethods())
                    .filter((m) -> m.getName().equals(name))
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

    private static class DispatchException extends Exception {
        DispatchException(String msg) {
            super(msg);
        }

        DispatchException(String message, Throwable cause) {
            super(cause);
        }
    }
}
