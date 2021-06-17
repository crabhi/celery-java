package com.geneea.celery.examples;

import com.google.common.base.Stopwatch;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.geneea.celery.Celery;
import com.geneea.celery.CeleryWorker;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        //factory.setHost("localhost");
        ExecutorService executor = Executors.newCachedThreadPool();
        factory.setUri("amqp://guest:123456@10.12.6.19:5672/10.12.6.19");
        factory.setUsername("guest");
        factory.setPassword("123456");
        factory.setVirtualHost("10.12.6.19");

        Connection connection = factory.newConnection();

        connection.close();

        //CeleryWorker worker = CeleryWorker.create("celery", connection);

        Celery client = Celery.builder()
                .queue("test_idata1")
                .brokerUri("amqp://guest:123456@10.12.6.19:5672/10.12.6.19")
                .backendUri("rpc://guest:123456@10.12.6.19:5672/10.12.6.19")
                .maxPriority(Optional.of(10))
                .build();

        try {
            /*for (int i = 0; i < 20; i++) {
                Stopwatch sw = Stopwatch.createStarted();
                Integer result = TestTaskProxy.with(client).sum(1, i).get();
                System.out.printf("CeleryTask #%d's result was: %s. The task took %s end-to-end.\n", i, result, sw);
            }*/

            //System.out.println("Testing result of void task: " + TestVoidTaskProxy.with(client).run(1, 2).get());


            String re="hh";
            System.out.println("Testing task that should fail and throw exception:");
            for(int i=0;i <10000; i++){
                Celery.AsyncResult t=client.submit(TestTask.class, "sum", new Object[]{re, 'd'});
                System.out.println( String.format("current is %s  with id :%s", i, t.getTaskId()));

                //boolean res= executor.isShutdown();
            }

        } finally {

            Connection cb= client.getBackendConnection();
            Connection cbr = client.getBrokerConnection();
            cb.close();
            cbr.close();
        }

        // The worker threads hang waiting for the messages for some reason for quite a long time but eventually,
        // the process finishes.
    }
}
