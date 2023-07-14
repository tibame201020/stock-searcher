package com.custom.stocksearcher.service.impl;

import com.custom.stocksearcher.models.CodeList;
import com.custom.stocksearcher.models.CodeParam;
import com.custom.stocksearcher.models.CompanyStatus;
import com.custom.stocksearcher.repo.CodeListRepo;
import com.custom.stocksearcher.repo.CompanyStatusRepo;
import com.custom.stocksearcher.service.UserStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

@Service
public class UserStorageImpl implements UserStorage {
    @Autowired
    private CodeListRepo codeListRepo;
    @Autowired
    private CompanyStatusRepo companyStatusRepo;

    @Override
    public Flux<CodeList> saveCodeList(CodeList codeList) {
        return Flux.fromIterable(codeList.getCodes())
                .flatMap(companyStatus -> {
                    if (null == companyStatus.getName() || companyStatus.getName().isEmpty()) {
                        return companyStatusRepo.findById(companyStatus.getCode());
                    } else {
                        return Mono.just(companyStatus);
                    }
                })
                .buffer()
                .flatMap(companyStatuses -> {
                    codeList.setDate(LocalDate.now());
                    codeList.setCodes(companyStatuses);

                    return codeListRepo.save(codeList);
                });
    }

    @Override
    public Flux<CodeList> getCodeListByUser(String user) {
        return codeListRepo.findByUser(user);
    }

    @Override
    public Flux<CompanyStatus> getIntersectionFromCodeList(List<String> codeListIds) {


        Flux<CompanyStatus> companyStatusAllFlux = codeListRepo.findAllById(codeListIds)
                .flatMap(codeList -> Flux.fromIterable(codeList.getCodes())).distinct(CompanyStatus::getCode);

        for (String codeListId : codeListIds) {
            Flux<CompanyStatus> companyStatusFlux = codeListRepo.findById(codeListId).flux().flatMap(codeList -> Flux.fromIterable(codeList.getCodes()));
            companyStatusAllFlux = companyStatusAllFlux.flatMap(
                    companyStatus -> companyStatusFlux.flatMap(companyStatus1 -> {
                        if (companyStatus1.equals(companyStatus)) {
                            return Flux.just(companyStatus);
                        } else {
                            return Flux.empty();
                        }
                    })
            );
            companyStatusAllFlux = Flux.from(companyStatusAllFlux);
        }

        return companyStatusAllFlux;
    }

    @Override
    public Flux<CompanyStatus> getCodeRange(String key) {
        switch (key) {
            case "all" -> {
                return companyStatusRepo.findAll();
            }
            case "listed" -> {
                return companyStatusRepo.findAll().filter(companyStatus -> !companyStatus.isTPE());
            }
            case "tpex" -> {
                return companyStatusRepo.findAll().filter(CompanyStatus::isTPE);
            }
            default -> {
                return codeListRepo
                        .findById(key)
                        .flux()
                        .flatMap(codeList -> Flux.fromIterable(codeList.getCodes()));
            }
        }
    }

    @Override
    public Flux<CodeParam> wrapperCodeParam(Flux<CompanyStatus> companyStatusFlux, CodeParam codeParam) {
        return companyStatusFlux.flatMap(
                companyStatus -> {
                    CodeParam actualCodeParam = new CodeParam();

                    actualCodeParam.setCode(companyStatus.getCode());
                    actualCodeParam.setBeginDate(codeParam.getBeginDate());
                    actualCodeParam.setEndDate(codeParam.getEndDate());
                    actualCodeParam.setTradeVolumeLimit(codeParam.getTradeVolumeLimit());
                    actualCodeParam.setKlineCnt(codeParam.getKlineCnt());
                    actualCodeParam.setLastOpenCalcLimit(codeParam.getLastOpenCalcLimit());
                    actualCodeParam.setLastCloseCalcLimit(codeParam.getLastCloseCalcLimit());
                    actualCodeParam.setClosingPriceCompareTarget(codeParam.getClosingPriceCompareTarget());
                    actualCodeParam.setCandlestickTypeList(codeParam.getCandlestickTypeList());

                    return Mono.just(actualCodeParam);
                }
        );
    }
}
