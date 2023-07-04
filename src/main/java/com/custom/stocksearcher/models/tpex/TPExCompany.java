package com.custom.stocksearcher.models.tpex;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.io.Serializable;

@ToString
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TPExCompany implements Serializable {
    @JsonProperty(value = "SecuritiesCompanyCode")
    private String code;
    @JsonProperty(value = "CompanyName")
    private String name;
}
