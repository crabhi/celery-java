package org.sedlakovi.celery.brokers;


import com.google.common.collect.ImmutableSet;
import org.sedlakovi.celery.UnsupportedProtocolException;
import org.sedlakovi.celery.spi.Broker;
import org.sedlakovi.celery.spi.BrokerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

/**
 * Internal utility to get the right {@link Broker} instance from a URI.
 */
public class CeleryBrokers {

    /**
     * Create a new broker.
     *
     * @param uri connection to broker
     * @param executor for background tasks
     * @return new broker instance
     */
    public static Broker createBroker(String uri, ExecutorService executor) {

        URI parsedUri = URI.create(uri);
        ImmutableSet.Builder<String> knownProtocols = ImmutableSet.builder();

        for (BrokerFactory factory: ServiceLoader.load(BrokerFactory.class)) {
            Collection<String> factoryProtocols = factory.getProtocols();
            knownProtocols.addAll(factoryProtocols);

            if (factoryProtocols.contains(parsedUri.getScheme())) {
                try {
                    return factory.createBroker(parsedUri, executor);
                } catch (IOException | TimeoutException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        throw new UnsupportedProtocolException(parsedUri.getScheme(), knownProtocols.build());
    }
}
