package com.geneea.celery.backends;

import com.google.common.collect.ImmutableSet;
import com.geneea.celery.UnsupportedProtocolException;
import com.geneea.celery.spi.Backend;
import com.geneea.celery.spi.BackendFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

/**
 * Internal utility to get the right {@link Backend} instance from a URI.
 */
public class CeleryBackends {

    /**
     * Create a new backend.
     *
     * @param uri connection to backend
     * @param executor for background tasks
     * @return new backend instance
     */
    public static Backend create(String uri, ExecutorService executor) {

        URI parsedUri = URI.create(uri);
        ImmutableSet.Builder<String> knownProtocols = ImmutableSet.builder();

        for (BackendFactory factory: ServiceLoader.load(BackendFactory.class)) {
            Collection<String> factoryProtocols = factory.getProtocols();
            knownProtocols.addAll(factoryProtocols);

            if (factoryProtocols.contains(parsedUri.getScheme())) {
                try {
                    return factory.createBackend(parsedUri, executor);
                } catch (TimeoutException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        throw new UnsupportedProtocolException(parsedUri.getScheme(), knownProtocols.build());
    }
}
