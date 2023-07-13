package com.custom.stocksearcher.models.listed;

import lombok.*;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.time.LocalDate;

@ToString
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ListedStockId implements Serializable {
    @Field(type = FieldType.Keyword)
    private String code;
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd")
    private LocalDate date;
}
