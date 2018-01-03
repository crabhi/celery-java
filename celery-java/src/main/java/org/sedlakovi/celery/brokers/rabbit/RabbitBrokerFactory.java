package org.sedlakovi.celery.brokers.rabbit;

import com.google.common.collect.ImmutableList;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.kohsuke.MetaInfServices;
import org.sedlakovi.celery.brokers.BrokerFactory;
import org.sedlakovi.celery.spi.Broker;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

@MetaInfServices(BrokerFactory.class)
public class RabbitBrokerFactory implements BrokerFactory {
    @Override
    public List<String> getProtocols() {
        return ImmutableList.of("amqp", "amqps");
    }

    @Override
    public Broker createBroker(String uri, ExecutorService executor) throws IOException, URISyntaxException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        try {
            factory.setUri(uri);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        } catch (KeyManagementException e) {
            throw new IOException(e);
        }

        Connection connection = factory.newConnection(executor);
        return new RabbitBroker(connection.createChannel());
    }
}
