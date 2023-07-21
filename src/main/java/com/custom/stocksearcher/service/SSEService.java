package com.custom.stocksearcher.service;

import reactor.core.publisher.Sinks;
import org.apache.commons.logging.Log;

public interface SSEService {
    void pushLog(String logMessage, Log log);
    Sinks.Many<String> getSink();
}
