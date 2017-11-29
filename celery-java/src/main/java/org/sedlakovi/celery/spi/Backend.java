package org.sedlakovi.celery.spi;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;

public interface Backend extends AutoCloseable {

    ResultsProvider resultsProviderFor(String clientId) throws IOException;

    void reportResult(String taskId, String queue, String correlationId, Object result) throws IOException;

    void reportException(String taskId, String queue, String correlationId, Throwable exception) throws IOException;

    interface ResultsProvider {
        ListenableFuture<?> getResult(String taskId);
    }
}
