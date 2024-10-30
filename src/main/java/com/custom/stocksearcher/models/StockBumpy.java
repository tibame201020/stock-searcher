package com.custom.stocksearcher.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 計算結果封裝bean
 */
@ToString
@NoArgsConstructor
@Getter
@Setter
public class StockBumpy implements Serializable {
    private String code;
    private String name;
    private String beginDate;
    private String endDate;

    private LocalDate highestDate;
    private BigDecimal highestPrice;

    private LocalDate lowestDate;
    private BigDecimal lowestPrice;

    private LocalDate lowestTradeVolumeDate;
    private BigDecimal lowestTradeVolume;

    private BigDecimal calcResult;

    private StockMAResult lastStockMA;
}
