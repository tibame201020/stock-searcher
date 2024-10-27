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
import com.custom.stocksearcher.repo.CompanyStatusRepo;
import com.custom.stocksearcher.repo.ListedStockRepo;
import com.custom.stocksearcher.repo.TPExStockRepo;
import com.custom.stocksearcher.service.StockCrawler;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.custom.stocksearcher.constant.Constant.*;

@Service
public class StockCrawlerImpl implements StockCrawler {
    private final WebClient webClient;
    private final ListedStockRepo listedStockRepo;
    private final CompanyStatusRepo companyStatusRepo;
    private final TPExStockRepo tpExStockRepo;
    private final DateProvider dateProvider;

    public StockCrawlerImpl(WebClient webClient, ListedStockRepo listedStockRepo, CompanyStatusRepo companyStatusRepo, TPExStockRepo tpExStockRepo, DateProvider dateProvider) {
        this.webClient = webClient;
        this.listedStockRepo = listedStockRepo;
        this.companyStatusRepo = companyStatusRepo;
        this.tpExStockRepo = tpExStockRepo;
        this.dateProvider = dateProvider;
    }


    @Override
    public Flux<ListedStock> getListedStockDataFromTWSEApi(String url) {
        String code = getUrlParam(url, "stockNo");

        Flux<ListedStock> listedStockFlux = webClient
                .post()
                .uri(url)
                .retrieve()
                .bodyToFlux(StockBasicInfo.class)
                .limitRate(WEBCLIENT_LIMIT_RATE)
                .filter(stockBasicInfo -> Objects.nonNull(stockBasicInfo) && Objects.nonNull(stockBasicInfo.getData()))
                .flatMap(stockBasicInfo -> Flux.fromArray(stockBasicInfo.getData()))
                .map(this::wrapperFromData)
                .flatMap(stockData -> {
                    ListedStockId listedStockId = new ListedStockId();
                    listedStockId.setCode(code);
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
        Flux<TPExCompany> tpExCompanyFlux = webClient
                .get()
                .uri(TPEx_COMPANY_URL)
                .retrieve()
                .bodyToFlux(TPExCompany.class)
                .limitRate(WEBCLIENT_LIMIT_RATE);

        Flux<CompanyStatus> companyStatusFlux = webClient
                .get()
                .uri(COMPANY_URL)
                .retrieve()
                .bodyToFlux(CompanyStatus.class)
                .limitRate(WEBCLIENT_LIMIT_RATE);

        Flux<CompanyStatus> totalCompanyFlux = tpExCompanyFlux
                .map(tpExCompany -> {
                    CompanyStatus companyStatus = new CompanyStatus();
                    companyStatus.setCode(tpExCompany.getCode());
                    companyStatus.setName(tpExCompany.getName());
                    companyStatus.setTPE(true);
                    return companyStatus;
                })
                .concatWith(companyStatusFlux);

        return companyStatusRepo.saveAll(totalCompanyFlux);
    }

    @Override
    public List<CompanyStatus> getListedCompanies() {
        Iterable<CompanyStatus> companyStatuses = getCompanies().toIterable();

        List<CompanyStatus> rtn = new ArrayList<>();
        for (CompanyStatus companyStatus : companyStatuses) {
            if (!companyStatus.isTPE()) {
                rtn.add(companyStatus);
            }
        }

        return rtn;
    }

    @Override
    public Flux<TPExStock> getTPExStockFromTPEx(String url) {
        String reportDate = getUrlParam(url, "d");
        Flux<TPExStock> tpExStockFlux = webClient
                .post()
                .uri(url)
                .retrieve()
                .bodyToFlux(TPExUrlObject.class)
                .limitRate(WEBCLIENT_LIMIT_RATE)
                .filter(tpExUrlObject -> Objects.nonNull(tpExUrlObject) && Objects.nonNull(tpExUrlObject.getAaData()))
                .flatMap(tpExUrlObject -> Flux.fromArray(tpExUrlObject.getAaData()))
                .filter(data -> data[0].length() != 6)
                .flatMap(data -> {
                    assert reportDate != null;
                    return Flux.just(wrapperFromData(data, reportDate));
                });

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
        LocalDate stockDate = dateProvider.strToLocalDate(date, STOCK_DATE_FORMAT);

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
        LocalDate date = dateProvider.strToLocalDate(dataInfo[0], STOCK_DATE_FORMAT);
        stockData.setDate(date);
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

    /**
     * 從url取得param value
     *
     * @param url 目標url
     * @param key param key
     * @return value
     */
    private String getUrlParam(String url, String key) {
        try {
            URI uri = new URI(url);
            String query = uri.getQuery();

            if (query != null && !query.isEmpty()) {
                String[] parameters = query.split("&");
                for (String parameter : parameters) {
                    String[] keyValue = parameter.split("=");
                    if (keyValue.length == 2 && keyValue[0].equals(key)) {
                        return keyValue[1];
                    }
                }
            }
        } catch (Exception e) {
            log.error("get url param error");
        }
        return null;
    }
}
