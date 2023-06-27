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
public class StockMAResult implements Serializable {
    private String code;
    private LocalDate date;
    private Integer maLevel;
    private BigDecimal maValue;
}
