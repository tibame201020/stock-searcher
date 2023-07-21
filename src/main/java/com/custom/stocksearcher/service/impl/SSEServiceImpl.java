package com.custom.stocksearcher.service.impl;

import com.custom.stocksearcher.service.SSEService;
import org.apache.commons.logging.Log;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

@Service
public class SSEServiceImpl implements SSEService {
    private final Sinks.Many<String> sink;

    public SSEServiceImpl() {
        this.sink = Sinks.many().multicast().onBackpressureBuffer();
    }

    @Override
    public void pushLog(String logMessage, Log log) {
        log.info(logMessage);
        sink.tryEmitNext(logMessage);
    }

    @Override
    public Sinks.Many<String> getSink() {
        return sink;
    }
}
