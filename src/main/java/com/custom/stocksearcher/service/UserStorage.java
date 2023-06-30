package com.custom.stocksearcher.service;

import com.custom.stocksearcher.models.CodeList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserStorage {
    Log log = LogFactory.getLog(UserStorage.class);

    Mono<CodeList> saveCodeList(CodeList codeList);

    Flux<CodeList> getCodeListByUser(String user);
}
