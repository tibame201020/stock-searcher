package com.custom.stocksearcher.service;

import com.custom.stocksearcher.models.CodeList;
import com.custom.stocksearcher.models.CompanyStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;

import java.util.List;

public interface UserStorage {
    Log log = LogFactory.getLog(UserStorage.class);

    Flux<CodeList> saveCodeList(CodeList codeList);

    Flux<CodeList> getCodeListByUser(String user);

    Flux<CompanyStatus> getIntersectionFromCodeList(List<String> codeListIds);
}
