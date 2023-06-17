package com.custom.stocksearcher.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;


/**
 * 股價資訊(日)bean
 */
@ToString
@NoArgsConstructor
@Getter
@Setter
public class StockData implements Serializable {
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd")
    private LocalDate date;
    @Field(type = FieldType.Keyword)
    private BigDecimal tradeVolume;
    @Field(type = FieldType.Keyword)
    private BigDecimal tradeValue;
    @Field(type = FieldType.Keyword)
    private BigDecimal openingPrice;
    @Field(type = FieldType.Keyword)
    private BigDecimal highestPrice;
    @Field(type = FieldType.Keyword)
    private BigDecimal lowestPrice;
    @Field(type = FieldType.Keyword)
    private BigDecimal closingPrice;
    @Field(type = FieldType.Keyword)
    private BigDecimal change;
    @Field(type = FieldType.Keyword)
    private BigDecimal transaction;
}