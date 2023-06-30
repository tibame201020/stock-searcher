package com.custom.stocksearcher.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;


@ToString
@NoArgsConstructor
@Getter
@Setter
@Document(indexName = "stock_ma_result")
public class StockMAResult implements Serializable {
    @Id
    private StockMAResultId stockMAResultId;
    @Field(type = FieldType.Keyword)
    private String code;
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd")
    private LocalDate date;
    @Field(type = FieldType.Keyword)
    private BigDecimal ma5;
    @Field(type = FieldType.Keyword)
    private BigDecimal ma10;
    @Field(type = FieldType.Keyword)
    private BigDecimal ma20;
    @Field(type = FieldType.Keyword)
    private BigDecimal ma60;
}
