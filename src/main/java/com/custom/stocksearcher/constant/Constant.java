package com.custom.stocksearcher.constant;

import java.math.BigDecimal;

/**
 * 常數
 */
public class Constant {
    public static final String FRONT_END_URL = "https://ftf7ff-4200.csb.app/";
    public static final String LOCAL_4200 = "http://localhost:4200/";
    public static final String LOCAL_URL = "http://localhost:9218/";
    public static final String LOCAL_9218_URL = "http://127.0.0.1:9218/";
    /**
     * for 接受跨域請求使用
     */
    public static final String[] CORS_URLS = new String[]{FRONT_END_URL, LOCAL_4200, LOCAL_URL, LOCAL_9218_URL};
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
     * K棒判斷參數
     */
    public static final BigDecimal HAMMER_LIMIT = BigDecimal.valueOf(1.2);
    public static final BigDecimal CYLINDER_LIMIT = BigDecimal.valueOf(0.2);

}
