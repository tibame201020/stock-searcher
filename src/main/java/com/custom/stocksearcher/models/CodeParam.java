package com.custom.stocksearcher.models;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

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
}
