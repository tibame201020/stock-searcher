package com.custom.stocksearcher.constant;

/**
 * 常數
 */
public class Constant {
    public static final String FRONT_END_URL = "https://ftf7ff-4200.csb.app/";
    public static final String LOCAL_4200 = "http://localhost:4200/";
    public static final String LOCAL_URL = "http://localhost:9218/";

    public static final String[] CORS_URLS = new String[]{FRONT_END_URL, LOCAL_4200, LOCAL_URL};

    /**
     * 取得股票資訊網址 填入西元年月日 & stockCode
     */
    public static final String STOCK_INFO_URL = "https://www.twse.com.tw/en/exchangeReport/STOCK_DAY?response=json&date=%s&stockNo=%s";
    /**
     * 取得所有公司名稱與股票代號
     */
    public static final String COMPANY_URL = "https://openapi.twse.com.tw/v1/exchangeReport/TWTB4U";

    /**
     * 定義schedule爬蟲起始日期
     */
    public static final String STOCK_CRAWLER_BEGIN = "2022-06-01";
    /**
     * 定義schedule爬蟲結束日期
     */
    public static final String STOCK_CRAWLER_END = "2023-06-30";

    /**
     * 開發期間預設user
     */
    public static final String DEV_USER = "dev-user";
}
