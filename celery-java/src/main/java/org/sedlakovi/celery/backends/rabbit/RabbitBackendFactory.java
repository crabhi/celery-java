package org.sedlakovi.celery.backends.rabbit;

import com.google.common.collect.ImmutableSet;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.http.client.utils.URIBuilder;
import org.kohsuke.MetaInfServices;
import org.sedlakovi.celery.spi.Backend;
import org.sedlakovi.celery.spi.BackendFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

@MetaInfServices(BackendFactory.class)
public class RabbitBackendFactory implements BackendFactory {
    @Override
    public Set<String> getProtocols() {
        return ImmutableSet.of("rpc");
    }

    @Override
    public Backend createBackend(URI uri, ExecutorService executor) throws IOException, TimeoutException {
        // Replace rpc:// -> amqp:// to be consistent with the Python API. The Python API uses rpc:// to designate AMQP
        // used in the manner of one return queue per client as opposed to one queue per returned message (the original
        // amqp:// protocol).
        //
        // The underlying RabbitMQ library wouldn't understand the rpc scheme so we construct the URI it can understand.
        URI correctSchemeUri;
        try {
            assert "rpc".equals(uri.getScheme());
            correctSchemeUri = new URIBuilder(uri).setScheme("amqp").build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        ConnectionFactory factory = new ConnectionFactory();
        try {
            factory.setUri(correctSchemeUri);
        } catch (NoSuchAlgorithmException | KeyManagementException | URISyntaxException e) {
            throw new IOException(e);
        }

        Connection connection = factory.newConnection(executor);
        return new RabbitBackend(connection.createChannel());
    }
}
