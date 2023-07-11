package com.custom.stocksearcher.service;

import com.custom.stocksearcher.models.CandlestickType;
import com.custom.stocksearcher.models.StockData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public interface StockCandlestick {
    Log log = LogFactory.getLog(StockCandlestick.class);

    /**
     * 判斷日K型態
     * @param stockData 股市資料(日)
     * @return CandlestickType
     */
    CandlestickType detectCandlestickType(StockData stockData);
}
