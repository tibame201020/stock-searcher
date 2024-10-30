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
import java.util.Objects;

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
    @Field(type = FieldType.Boolean)
    private boolean isTPE = false;
    /**
     * 每日更新一次
     */
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd")
    private LocalDate updateDate = LocalDate.now();

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        CompanyStatus other = (CompanyStatus) obj;

        return Objects.equals(code, other.code) &&
                Objects.equals(name, other.name);
    }

}
