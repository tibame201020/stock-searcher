package com.custom.stocksearcher.provider;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 日期 util
 */
@Component
public class DateProvider {


    /**
     * 取得開始與結束的時間List
     *
     * @param beginDate 開始日期
     * @param endDate   結束日期
     * @return List<YearMonth>
     */
    public List<YearMonth> calculateMonthList(LocalDate beginDate, LocalDate endDate) {
        List<YearMonth> monthList = new ArrayList<>();
        YearMonth currentMonth = YearMonth.from(beginDate);

        while (!currentMonth.isAfter(YearMonth.from(endDate))) {
            monthList.add(currentMonth);
            currentMonth = currentMonth.plusMonths(1);
        }

        return monthList;
    }

    /**
     * 取得系統日期
     *
     * @return 系統日期
     */
    public String getSystemDateTimeFormat() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());

        return now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public LocalDate strToLocalDate(String dateStr, String format) {
        return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(format));
    }

    public String localDateToString(LocalDate localDate, String format) {
        return localDate.format(DateTimeFormatter.ofPattern(format));
    }

}
