package org.sedlakovi.celery;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Joiner;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Client {
    private final Channel channel;
    private final String queue;
    private final String clientId;
    private final String clientName;
    private final ObjectMapper jsonMapper;
    private final RabbitResultConsumer resultConsumer;

    public Client(Channel channel, RabbitBackend backend, String queue) throws IOException {
        this.channel = channel;
        this.queue = queue;
        this.clientId = UUID.randomUUID().toString();
        this.clientName = clientId + "@" + InetAddress.getLocalHost().getHostName();
        this.jsonMapper = new ObjectMapper();

        this.resultConsumer = backend.createResultConsumer(clientId);
    }

    public Client(Channel channel, RabbitBackend backend) throws IOException {
        this(channel, backend, "celery");
    }

    public Future<?> submit(Class<? extends Task> taskClass, Object... args) throws IOException {
        return submit(taskClass.getName(), args);
    }

    public Future<?> submit(String name, Object... args) throws IOException {
        String taskId = UUID.randomUUID().toString();

        Map<String, Object> headers = new HashMap<>();
        headers.put("timelimit", Arrays.asList(null, null));
        headers.put("task", name);
        headers.put("retries", 0);
        headers.put("argsrepr", "(" + Joiner.on(", ").join(args) + ")");
        headers.put("parent_id", null);
        headers.put("root_id", taskId);
        headers.put("id", taskId);
        headers.put("kwargsrepr", "{}");
        headers.put("expires", null);
        headers.put("eta", null);
        headers.put("lang", "py"); // sic
        headers.put("group", null);
        headers.put("origin", clientName);

        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .replyTo(clientId)
                .correlationId(taskId)
                .priority(0)
                .deliveryMode(2)
                .headers(headers)
                .contentEncoding("utf-8")
                .contentType("application/json")
                .build();

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

        channel.basicPublish("", queue, props, jsonMapper.writeValueAsBytes(payload));

        return resultConsumer.getResult(taskId);
    }

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection(Executors.newCachedThreadPool());

        RabbitBackend backend = new RabbitBackend(connection.createChannel());
        Client client = new Client(connection.createChannel(), backend);

        try {
            System.out.println(client.submit(TestTask.class, 1, 2).get());
            System.out.println(client.submit(TestVoidTask.class, 1, 2).get());
            System.out.println(client.submit(TestTask.class, "a", "b").get());
        } finally {
            connection.close();
        }
    }

}
