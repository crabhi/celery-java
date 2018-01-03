package org.sedlakovi.celery.brokers;

import org.sedlakovi.celery.spi.Broker;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

public interface BrokerFactory {
    List<String> getProtocols();

    Broker createBroker(String uri, ExecutorService executor) throws URISyntaxException, IOException, TimeoutException;
}
