package com.geneea.celery.spi;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.Closeable;
import java.io.IOException;

/**
 * <i>Internal, used by {@link com.geneea.celery.Celery} and implemented by backend providers.</i>
 *
 * <p>
 *     Backend, in Celery terminology, is a way to deliver task results back to the client.
 * </p>
 */
public interface Backend extends Closeable {

    /**
     * The client ({@link com.geneea.celery.Celery}) uses this method to subscribe to results of the tasks it sends.
     *
     * @param clientId your unique client ID
     * @return results provider returning task results
     * @throws IOException signalizes connection problem
     */
    ResultsProvider resultsProviderFor(String clientId) throws IOException;

    /**
     * Report successful result of computation back to the client.
     *
     * @param taskId unique task ID as received
     * @param queue which queue to report the result to (usually the client ID)
     * @param correlationId correlation ID as received
     * @param result the computation result (needs to be JSON serializable)
     * @throws IOException in case of connection problem
     */
    void reportResult(String taskId, String queue, String correlationId, Object result) throws IOException;

    /**
     * Report erroneous result of computation back to the client.
     *
     * @param taskId unique task ID as received
     * @param queue which queue to report the result to (usually the client ID)
     * @param correlationId correlation ID as received
     * @param exception description of the problem
     * @throws IOException in case of connection problem
     */
    void reportException(String taskId, String queue, String correlationId, Throwable exception) throws IOException;

    /**
     * A way to get notified about completion of the tasks.
     */
    interface ResultsProvider {
        /**
         * @param taskId unique ID of the task, as used in {@link Message.Headers#setId(String)}
         * @return the computation result that completes when the result is retrieved from the queue
         */
        ListenableFuture<Object> getResult(String taskId);
    }
}
