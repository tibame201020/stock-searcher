package com.custom.stocksearcher.service.impl;

import com.custom.stocksearcher.models.*;
import com.custom.stocksearcher.provider.DateProvider;
import com.custom.stocksearcher.provider.WebProvider;
import com.custom.stocksearcher.repo.CompanyStatusRepo;
import com.custom.stocksearcher.repo.StockMonthDataRepo;
import com.custom.stocksearcher.service.StockCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static com.custom.stocksearcher.constant.Constant.COMPANY_URL;
import static com.custom.stocksearcher.constant.Constant.STOCK_INFO_URL;

@Service
public class StockCrawlerImpl implements StockCrawler {

    @Autowired
    private WebProvider webProvider;
    @Autowired
    private DateProvider dateProvider;
    @Autowired
    private StockMonthDataRepo stockMonthDataRepo;
    @Autowired
    private CompanyStatusRepo companyStatusRepo;

    @Override
    public Mono<StockMonthData> getStockMonthDataFromTWSEApi(String stockCode, String dateStr) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\n===============================================")
                .append("\n")
                .append("從網路取得股價資料").append("\n")
                .append("代號 :").append(stockCode).append("\n")
                .append("時間 :").append(dateStr);

        log.info(stringBuilder);
        String url = String.format(STOCK_INFO_URL, dateStr, stockCode);
        StockBasicInfo stockBasicInfo = webProvider.getUrlToObject(url, StockBasicInfo.class);
        if (null == stockBasicInfo || null == stockBasicInfo.getData()) {
            return Mono.empty();
        }
        StockMonthData stockMonthData = transStockMonthData(stockBasicInfo.getData(), stockCode);

        return stockMonthDataRepo.save(stockMonthData);
    }

    @Override
    public Flux<CompanyStatus> getCompanies() {
        log.info("===============================================\n從網路取得資料公司資料");
        CompanyStatus[] companies = new RestTemplate().getForObject(COMPANY_URL, CompanyStatus[].class);

        assert companies != null;
        return companyStatusRepo.saveAll(Flux.fromArray(companies));
    }

    private StockMonthData transStockMonthData(String[][] data, String code) {
        String yearMonth = YearMonth.from(LocalDate.parse(data[0][0].replace("/", "-"))).toString();
        boolean isThisMonth = dateProvider.isThisMonth(data[0][0].replace("/", ""));

        StockMonthDataId stockMonthDataId = new StockMonthDataId();
        stockMonthDataId.setCode(code);
        stockMonthDataId.setYearMonth(yearMonth);

        StockMonthData stockMonthData = new StockMonthData();
        stockMonthData.setStockMonthDataId(stockMonthDataId);
        stockMonthData.setCode(code);
        stockMonthData.setYearMonth(yearMonth);
        stockMonthData.setHistory(!isThisMonth);
        stockMonthData.setUpdateDate(LocalDate.now());

        List<StockData> stockDataList = new ArrayList<>();
        for (String[] dataInfo : data) {
            StockData stockData = translateStockData(dataInfo, code);
            stockDataList.add(stockData);
        }
        stockMonthData.setStockDataList(stockDataList);

        return stockMonthData;
    }

    private StockData translateStockData(String[] dataInfo, String code) {
        StockData stockData = new StockData();

        stockData.setDate(LocalDate.parse(dataInfo[0].replace("/", "-")));
        stockData.setTradeVolume(dataInfo[1]);
        stockData.setTradeValue(dataInfo[2]);
        stockData.setOpeningPrice(dataInfo[3]);
        stockData.setHighestPrice(dataInfo[4]);
        stockData.setLowestPrice(dataInfo[5]);
        stockData.setClosingPrice(dataInfo[6]);
        stockData.setChange(dataInfo[7]);
        stockData.setTransaction(dataInfo[8]);

        return stockData;
    }
}
