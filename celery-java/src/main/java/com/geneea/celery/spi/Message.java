package com.geneea.celery.spi;


import java.io.IOException;

/**
 * A message (most often a unit of work) to be sent to the queue.
 */
public interface Message {

    /**
     * @param body payload
     */
    void setBody(byte[] body);

    /**
     * @param contentEncoding encoding of the body
     */
    void setContentEncoding(String contentEncoding);

    /**
     * @param contentType MIME type of the body
     */
    void setContentType(String contentType);

    /**
     * @return message headers builder
     */
    Headers getHeaders();

    /**
     * Once ready, you can send the message.
     *
     * @param queue into which queue to send the message
     * @throws IOException in case of connection problem
     */
    void send(String queue) throws IOException;

    /**
     * Message headers to be set
     */
    interface Headers {

        /**
         * @param id unique message id
         */
        void setId(String id);

        /**
         * @param argsRepr readable representation of message arguments for debug purposes
         */
        void setArgsRepr(String argsRepr);

        /**
         * @param origin name of the client sending the message
         */
        void setOrigin(String origin);

        /**
         * @param clientId unique client ID, the same as is used in {@link Backend#resultsProviderFor(String)}
         */
        void setReplyTo(String clientId);

        /**
         * @param task name of the task to be executed (worker looks for a function/class by this name)
         */
        void setTaskName(String task);
    }
}
