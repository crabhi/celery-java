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
                .brokerUri("**********")
                .backendUri("**********")
                .isPriQueue(true)
                .maxPriority(10)
                .build();
        try {
            Thread td = new Thread(){
                public void run() {
                    try{
                        for(int i=0 ; i < 10 ; i++){
                            client.submitWithPri("tasks.add",2, new Object[]{0, 2});
                        }
                    }catch (Exception ex){

                    }
                }
            };

            Thread td2 = new Thread(){
                public void run() {
                    try{
                        for(int i=0 ; i < 10 ; i++){
                            client.submitWithPri("tasks.add",8, new Object[]{(i+1)*10, 8});
                        }
                    }catch (Exception ex){

                    }
                }
            };

            td.start();
            td2.start();

        } finally {
          //  worker.close();
            //worker.join();
           // executor.shutdown();
        }

        // The worker threads hang waiting for the messages for some reason for quite a long time but eventually,
        // the process finishes.
    }
}
