package com.custom.stocksearcher.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@ToString
@NoArgsConstructor
@Getter
@Setter
public class StockBumpy implements Serializable {
    private String code;
    private LocalDate highestDate;
    private BigDecimal highestPrice;
    private LocalDate lowestDate;
    private BigDecimal lowestPrice;
    private BigDecimal calcResult;
}
