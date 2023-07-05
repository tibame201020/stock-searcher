package com.custom.stocksearcher.models.tpex;

import com.custom.stocksearcher.models.StockData;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 上櫃公司股價bean
 */
@ToString
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "tpex_stock")
public class TPExStock implements Serializable {
    @Id
    private TPExStockId tpExStockId;
    private StockData stockData;
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd")
    private LocalDate date;
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd")
    private LocalDate updateDate;
}
