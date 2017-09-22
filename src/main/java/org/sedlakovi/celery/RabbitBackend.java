package org.sedlakovi.celery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

    void reportResult(String taskId, Object result) {
        System.out.printf("Reporting result %s\n", result);

        ObjectNode o = jsonMapper.createObjectNode();
        o.putArray("children");
        o.putNull("traceback");
        o.putPOJO("result", result);
        o.put("task_id", taskId);
        o.put("status", "SUCCESS");
        // {"children": [], "traceback": null, "result": 3, "task_id": "93b51f54-8dde-4ab3-8226-04fb0afa546c", "status": "SUCCESS"}
    }

    void reportException(Throwable e) {
        // e.printStackTrace();
        // throw new NotImplementedException();
    }

    void close() throws IOException {
        channel.abort();
    }
}
