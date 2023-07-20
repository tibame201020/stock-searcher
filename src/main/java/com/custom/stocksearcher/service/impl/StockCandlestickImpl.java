package com.custom.stocksearcher.service.impl;

import com.custom.stocksearcher.models.CandlestickType;
import com.custom.stocksearcher.models.StockData;
import com.custom.stocksearcher.service.StockCandlestick;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.custom.stocksearcher.constant.Constant.*;

@Service
public class StockCandlestickImpl implements StockCandlestick {

    @Override
    public CandlestickType detectCandlestickType(StockData stockData) {
        BigDecimal openingPrice = stockData.getOpeningPrice();
        BigDecimal closingPrice = stockData.getClosingPrice();
        BigDecimal highestPrice = stockData.getHighestPrice();
        BigDecimal lowestPrice = stockData.getLowestPrice();

        BigDecimal body = openingPrice.subtract(closingPrice);
        String colorType = body.compareTo(BigDecimal.ZERO) == 0 ?
                CandlestickType_BLACK : body.compareTo(BigDecimal.ZERO) > 0 ?
                CandlestickType_GREEN : CandlestickType_RED;

        BigDecimal bodyLine = body.abs();
        BigDecimal upperShadowLine = highestPrice.subtract(openingPrice.max(closingPrice));
        BigDecimal lowerShadowLine = openingPrice.min(closingPrice).subtract(lowestPrice);

        CandlestickType candlestickType = CandlestickType.UnknownType;
        switch (colorType) {
            case CandlestickType_BLACK -> candlestickType = blackCharge(upperShadowLine, lowerShadowLine);
            case CandlestickType_RED -> candlestickType = redCharge(bodyLine, upperShadowLine, lowerShadowLine);
            case CandlestickType_GREEN -> candlestickType = greenCharge(bodyLine, upperShadowLine, lowerShadowLine);
        }

        if (CandlestickType.UnknownType.getName().equalsIgnoreCase(candlestickType.getName())) {
            log.info("candlestickType = " + candlestickType.getName());
        }

        return candlestickType;
    }

    private CandlestickType blackCharge(BigDecimal upperShadowLine, BigDecimal lowerShadowLine) {
        String shadowLine = chargeShadowLine(upperShadowLine, lowerShadowLine);

        CandlestickType candlestickType = CandlestickType.UnknownType;
        switch (shadowLine) {
            case DASH_LINE -> candlestickType = CandlestickType.DashLine;
            case T_LINE_UP -> candlestickType = CandlestickType.InvertedTLine;
            case T_LINE_DOWN -> candlestickType = CandlestickType.TLine;
            case CROSS -> candlestickType = CandlestickType.CrossLine;
            case CROSS_UP -> candlestickType = CandlestickType.CrossLineUp;
            case CROSS_DOWN -> candlestickType = CandlestickType.CrossLineDown;
        }

        return candlestickType;
    }

    private CandlestickType redCharge(BigDecimal bodyLine, BigDecimal upperShadowLine, BigDecimal lowerShadowLine) {
        BigDecimal bodyShadowRatio = upperShadowLine.max(lowerShadowLine).divide(bodyLine, 4, RoundingMode.FLOOR);

        boolean isShadowLineSameLong = upperShadowLine.compareTo(lowerShadowLine) == 0;
        boolean isUpperShadowLineLong = upperShadowLine.compareTo(lowerShadowLine) > 0;
        boolean isHammer = bodyShadowRatio.compareTo(HAMMER_LIMIT) > 0;
        boolean isCylinder = bodyShadowRatio.compareTo(CYLINDER_LIMIT) > 0;

        if (isHammer) {
            if (isShadowLineSameLong) {
                return CandlestickType.BullishHammer;
            } else if (isUpperShadowLineLong) {
                return CandlestickType.BullishUpperHammer;
            } else {
                return CandlestickType.BullishLowerHammer;
            }
        } else if (isCylinder) {
            if (isShadowLineSameLong) {
                return CandlestickType.BullishHighWave;
            } else if (isUpperShadowLineLong) {
                return CandlestickType.BullishUpperShadow;
            } else {
                return CandlestickType.BullishLowerShadow;
            }
        } else {
            return CandlestickType.BullishLine;
        }
    }

    private CandlestickType greenCharge(BigDecimal bodyLine, BigDecimal upperShadowLine, BigDecimal lowerShadowLine) {
        BigDecimal bodyShadowRatio = upperShadowLine.max(lowerShadowLine).divide(bodyLine, 4, RoundingMode.FLOOR);

        boolean isShadowLineSameLong = upperShadowLine.compareTo(lowerShadowLine) == 0;
        boolean isUpperShadowLineLong = upperShadowLine.compareTo(lowerShadowLine) > 0;
        boolean isHammer = bodyShadowRatio.compareTo(HAMMER_LIMIT) > 0;
        boolean isCylinder = bodyShadowRatio.compareTo(CYLINDER_LIMIT) > 0;

        if (isHammer) {
            if (isShadowLineSameLong) {
                return CandlestickType.BearishHammer;
            } else if (isUpperShadowLineLong) {
                return CandlestickType.BearishUpperHammer;
            } else {
                return CandlestickType.BearishLowerHammer;
            }
        } else if (isCylinder) {
            if (isShadowLineSameLong) {
                return CandlestickType.BearishHighWave;
            } else if (isUpperShadowLineLong) {
                return CandlestickType.BearishUpperShadow;
            } else {
                return CandlestickType.BearishLowerShadow;
            }
        } else {
            return CandlestickType.BearishLine;
        }
    }

    private String chargeShadowLine(BigDecimal upperShadowLine, BigDecimal lowerShadowLine) {
        boolean upperShadowLineZero = upperShadowLine.compareTo(BigDecimal.ZERO) == 0;
        boolean lowerShadowLineZero = lowerShadowLine.compareTo(BigDecimal.ZERO) == 0;
        boolean shadowLineSameLong = upperShadowLine.compareTo(lowerShadowLine) == 0;
        boolean upperShadowLineLonger = upperShadowLine.compareTo(lowerShadowLine) > 0;

        if (upperShadowLineZero) {
            if (lowerShadowLineZero) {
                return DASH_LINE;
            } else {
                return T_LINE_DOWN;
            }

        } else if (lowerShadowLineZero) {
            return T_LINE_UP;
        } else {
            if (shadowLineSameLong) {
                return CROSS;
            } else {
                if (upperShadowLineLonger) {
                    return CROSS_UP;
                } else {
                    return CROSS_DOWN;
                }
            }
        }
    }


}
