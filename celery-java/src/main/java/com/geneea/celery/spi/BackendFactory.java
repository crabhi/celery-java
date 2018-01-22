package com.geneea.celery.spi;

import java.io.IOException;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

/**
 * Pluggable interface for backends. Implement it as an entry point if you provide your own {@link Backend}.
 */
public interface BackendFactory {

    /**
     * @return protocols this factory supports
     */
    Set<String> getProtocols();

    /**
     * Instantiate a backend. The {@link URI} scheme will be one of the set you provided via {@link #getProtocols()}.
     *
     * @param uri how to connect to the backend
     * @param executor for background tasks
     * @return new backend instance
     *
     * @throws IOException general IO problem
     * @throws TimeoutException if the connection times out
     */
    Backend createBackend(URI uri, ExecutorService executor) throws IOException, TimeoutException;
}
