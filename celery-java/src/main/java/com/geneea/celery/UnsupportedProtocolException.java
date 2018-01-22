package com.geneea.celery;

import com.google.common.base.Joiner;

import java.text.MessageFormat;
import java.util.Collection;

/**
 * Thrown when the caller can't process the URI because it can't handle its protocol (scheme).
 */
public class UnsupportedProtocolException extends RuntimeException {
    private final String protocol;
    private final Collection<String> supportedProtocols;

    public UnsupportedProtocolException(String protocol, Collection<String> supportedProtocols) {
        this.protocol = protocol;
        this.supportedProtocols = supportedProtocols;
    }

    @Override
    public String getMessage() {
        return MessageFormat.format("Unsupported protocol: {0}. Supported protocols are: {1}",
                protocol, Joiner.on(", ").join(supportedProtocols));
    }
}
