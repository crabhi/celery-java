package org.sedlakovi.celery.spi;


import java.io.IOException;

public interface Message {

    void setBody(byte[] body);

    void setContentEncoding(String contentEncoding);

    void setContentType(String contentType);

    Headers getHeaders();

    void send(String queue) throws IOException;

    interface Headers {

        void setId(String id);

        void setArgsRepr(String argsRepr);

        void setOrigin(String origin);

        void setReplyTo(String clientId);

        void setTaskName(String task);
    }
}
