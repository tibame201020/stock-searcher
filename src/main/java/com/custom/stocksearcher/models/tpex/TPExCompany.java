package com.custom.stocksearcher.models.tpex;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.io.Serializable;

/**
 * 上櫃公司資訊
 */
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
