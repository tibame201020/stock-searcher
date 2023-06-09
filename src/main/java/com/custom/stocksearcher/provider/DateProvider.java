package com.custom.stocksearcher.provider;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class DateProvider {

    /**
     * 是否為當月
     *
     * @param dateStr 傳入日期
     * @return
     */
    public boolean isThisMonth(String dateStr) {
        LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd")).withDayOfMonth(1);
        return LocalDate.now().withDayOfMonth(1).isEqual(date);
    }

    /**
     * 與傳入日期是否為同一天
     *
     * @param updateDate 傳入日期
     * @return
     */
    public boolean isUpdateDateToday(LocalDate updateDate, AtomicBoolean atomicBoolean) {
        atomicBoolean.set(!LocalDate.now().isEqual(updateDate));
        return LocalDate.now().isEqual(updateDate);
    }

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

        while (!currentMonth.isAfter(YearMonth.from(endDate.minusMonths(1)))) {
            monthList.add(currentMonth);
            currentMonth = currentMonth.plusMonths(1);
        }

        return monthList;
    }
}
