package com.custom.stocksearcher.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;

@Getter
@Setter
@ToString
public class StockDataId {
    @Field(type = FieldType.Keyword)
    private String code;
    @Field(type = FieldType.Date)
    private LocalDate date;
}
