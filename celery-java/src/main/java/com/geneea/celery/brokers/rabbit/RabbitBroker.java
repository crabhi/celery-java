package com.geneea.celery.brokers.rabbit;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.geneea.celery.spi.Broker;
import com.geneea.celery.spi.Message;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class RabbitBroker implements Broker {
    private final Channel channel;

    public RabbitBroker(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void declareQueue(String name) throws IOException {
        channel.queueDeclare(name, true, false, false, null);
    }

    @Override
    public void declareQueue(String name, int maxPriority) throws IOException {
        Map<String, Object> props = new HashMap<>();
        props.put("x-max-priority", maxPriority);
        channel.queueDeclare(name, true, false, false, props);
    }

    @Override
    public Message newMessage() {
        return new RabbitMessage();
    }

    public Channel getChannel() {
        return channel;
    }

    @Override
    public Message newMessage(int priority) {
        return new RabbitMessage(priority);
    }

    class RabbitMessage implements Message {
        private byte[] body;
        private final AMQP.BasicProperties.Builder props;

        private final RabbitMessageHeaders headers = new RabbitMessageHeaders();

        public RabbitMessage(){
            props = new AMQP.BasicProperties.Builder()
                    .deliveryMode(2)
                    .priority(0);
        }

        public RabbitMessage(int priority){
            props = new AMQP.BasicProperties.Builder()
                    .deliveryMode(2)
                    .priority(priority);
        }

        @Override
        public void setBody(byte[] body) {
            this.body = body;
        }

        @Override
        public void setContentEncoding(String contentEncoding) {
            props.contentEncoding(contentEncoding);
        }

        @Override
        public void setContentType(String contentType) {
            props.contentType(contentType);
        }

        @Override
        public Headers getHeaders() {
            return headers;
        }

        @Override
        public void send(String queue) throws IOException {
            AMQP.BasicProperties messageProperties = props.headers(headers.map).build();
            channel.basicPublish("", queue, messageProperties, body);
        }

        class RabbitMessageHeaders implements Message.Headers {

            private final Map<String, Object> map = new HashMap<>();

            RabbitMessageHeaders() {
                map.put("timelimit", Arrays.asList(null, null));
                map.put("retries", 0);
                map.put("parent_id", null);
                map.put("kwargsrepr", "{}");
                map.put("expires", null);
                map.put("eta", null);
                map.put("lang", "py"); // sic
                map.put("group", null);
            }

            @Override
            public void setId(String id) {
                props.correlationId(id);
                map.put("root_id", id);
                map.put("id", id);
            }

            @Override
            public void setArgsRepr(String argsRepr) {
                map.put("argsrepr", argsRepr);
            }

            @Override
            public void setOrigin(String origin) {
                map.put("origin", origin);
            }

            @Override
            public void setReplyTo(String clientId) {
                props.replyTo(clientId);
            }

            @Override
            public void setTaskName(String task) {
                map.put("task", task);
            }
        }
    }
}
/*

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
 */