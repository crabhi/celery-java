package com.geneea.celery.examples;

import com.google.common.base.Stopwatch;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.geneea.celery.Celery;
import com.geneea.celery.CeleryWorker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
       // factory.setHost("localhost");
       // ExecutorService executor = Executors.newCachedThreadPool();
       // Connection connection = factory.newConnection(executor);

       // CeleryWorker worker = CeleryWorker.create("celery", connection);

        Celery client = Celery.builder()
                .queue("test_idata1")
                .brokerUri("amqp://guest:123456@10.12.7.203:5672/10.12.7.203")
                .backendUri("rpc://guest:123456@10.12.7.203:5672/10.12.7.203")
                .build();
        try {
            client.submit("tasks.add", new Object[]{1, 2});
        } finally {
          //  worker.close();
            //worker.join();
           // executor.shutdown();
        }

        // The worker threads hang waiting for the messages for some reason for quite a long time but eventually,
        // the process finishes.
    }
}
