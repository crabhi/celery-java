package org.sedlakovi.celery.spi;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;

public interface Backend {

    ResultsProvider resultsProviderFor(String clientId) throws IOException;

    interface ResultsProvider {
        ListenableFuture<?> getResult(String taskId);
    }
}
