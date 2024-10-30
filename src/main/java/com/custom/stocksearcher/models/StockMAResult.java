package com.custom.stocksearcher.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * MA股價封裝
 */
@ToString
@NoArgsConstructor
@Getter
@Setter
public class StockMAResult implements Serializable {
    private String code;
    private LocalDate date;
    private BigDecimal ma5;
    private BigDecimal ma10;
    private BigDecimal ma20;
    private BigDecimal ma60;
    private BigDecimal price;
}
