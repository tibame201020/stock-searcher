package com.custom.stocksearcher.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
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
    private String tradeVolume;
    @Field(type = FieldType.Keyword)
    private String tradeValue;
    @Field(type = FieldType.Keyword)
    private String openingPrice;
    @Field(type = FieldType.Keyword)
    private String highestPrice;
    @Field(type = FieldType.Keyword)
    private String lowestPrice;
    @Field(type = FieldType.Keyword)
    private String closingPrice;
    @Field(type = FieldType.Keyword)
    private String change;
    @Field(type = FieldType.Keyword)
    private String transaction;
}