package com.geneea.celery.brokers.rabbit;

import com.google.common.collect.ImmutableSet;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.kohsuke.MetaInfServices;
import com.geneea.celery.spi.Broker;
import com.geneea.celery.spi.BrokerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

@MetaInfServices(BrokerFactory.class)
public class RabbitBrokerFactory implements BrokerFactory {
    @Override
    public Set<String> getProtocols() {
        return ImmutableSet.of("amqp", "amqps");
    }

    @Override
    public Broker createBroker(URI uri, ExecutorService executor) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        try {
            factory.setUri(uri);
        } catch (NoSuchAlgorithmException | KeyManagementException | URISyntaxException e) {
            throw new IOException(e);
        }

        Connection connection = factory.newConnection(executor);
        return new RabbitBroker(connection.createChannel());
    }
}
