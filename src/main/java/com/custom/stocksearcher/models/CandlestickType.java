package com.custom.stocksearcher.models;

/**
 * 日K型態
 */
public enum CandlestickType {
    BullishLine("大陽線"),
    BullishHighWave("上下影線陽線"),
    BullishUpperShadow("上影線陽線"),
    BullishLowerShadow("下影線陽線"),
    BullishHammer("陽線錘"),
    BullishUpperHammer("上陽線錘"),
    BullishLowerHammer("下陽線錘"),

    BearishLine("大陰線"),
    BearishHighWave("上下影線陰線"),
    BearishUpperShadow("上影線陰線"),
    BearishLowerShadow("下影線陰線"),
    BearishHammer("陰線錘"),
    BearishUpperHammer("上陰線錘"),
    BearishLowerHammer("下陰線錘"),

    CrossLine("十字線"),
    CrossLineUp("十字線上影線較長"),
    CrossLineDown("十字線下影線較長"),
    DashLine("一字線"),
    TLine("T字線"),
    InvertedTLine("倒T字線"),

    UnknownType("未定義型態");

    private String name;
    CandlestickType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
