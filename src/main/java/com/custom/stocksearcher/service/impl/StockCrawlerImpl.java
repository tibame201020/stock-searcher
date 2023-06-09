package com.custom.stocksearcher.service.impl;

import com.custom.stocksearcher.models.CompanyStatus;
import com.custom.stocksearcher.models.StockBasicInfo;
import com.custom.stocksearcher.models.StockData;
import com.custom.stocksearcher.models.StockDataId;
import com.custom.stocksearcher.provider.DateProvider;
import com.custom.stocksearcher.provider.WebProvider;
import com.custom.stocksearcher.repo.CompanyStatusRepo;
import com.custom.stocksearcher.repo.StockDataRepo;
import com.custom.stocksearcher.service.StockCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.YearMonth;

import static com.custom.stocksearcher.constant.Constant.COMPANY_URL;
import static com.custom.stocksearcher.constant.Constant.STOCK_INFO_URL;

@Service
public class StockCrawlerImpl implements StockCrawler {

    @Autowired
    private WebProvider webProvider;
    @Autowired
    private DateProvider dateProvider;
    @Autowired
    private StockDataRepo stockDataRepo;
    @Autowired
    private CompanyStatusRepo companyStatusRepo;

    @Override
    public Flux<StockData> getStockDataLs(String stockCode, String dateStr) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("===============================================")
                .append("\n")
                .append("從網路取得股價資料").append("\n")
                .append("代號 :").append(stockCode).append("\n")
                .append("時間 :").append(dateStr);

        log.info(stringBuilder);
        String url = String.format(STOCK_INFO_URL, dateStr, stockCode);
        StockBasicInfo stockBasicInfo = webProvider.getUrlToObject(url, StockBasicInfo.class);
        if (null == stockBasicInfo || null == stockBasicInfo.getData()) {
            return Flux.empty();
        }
        return stockDataRepo.saveAll(Flux.fromArray(stockBasicInfo.getData())
                .map(dataInfo -> translateStockData(dataInfo, stockCode)));
    }

    @Override
    public Flux<CompanyStatus> getCompanies() {
        log.info("===============================================\n從網路取得資料公司資料");
        CompanyStatus[] companies = new RestTemplate().getForObject(COMPANY_URL, CompanyStatus[].class);

        assert companies != null;
        return companyStatusRepo.saveAll(Flux.fromArray(companies));
    }

    private StockData translateStockData(String[] dataInfo, String code) {
        StockDataId stockDataId = new StockDataId();
        stockDataId.setCode(code);
        stockDataId.setDate(LocalDate.parse(dataInfo[0].replace("/", "-")));

        StockData stockData = new StockData();
        stockData.setStockDataId(stockDataId);
        stockData.setCode(code);
        stockData.setDate(stockDataId.getDate());
        stockData.setTradeVolume(dataInfo[1]);
        stockData.setTradeValue(dataInfo[2]);
        stockData.setOpeningPrice(dataInfo[3]);
        stockData.setHighestPrice(dataInfo[4]);
        stockData.setLowestPrice(dataInfo[5]);
        stockData.setClosingPrice(dataInfo[6]);
        stockData.setChange(dataInfo[7]);
        stockData.setTransaction(dataInfo[8]);
        stockData.setUpdateDate(LocalDate.now());

        boolean isThisMonth = dateProvider.isThisMonth(dataInfo[0].replace("/", ""));
        stockData.setHistory(!isThisMonth);
        stockData.setMonth(YearMonth.from(stockDataId.getDate()).toString());

        return stockData;
    }
}
