package com.custom.stocksearcher.models.tpex;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

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
