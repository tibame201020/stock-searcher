package com.custom.stocksearcher.models.listed;

import com.custom.stocksearcher.models.StockData;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 上市股票bean
 */
@ToString
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "listed_stock")
public class ListedStock implements Serializable {
    @Id
    private ListedStockId listedStockId;
    private StockData stockData;
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd")
    private LocalDate date;
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd")
    private LocalDate updateDate;
}
