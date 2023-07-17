package com.custom.stocksearcher.service;

import com.custom.stocksearcher.models.CodeList;
import com.custom.stocksearcher.models.CodeParam;
import com.custom.stocksearcher.models.CompanyStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * user codeList service
 */
public interface UserStorage {
    Log log = LogFactory.getLog(UserStorage.class);

    /**
     * saveCodeList
     *
     * @param codeList 使用者codeList
     * @return Flux<CodeList>
     */
    Flux<CodeList> saveCodeList(CodeList codeList);

    /**
     * 取得歸屬於用戶的codeList
     *
     * @param user 用戶名
     * @return Flux<CodeList>
     */
    Flux<CodeList> getCodeListByUser(String user);

    /**
     * 取得CodeList交集
     *
     * @param codeListIds
     * @return 交集結果
     */
    Flux<CompanyStatus> getIntersectionFromCodeList(List<String> codeListIds);

    /**
     * 取得篩選範圍
     *
     * @param key            keyword
     * @param without4upCode without4upCode
     * @return Flux<CompanyStatus>
     */
    Flux<CompanyStatus> getCodeRange(String key, boolean without4upCode);

    /**
     * 根據範圍產生實際要找的codeParam
     *
     * @param companyStatusFlux 範圍
     * @param codeParam         原有條件
     * @return Flux<CodeParam>
     */
    Flux<CodeParam> wrapperCodeParam(Flux<CompanyStatus> companyStatusFlux, CodeParam codeParam);
}
