package org.sedlakovi.celery.examples;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.sedlakovi.celery.Client;
import org.sedlakovi.celery.RabbitBackend;

import java.util.concurrent.Executors;

/**
 * Created by krab on 23.9.17.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection(Executors.newCachedThreadPool());

        RabbitBackend backend = new RabbitBackend(connection.createChannel());
        Client client = new Client(connection.createChannel(), backend);

        try {
            System.out.println(client.submit(TestTask.class, "run", 1, 2).get());
            System.out.println(client.submit(TestVoidTask.class, "run", 1, 2).get());
            System.out.println(client.submit(TestTask.class, "run", "a", "b").get());
        } finally {
            connection.close();
        }
    }
}
