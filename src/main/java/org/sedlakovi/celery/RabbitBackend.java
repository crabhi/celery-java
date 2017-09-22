package org.sedlakovi.celery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import java.io.IOException;

class RabbitBackend {
    private final Channel channel;
    private final ObjectMapper jsonMapper;

    RabbitBackend(Channel channel) {
        this.channel = channel;
        jsonMapper = new ObjectMapper();
    }

    void reportResult(String taskId, String replyTo, String correlationId, Object result)
            throws IOException {

        System.out.printf("Reporting result %s\n", result);

        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .correlationId(correlationId)
                .priority(0)
                .deliveryMode(1)
                .contentType("application/json")
                .contentEncoding("utf-8")
                .build();

        ObjectNode o = jsonMapper.createObjectNode();
        o.putArray("children");
        o.putNull("traceback");
        o.putPOJO("result", result);
        o.put("task_id", taskId);
        o.put("status", "SUCCESS");

        channel.basicPublish("", replyTo, properties, jsonMapper.writeValueAsBytes(o));
        // {"children": [], "traceback": null, "result": 3, "task_id": "93b51f54-8dde-4ab3-8226-04fb0afa546c", "status": "SUCCESS"}
    }

    void reportException(String taskId, String replyTo, String correlationId, Throwable e) throws IOException {
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .correlationId(correlationId)
                .priority(0)
                .deliveryMode(1)
                .contentType("application/json")
                .contentEncoding("utf-8")
                .build();

        ObjectNode o = jsonMapper.createObjectNode();
        o.putArray("children");
        o.putNull("traceback");
        o.putObject("result")
            .put("exc_type", e.getClass().getSimpleName())
            .put("exc_message", e.getMessage());
        o.put("task_id", taskId);
        o.put("status", "FAILURE");

        channel.basicPublish("", replyTo, properties, jsonMapper.writeValueAsBytes(o));
        // {"children": [], "status": "FAILURE", "result": {"exc_type": "NotRegistered", "exc_message": "'org.sedlakovi.celery.TestTask'"}, "traceback": null, "task_id": "f4f264f9-739b-4fab-85fa-cdb3f73dbef5"}
    }

    void close() throws IOException {
        channel.abort();
    }
}
