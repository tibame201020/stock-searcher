package com.custom.stocksearcher.models.tpex;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * 對應TPEX回傳上櫃股價資訊bean
 */
@ToString
@NoArgsConstructor
@Getter
@Setter
public class TPExUrlObject implements Serializable {

    @JsonProperty(value = "reportDate")
    private String reportDate;
    @JsonProperty(value = "aaData")
    private String[][] aaData;
}
