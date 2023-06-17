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
import java.time.LocalDate;
import java.util.List;

/**
 * 股價資訊(月)bean
 */
@ToString
@NoArgsConstructor
@Getter
@Setter
@Document(indexName = "stock_month_data")
public class StockMonthData implements Serializable {
    @Id
    private StockMonthDataId stockMonthDataId;
    @Field(type = FieldType.Keyword)
    private String code;
    @Field(type = FieldType.Keyword)
    private String yearMonth;
    @Field(type = FieldType.Boolean)
    private boolean isHistory;
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd")
    private LocalDate updateDate;
    @Field(type = FieldType.Nested)
    private List<StockData> stockDataList;
}
