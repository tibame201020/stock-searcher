package com.custom.stocksearcher.models;

import lombok.*;

import java.io.Serializable;
import java.time.YearMonth;

@ToString
@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
public class CodeWithYearMonth implements Serializable {
    private String code;
    private YearMonth yearMonth;
}
