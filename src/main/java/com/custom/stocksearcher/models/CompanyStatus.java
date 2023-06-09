package com.custom.stocksearcher.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 公司股票代號bean
 */
@ToString
@NoArgsConstructor
@Getter
@Setter
@Document(indexName = "company_status")
public class CompanyStatus implements Serializable {
    @Id
    @Field(type = FieldType.Keyword)
    @JsonProperty(value = "Code")
    private String code;
    @Field(type = FieldType.Text)
    @JsonProperty(value = "Name")
    private String name;
    /**
     * 用於標記是否已經爬過資料
     */
    @Field(type = FieldType.Boolean)
    private boolean wasCrawler = false;
    /**
     * 每日更新一次
     */
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd")
    private LocalDate updateDate = LocalDate.now();

}
