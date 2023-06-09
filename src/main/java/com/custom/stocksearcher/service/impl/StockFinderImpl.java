package com.custom.stocksearcher.service.impl;

import com.custom.stocksearcher.models.CodeParam;
import com.custom.stocksearcher.models.CompanyStatus;
import com.custom.stocksearcher.models.StockData;
import com.custom.stocksearcher.models.listed.ListedStock;
import com.custom.stocksearcher.models.tpex.TPExStock;
import com.custom.stocksearcher.provider.DateProvider;
import com.custom.stocksearcher.repo.CompanyStatusRepo;
import com.custom.stocksearcher.repo.ListedStockRepo;
import com.custom.stocksearcher.repo.TPExStockRepo;
import com.custom.stocksearcher.service.StockFinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.Comparator;

@Service
public class StockFinderImpl implements StockFinder {

    @Autowired
    private ListedStockRepo listedStockRepo;
    @Autowired
    private CompanyStatusRepo companyStatusRepo;
    @Autowired
    private TPExStockRepo tpExStockRepo;
    @Autowired
    private DateProvider dateProvider;


    @Override
    public Flux<StockData> findStockInfo(CodeParam codeParam) {
        return Flux.from(companyStatusRepo.findById(codeParam.getCode()))
                .flatMap(companyStatus -> {
                    if (companyStatus.isTPE()) {
                        return findTPExStock(codeParam);
                    } else {
                        return findListedStock(codeParam);
                    }
                })
                .filter(this::verifyStockData);
    }

    @Override
    public Flux<CompanyStatus> findCompaniesByKeyWord(String keyword) {
        if (null == keyword || keyword.length() < 2) {
            return Flux.empty();
        }
        return companyStatusRepo
                .findAll()
                .filter(companyStatus -> {
                    if (companyStatus.isTPE()) {
                        return companyStatus.getCode().length() != 6;
                    } else {
                        return true;
                    }
                })
                .filter(companyStatus -> companyStatus.toString().contains(keyword));
    }

    @Override
    public Flux<StockData> getStockDataWithKlineCnt(CodeParam codeParam) {
        Integer klineCnt = codeParam.getKlineCnt();
        if (null != klineCnt && klineCnt > 0) {
            LocalDate beginDate = LocalDate.parse(codeParam.getEndDate()).minusDays(klineCnt * 3L);
            codeParam.setBeginDate(beginDate.toString());
        }

        Flux<StockData> stockDataFlux = findStockInfo(codeParam);

        if (null != klineCnt && klineCnt > 0) {
            stockDataFlux = stockDataFlux.takeLast(klineCnt);
        }
        return Flux.from(stockDataFlux).sort(Comparator.comparing(StockData::getDate));
    }


    /**
     * 上市股票查詢
     *
     * @param codeParam
     * @return
     */
    private Flux<StockData> findListedStock(CodeParam codeParam) {
        return Flux.from(companyStatusRepo.findById(codeParam.getCode()))
                .filter(companyStatus -> !companyStatus.isTPE())
                .flatMap(companyStatus -> listedStockRepo
                        .findByListedStockId_CodeAndDateBetweenOrderByDate(
                                companyStatus.getCode(),
                                LocalDate.parse(codeParam.getBeginDate()).minusDays(1),
                                LocalDate.parse(codeParam.getEndDate()).plusDays(1)
                        ))
                .map(ListedStock::getStockData);
    }


    /**
     * 上櫃股票查詢
     *
     * @param codeParam
     * @return
     */
    private Flux<StockData> findTPExStock(CodeParam codeParam) {
        return Flux.from(companyStatusRepo.findById(codeParam.getCode()))
                .filter(CompanyStatus::isTPE)
                .filter(companyStatus -> companyStatus.getCode().length() != 6)
                .flatMap(companyStatus -> tpExStockRepo
                        .findByTpExStockId_CodeAndDateBetweenOrderByDate(
                                companyStatus.getCode(),
                                LocalDate.parse(codeParam.getBeginDate()).minusDays(1),
                                LocalDate.parse(codeParam.getEndDate()).plusDays(1)
                        )).map(TPExStock::getStockData);
    }

    /**
     * 確保stockData為有效資料
     *
     * @param stockData
     * @return
     */
    private boolean verifyStockData(StockData stockData) {
        return stockData.getOpeningPrice() != null
                && stockData.getClosingPrice() != null
                && stockData.getHighestPrice() != null
                && stockData.getLowestPrice() != null;
    }

}
