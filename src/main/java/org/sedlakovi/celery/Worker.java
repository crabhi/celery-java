
package org.sedlakovi.celery;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class Worker {

    private static class Args {

        @Parameter(names = "--queue", description = "Celery queue to watch")
        private String queue = "celery";

        @Parameter(names = "--concurrency", description = "Number of concurrent tasks to process")
        private int numWorkers = 2;
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

        final List<RabbitMessageConsumer> consumers = new ArrayList<>();

        for (int i = 0; i < args.numWorkers; i++) {
            final Channel channel = connection.createChannel();
            channel.queueDeclare(args.queue, true, false, false, null);
            RabbitBackend backend = new RabbitBackend(channel);
            RabbitMessageConsumer consumer = new RabbitMessageConsumer(channel, backend);
            channel.basicConsume(args.queue, false, "", true, false, null, consumer);
            consumers.add(consumer);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (RabbitMessageConsumer consumer : consumers) {
                try {
                    consumer.close();
                    consumer.join();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }));

        System.out.println(String.format("Started consuming tasks from queue %s.", args.queue));
        System.out.println("Known tasks:");
        for (String taskName : TaskRegistry.getRegisteredTaskNames()) {
            System.out.print("  - ");
            System.out.println(taskName);
        }
    }

}
