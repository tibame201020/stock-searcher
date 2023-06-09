package com.custom.stocksearcher.task;

import com.custom.stocksearcher.repo.CompanyStatusRepo;
import com.custom.stocksearcher.service.StockCrawler;
import com.custom.stocksearcher.service.StockFinder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 股價爬蟲Task
 */
@Component
public class Schedule {
    private final Log LOG = LogFactory.getLog(this.getClass());

    @Autowired
    private StockFinder stockFinder;
    @Autowired
    private StockCrawler stockCrawler;
    @Autowired
    private CompanyStatusRepo companyStatusRepo;

    // todo: 取得公司列表 > 取得股價資訊 > 存入elasticsearch > 更新公司 crawler status
}
