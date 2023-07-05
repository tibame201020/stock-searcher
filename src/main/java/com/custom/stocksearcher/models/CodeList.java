package com.custom.stocksearcher.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;
import java.util.List;

/**
 * 封裝CodeList
 * 便於儲存篩選結果
 */
@ToString
@NoArgsConstructor
@Getter
@Setter
@Document(indexName = "code_list")
public class CodeList {
    @Id
    @Field(type = FieldType.Keyword)
    private String codeListId;
    @Field(type = FieldType.Keyword)
    private String name;
    @Field(type = FieldType.Keyword)
    private String user;
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd")
    private LocalDate date;
    @Field(type = FieldType.Nested)
    private List<CompanyStatus> codes;
}
