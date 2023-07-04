package com.custom.stocksearcher.models.tpex;

import lombok.*;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;

@ToString
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TPExStockId {
    @Field(type = FieldType.Keyword)
    private String code;
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd")
    private LocalDate date;
}
