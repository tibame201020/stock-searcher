package com.custom.stocksearcher.util;

import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;

import java.util.function.Supplier;

public class ElasticSearchUtil {

    public static Supplier<Query> supplier() {
        Supplier<Query> supplier = () -> Query.of(q -> q.matchAll(matchAllQuery()));
        return supplier;
    }

    public static MatchAllQuery matchAllQuery() {
        return new MatchAllQuery.Builder().build();
    }
}
