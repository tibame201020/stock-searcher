package com.custom.stocksearcher.constant;

/**
 * 常數
 */
public class Constant {
    public static final String FRONT_END_URL = "https://ftf7ff-4200.csb.app/";
    public static final String LOCAL_4200 = "http://localhost:4200/";
    public static final String LOCAL_URL = "http://localhost:9218/";
    /**
     * for 接受跨域請求使用
     */
    public static final String[] CORS_URLS = new String[]{FRONT_END_URL, LOCAL_4200, LOCAL_URL};

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
    public static final String STOCK_CRAWLER_BEGIN = "2022-06-01";

    /**
     * K棒判斷常數
     */
    public static final String CandlestickType_BLACK = "BLACK";
    public static final String CandlestickType_RED = "RED";
    public static final String CandlestickType_GREEN = "GREEN";

    public static final String DASH_LINE = "DashLine";
    public static final String T_LINE_UP = "TLineUP";
    public static final String T_LINE_DOWN = "TLineDown";
    public static final String CROSS = "Cross";
    public static final String CROSS_UP = "CrossUp";
    public static final String CROSS_DOWN = "CrossDown";

    /**
     * 上市股票爬蟲參數
     */
    public static final long LISTED_CRAWL_DURATION_MILLS = 3000;
    public static final int LISTED_CRAWL_UPDATE_HOUR = 14;
    public static final String LISTED_DATA_FILE_NAME = "stocksListed";
    /**
     * 上櫃股票爬蟲參數
     */
    public static final long TPEX_CRAWL_DURATION_MILLS = 1000;
    public static final String TPEX_DATA_FILE_NAME = "stocksTPEX";

}
