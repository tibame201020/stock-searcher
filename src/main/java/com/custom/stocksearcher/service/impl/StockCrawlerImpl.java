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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.Objects;

import static com.custom.stocksearcher.constant.Constant.COMPANY_URL;
import static com.custom.stocksearcher.constant.Constant.TPEx_COMPANY_URL;

@Service
public class StockCrawlerImpl implements StockCrawler {

    WebClient webClient = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect(true)))
            .exchangeStrategies(ExchangeStrategies.builder()
                    .codecs(config -> config.defaultCodecs().maxInMemorySize(1048576 * 100))
                    .build())
            .build();
    @Autowired
    private DateProvider dateProvider;
    @Autowired
    private ListedStockRepo listedStockRepo;
    @Autowired
    private CompanyStatusRepo companyStatusRepo;
    @Autowired
    private TPExStockRepo tpExStockRepo;

    @Override
    public Flux<ListedStock> getListedStockDataFromTWSEApi(String url) {
        String code = getUrlParam(url, "stockNo");

        Flux<ListedStock> listedStockFlux = webClient
                .post()
                .uri(url)
                .retrieve()
                .bodyToFlux(StockBasicInfo.class)
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
                .bodyToFlux(TPExCompany.class);

        Flux<CompanyStatus> companyStatusFlux = webClient
                .get()
                .uri(COMPANY_URL)
                .retrieve()
                .bodyToFlux(CompanyStatus.class);

        Flux<CompanyStatus> totalCompanyFlux = tpExCompanyFlux
                .filter(tpExCompany -> tpExCompany.getCode().length() != 6)
                .flatMap(tpExCompany -> {
                    CompanyStatus companyStatus = new CompanyStatus();
                    companyStatus.setCode(tpExCompany.getCode());
                    companyStatus.setName(tpExCompany.getName());
                    companyStatus.setTPE(true);
                    return Mono.just(companyStatus);
                })
                .concatWith(companyStatusFlux);

        return companyStatusRepo.saveAll(totalCompanyFlux);
    }

    @Override
    public Flux<TPExStock> getTPExStockFromTPEx(String url) {
        String reportDate = getUrlParam(url, "d");
        Flux<TPExStock> tpExStockFlux = webClient
                .post()
                .uri(url)
                .retrieve()
                .bodyToFlux(TPExUrlObject.class)
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
