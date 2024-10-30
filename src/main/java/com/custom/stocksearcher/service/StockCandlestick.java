package com.custom.stocksearcher.service;

import com.custom.stocksearcher.models.CandlestickType;
import com.custom.stocksearcher.models.StockData;

public interface StockCandlestick {
    /**
     * 判斷日K型態
     *
     * @param stockData 股市資料(日)
     * @return CandlestickType
     */
    CandlestickType detectCandlestickType(StockData stockData);
}
