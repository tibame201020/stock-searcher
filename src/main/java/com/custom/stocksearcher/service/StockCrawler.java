package com.custom.stocksearcher.service;

import com.custom.stocksearcher.models.CompanyStatus;
import com.custom.stocksearcher.models.listed.ListedStock;
import com.custom.stocksearcher.models.tpex.TPExStock;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;

/**
 * 股價爬蟲Service
 */
public interface StockCrawler {

    Log log = LogFactory.getLog(StockCrawler.class);

    /**
     * 從twse取得股票資訊(上市)
     *
     * @param url 爬蟲網址
     * @return Flux<ListedStock>
     */
    Flux<ListedStock> getListedStockDataFromTWSEApi(String url);

    /**
     * 從twse open api 取得公司名稱與股票代號
     *
     * @return 公司名稱與股票代號列表
     */
    Flux<CompanyStatus> getCompanies();

    /**
     * 從tpex取得股票資料(上櫃)
     *
     * @param url 爬蟲網址
     * @return Flux<TPExStock>
     */
    Flux<TPExStock> getTPExStockFromTPEx(String url);
}
