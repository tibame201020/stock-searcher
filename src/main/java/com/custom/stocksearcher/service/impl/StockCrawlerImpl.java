package com.custom.stocksearcher.service.impl;

import com.custom.stocksearcher.models.CompanyStatus;
import com.custom.stocksearcher.models.StockBasicInfo;
import com.custom.stocksearcher.models.StockData;
import com.custom.stocksearcher.models.listed.ListedStock;
import com.custom.stocksearcher.models.listed.ListedStockId;
import com.custom.stocksearcher.models.tpex.TPExCompany;
import com.custom.stocksearcher.models.tpex.TPExStock;
import com.custom.stocksearcher.models.tpex.TPExStockId;
import com.custom.stocksearcher.models.tpex.TPExUrlObject;
import com.custom.stocksearcher.provider.DateProvider;
import com.custom.stocksearcher.provider.WebProvider;
import com.custom.stocksearcher.repo.CompanyStatusRepo;
import com.custom.stocksearcher.repo.ListedStockRepo;
import com.custom.stocksearcher.repo.TPExStockRepo;
import com.custom.stocksearcher.service.StockCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.custom.stocksearcher.constant.Constant.*;

@Service
public class StockCrawlerImpl implements StockCrawler {

    @Autowired
    private WebProvider webProvider;
    @Autowired
    private DateProvider dateProvider;
    @Autowired
    private ListedStockRepo listedStockRepo;
    @Autowired
    private CompanyStatusRepo companyStatusRepo;
    @Autowired
    private TPExStockRepo tpExStockRepo;

    @Override
    public Flux<ListedStock> getListedStockDataFromTWSEApi(String stockCode, String dateStr) {
        String url = String.format(STOCK_INFO_URL, dateStr, stockCode);
        StockBasicInfo stockBasicInfo;
        try {
            stockBasicInfo = webProvider.getUrlToObject(url, StockBasicInfo.class);
        } catch (Exception e) {
            log.error("get listed stock from url error:\n" + url);
            return Flux.empty();
        }

        if (null == stockBasicInfo || null == stockBasicInfo.getData()) {
            return Flux.empty();
        }

        Flux<ListedStock> listedStockFlux = Flux.fromArray(stockBasicInfo.getData())
                .filter(data -> data.length > 0)
                .map(this::wrapperFromData)
                .flatMap(stockData -> {
                    ListedStockId listedStockId = new ListedStockId();
                    listedStockId.setCode(stockCode);
                    listedStockId.setDate(stockData.getDate());
                    ListedStock listedStock = new ListedStock();
                    listedStock.setListedStockId(listedStockId);
                    listedStock.setStockData(stockData);
                    listedStock.setDate(stockData.getDate());
                    listedStock.setUpdateDate(LocalDate.now());

                    return Mono.just(listedStock);
                });

        return listedStockRepo.saveAll(listedStockFlux);
    }

    @Override
    public Flux<CompanyStatus> getCompanies() {
        TPExCompany[] tpExCompanies = webProvider.getUrlToObject(TPEx_COMPANY_URL, TPExCompany[].class);
        CompanyStatus[] companies = webProvider.getUrlToObject(COMPANY_URL, CompanyStatus[].class);
        assert tpExCompanies != null;
        assert companies != null;
        Flux<CompanyStatus> companyStatusFlux = Flux.fromArray(tpExCompanies)
                .filter(tpExCompany -> tpExCompany.getCode().length() != 6)
                .flatMap(tpExCompany -> {
                    CompanyStatus companyStatus = new CompanyStatus();
                    companyStatus.setCode(tpExCompany.getCode());
                    companyStatus.setName(tpExCompany.getName());
                    companyStatus.setTPE(true);
                    return Mono.just(companyStatus);
                })
                .concatWith(Flux.fromArray(companies));

        return companyStatusRepo.saveAll(companyStatusFlux);
    }

    @Override
    public Flux<TPExStock> getTPExStockFromTPEx(String url) {
        TPExUrlObject tpExUrlObject;
        try {
            tpExUrlObject = webProvider.getUrlToObject(url, TPExUrlObject.class);
        } catch (Exception e) {
            return Flux.empty();
        }
        String reportDate = tpExUrlObject.getReportDate();

        Flux<TPExStock> tpExStockFlux = Flux.fromArray(tpExUrlObject.getAaData())
                .filter(data -> null != data || data.length > 0)
                .filter(data -> data[0].length() != 6)
                .flatMap(data -> Flux.just(wrapperFromData(data, reportDate)));

        return tpExStockRepo.saveAll(tpExStockFlux);
    }

    /**
     * 包裝TPExStock
     *
     * @param data 資料
     * @param date 日期
     * @return TPExStock
     */
    private TPExStock wrapperFromData(String[] data, String date) {
        LocalDate stockDate = LocalDate.parse(date.replaceAll("/", "-"));

        TPExStockId tpExStockId = new TPExStockId();
        tpExStockId.setCode(data[0]);
        tpExStockId.setDate(stockDate);

        StockData stockData = new StockData();
        stockData.setDate(stockDate);
        stockData.setTradeVolume(transBigDecimal(data[8]));
        stockData.setTradeValue(transBigDecimal(data[9]));
        stockData.setOpeningPrice(transBigDecimal(data[4]));
        stockData.setHighestPrice(transBigDecimal(data[5]));
        stockData.setLowestPrice(transBigDecimal(data[6]));
        stockData.setClosingPrice(transBigDecimal(data[2]));
        stockData.setChange(transBigDecimal(data[3]));

        TPExStock tpExStock = new TPExStock();
        tpExStock.setTpExStockId(tpExStockId);
        tpExStock.setStockData(stockData);
        tpExStock.setDate(stockDate);
        tpExStock.setUpdateDate(LocalDate.now());

        return tpExStock;
    }


    /**
     * StockData
     *
     * @param dataInfo 資料
     * @return StockData
     */
    private StockData wrapperFromData(String[] dataInfo) {
        StockData stockData = new StockData();
        stockData.setDate(LocalDate.parse(dataInfo[0].replace("/", "-")));
        stockData.setTradeVolume(transBigDecimal(dataInfo[1]));
        stockData.setTradeValue(transBigDecimal(dataInfo[2]));
        stockData.setOpeningPrice(transBigDecimal(dataInfo[3]));
        stockData.setHighestPrice(transBigDecimal(dataInfo[4]));
        stockData.setLowestPrice(transBigDecimal(dataInfo[5]));
        stockData.setClosingPrice(transBigDecimal(dataInfo[6]));
        stockData.setChange(transBigDecimal(dataInfo[7]));
        stockData.setTransaction(transBigDecimal(dataInfo[8]));

        return stockData;
    }

    /**
     * 轉換BigDecimal
     *
     * @param str 目標字串
     * @return BigDecimal
     */
    private BigDecimal transBigDecimal(String str) {
        try {
            str = str.replaceAll(",", "").replaceAll("X", "").trim();
            return new BigDecimal(str);
        } catch (Exception e) {
            return null;
        }
    }
}
