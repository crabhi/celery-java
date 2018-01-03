package org.sedlakovi.celery.examples;

import com.google.common.base.Stopwatch;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.sedlakovi.celery.Celery;
import org.sedlakovi.celery.CeleryWorker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        ExecutorService executor = Executors.newCachedThreadPool();
        Connection connection = factory.newConnection(executor);

        CeleryWorker worker = CeleryWorker.create("celery", connection);

        Celery client = Celery.builder()
                .brokerUri("amqp://localhost/%2F")
                .backendUri("rpc://localhost/%2F")
                .build();

        try {
            for (int i = 0; i < 20; i++) {
                Stopwatch sw = Stopwatch.createStarted();
                Integer result = TestTaskProxy.with(client).sum(1, i).get();
                System.out.printf("CeleryTask #%d's result was: %s. The task took %s end-to-end.\n", i, result, sw);
            }

            System.out.println("Testing result of void task: " + TestVoidTaskProxy.with(client).run(1, 2).get());
            System.out.println("Testing task that should fail and throw exception:");
            client.submit(TestTask.class, "sum", new Object[]{"a", "b"}).get();
        } finally {
            connection.close();
            worker.close();
            worker.join();
            executor.shutdown();
        }

        // The worker threads hang waiting for the messages for some reason for quite a long time but eventually,
        // the process finishes.
    }
}
