package com.custom.stocksearcher.constant;

import java.math.BigDecimal;

/**
 * 常數
 */
public class Constant {
    /**
     * 取得股票資訊網址 填入西元年月日 & stockCode
     */
    public static final String STOCK_INFO_URL = "https://www.twse.com.tw/en/exchangeReport/STOCK_DAY?response=json&date=%s&stockNo=%s";

    /**
     * 上櫃歷史股價 填入date ex:2022/03/01
     */
    public static final String TPEx_LIST_URL = "https://www.tpex.org.tw/web/stock/aftertrading/daily_close_quotes/stk_quote_result.php?l=en-us&d=%s";
    /**
     * 取得所有上市公司名稱與股票代號
     */
    public static final String COMPANY_URL = "https://openapi.twse.com.tw/v1/exchangeReport/TWTB4U";
    /**
     * 取得所有上櫃公司名稱與股票代號
     */
    public static final String TPEx_COMPANY_URL = "https://www.tpex.org.tw/openapi/v1/mopsfin_t187ap03_O";
    /**
     * 定義schedule爬蟲起始日期
     */
    public static final String LISTED_STOCK_CRAWLER_BEGIN = "2021-01-01";
    public static final String TPEx_STOCK_CRAWLER_BEGIN = "2021-11-01";
    public static final String STOCK_DATE_FORMAT = "yyyy/MM/dd";

    public static final int WEBCLIENT_LIMIT_RATE = 11;

    /**
     * 上市股票爬蟲參數
     */
    public static final long LISTED_CRAWL_DURATION_MILLS = 3000;
    public static final int LISTED_CRAWL_UPDATE_HOUR = 14;
    /**
     * 上櫃股票爬蟲參數
     */
    public static final long TPEX_CRAWL_DURATION_MILLS = 1000;

}
