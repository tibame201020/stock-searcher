package com.custom.stocksearcher.models;

import lombok.*;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;


@ToString
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class StockMonthDataId implements Serializable {
    @Field(type = FieldType.Keyword)
    private String code;
    @Field(type = FieldType.Keyword)
    private String yearMonth;
}
