package com.custom.stocksearcher.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * 對應twse回傳歷史股價bean
 */
@ToString
@NoArgsConstructor
@Getter
@Setter
public class StockBasicInfo implements Serializable {
    @JsonProperty(value = "status")
    private String status;

    @JsonProperty(value = "date")
    private String date;

    @JsonProperty(value = "title")
    private String title;

    @JsonProperty(value = "fields")
    private String[] fields;

    @JsonProperty(value = "data")
    private String[][] data;

    @JsonProperty(value = "notes")
    private String[] notes;
}
