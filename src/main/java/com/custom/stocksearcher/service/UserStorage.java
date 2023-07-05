package com.custom.stocksearcher.service;

import com.custom.stocksearcher.models.CodeList;
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
}
