package com.custom.stocksearcher.service.impl;

import com.custom.stocksearcher.models.CodeList;
import com.custom.stocksearcher.repo.CodeListRepo;
import com.custom.stocksearcher.service.UserStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Service
public class UserStorageImpl implements UserStorage {
    @Autowired
    private CodeListRepo codeListRepo;

    @Override
    public Mono<CodeList> saveCodeList(CodeList codeList) {
        codeList.setDate(LocalDate.now());
        return codeListRepo.save(codeList);
    }

    @Override
    public Flux<CodeList> getCodeListByUser(String user) {
        return codeListRepo.findByUser(user);
    }
}
