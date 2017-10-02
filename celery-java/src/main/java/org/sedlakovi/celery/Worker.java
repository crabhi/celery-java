
package org.sedlakovi.celery;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.primitives.Primitives;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Worker that listens on RabbitMQ queue and executes tasks. You can either embed it into your project via
 * {@link #create(String, Connection)} or start it stand-alone and supply your tasks on classpath like this:
 *
 * <pre>
 * java -cp celery-java-xyz.jar:your-tasks.jar org.sedlakovi.celery.Worker --concurrency 8
 * </pre>
 */
public class Worker extends DefaultConsumer {

    private final ObjectMapper jsonMapper;
    private final Lock taskRunning = new ReentrantLock();
    private final RabbitBackend backend;

    private static final Logger LOG = Logger.getLogger(Worker.class.getName());

    public Worker(Channel channel, RabbitBackend backend) {
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

            List<?> payload = jsonMapper.readValue(message, List.class);

            String taskClassName = properties.getHeaders().get("task").toString();
            Object result = processTask(
                    taskClassName,
                    (List<?>) payload.get(0));

            LOG.info(String.format("Task %s[%s] succeeded in %s. Result was: %s",
                    taskClassName, taskId, stopwatch, result));

            backend.reportResult(taskId, properties.getReplyTo(), properties.getCorrelationId(), result);

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

    private Object processTask(String taskClassName, List<?> args)
            throws DispatchException, InvocationTargetException {

        Task task = TaskRegistry.getTask(taskClassName);

        if (task == null) {
            throw new DispatchException(String.format("Task %s not registered.", taskClassName));
        }

        List<Class<?>> argTypes = args.stream()
                .map(Object::getClass)
                .collect(Collectors.toList());

        Optional<java.lang.reflect.Method> maybeMethod = findRunMethod(task.getClass(), argTypes);

        if (!maybeMethod.isPresent()) {
            throw new DispatchException(String.format("Method run(%s) not present in %s",
                    Joiner.on(",").join(argTypes), taskClassName));
        }
        java.lang.reflect.Method method = maybeMethod.get();

        try {
            return method.invoke(task, args.toArray());
        } catch (IllegalAccessException e) {
            throw new DispatchException(String.format("Error calling %s", method), e);
        } catch (IllegalArgumentException e) {
            // should not happen because of findRunMethod
            throw new AssertionError(String.format("Error calling %s", method), e);
        }
    }

    public void close() throws IOException {
        getChannel().abort();
        backend.close();
    }

    public void join() {
        taskRunning.lock();
        taskRunning.unlock();
    }

    private static Optional<java.lang.reflect.Method> findRunMethod(Class<?> cls, List<Class<?>> args) {
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

    private static class Args {

        @Parameter(names = "--queue", description = "Celery queue to watch")
        private String queue = "celery";

        @Parameter(names = "--concurrency", description = "Number of concurrent tasks to process")
        private int numWorkers = 2;
    }

    public static Worker create(String queue, Connection connection) throws IOException {
        final Channel channel = connection.createChannel();
        channel.basicQos(2);
        channel.queueDeclare(queue, true, false, false, null);
        RabbitBackend backend = new RabbitBackend(channel);
        final Worker consumer = new Worker(channel, backend);
        channel.basicConsume(queue, false, "", true, false, null, consumer);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                consumer.close();
                consumer.join();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));

        return consumer;
    }

    public static void main(String[] argv) throws Exception {
        Args args = new Args();
        JCommander.newBuilder()
                .addObject(args)
                .build()
                .parse(argv);

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection(Executors.newCachedThreadPool());

        for (int i = 0; i < args.numWorkers; i++) {
            create(args.queue, connection);
        }

        System.out.println(String.format("Started consuming tasks from queue %s.", args.queue));
        System.out.println("Known tasks:");
        for (String taskName : TaskRegistry.getRegisteredTaskNames()) {
            System.out.print("  - ");
            System.out.println(taskName);
        }
    }
}
