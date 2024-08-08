package com.custom.stocksearcher.models;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 查詢參數bean
 */
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CodeParam implements Serializable {
    /**
     * 股票代號
     */
    private String code;
    /**
     * 起始日期
     */
    private String beginDate;
    /**
     * 結束日期
     */
    private String endDate;

    private BigDecimal bumpyHighLimit;

    private BigDecimal bumpyLowLimit;

    private BigDecimal tradeVolumeLimit;

    private Integer beforeEndDateDays;

    private Integer klineCnt;

    private BigDecimal lastOpenCalcLimit;
    private BigDecimal lastCloseCalcLimit;

    /**
     * 收盤價要高於季線條件
     */
    private String closingPriceCompareTargetHigher;
    /**
     * 收盤價要高於低於條件
     */
    private String closingPriceCompareTargetLower;

    private List<String> candlestickTypeList;

    private boolean without4upCode;
    private BigDecimal priceLowLimit;
    private BigDecimal priceHighLimit;
}
