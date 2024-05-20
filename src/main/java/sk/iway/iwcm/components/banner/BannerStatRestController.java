package sk.iway.iwcm.components.banner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.banner.model.BannerBean;
import sk.iway.iwcm.stat.Column;
import sk.iway.iwcm.stat.rest.StatService;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@Datatable
@RequestMapping("/admin/rest/banner-stat")
@PreAuthorize("@WebjetSecurityService.hasPermission('menuBanner')")
public class BannerStatRestController extends DatatableRestControllerV2<BannerBean, Long> {

    private final BannerRepository bannerRepository;
    private static final String VIEWS = "views";
    private static final String CLICKS = "clicks";

    @Autowired
    public BannerStatRestController(BannerRepository bannerRepository) {
        super(bannerRepository);
        this.bannerRepository = bannerRepository;
    }

    @Override
    public Page<BannerBean> getAllItems(Pageable pageable) {

        int bannerId = Tools.getIntValue(getRequest().getParameter("bannerId"), -1);
        if(bannerId > -1) {
            Optional<BannerBean> optBanner = bannerRepository.findFirstByIdAndDomainId(Long.valueOf(bannerId), CloudToolsForCore.getDomainId());
            if(optBanner.isPresent()) optBanner.get().setNameLink(optBanner.get().getName());
            return new DatatablePageImpl<>( Arrays.asList(optBanner.orElse(null)) );
        }

        return new DatatablePageImpl<>(getDataAndConvertIntoPageItems(null));
    }

    @Override
    public Page<BannerBean> searchItem(Map<String, String> params, Pageable pageable, BannerBean search) {

        String dateRange = "";
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if(entry.getKey().equalsIgnoreCase("searchDayDate")) {
                dateRange = entry.getValue();
            }
        }

        return new DatatablePageImpl<>(getDataAndConvertIntoPageItems(dateRange));
    }

    private List<BannerBean> getDataAndConvertIntoPageItems(String dateRange) {
        //Process date range param
        Date[] date = StatService.processDateRangeString(dateRange);
        List<BannerBean> topBanners = new ArrayList<>();
        List<Column> columns = BannerDB.getTop10Banners(date[0], date[1], null);

        for(Column column : columns) {
            BannerBean bannerStat = new BannerBean();
            bannerStat.setId(Long.valueOf( column.getIntColumn1()));
            bannerStat.setBannerGroup(column.getColumn8());
            bannerStat.setBannerType(Integer.valueOf(column.getColumn9()));
            bannerStat.setStatViews(column.getIntColumn3());
            bannerStat.setStatClicks(column.getIntColumn4());
            bannerStat.setNameLink(column.getColumn3());

            topBanners.add(bannerStat);
        }
        return topBanners;
    }

    @RequestMapping(
        value="/lineChartData",
        params={"dayDate", "dataType"})
    @ResponseBody
    public Map<String, List<BannerBean>> getLineChartData(
                @RequestParam("dayDate") String stringRange,
                @RequestParam("dataType") String dataType) {

        Date[] dateRangeArr = StatService.processDateRangeString(stringRange);

        Map<String,  Map<Date, Number>> data;
        if(dataType.equals(VIEWS)) {
            data = BannerDB.getTop10BannersViewsTimeData(dateRangeArr[0], dateRangeArr[1], null);
        } else if(dataType.equals(CLICKS)) {
            data = BannerDB.getTop10BannersClicksTimeData(dateRangeArr[0], dateRangeArr[1], null);
        } else {
            //Invalid parameter
            return null;
        }

        return hashtableToLineChartData(data, dataType);
    }

    @RequestMapping(
        value="/detailLineChartData",
        params={"bannerId", "dayDate", "dataType"})
    @ResponseBody
    public Map<String, List<BannerBean>> getDetailLineChartData(
                @RequestParam("bannerId") int bannerId,
                @RequestParam("dayDate") String stringRange,
                @RequestParam("dataType") String dataType) {

        Date[] dateRangeArr = StatService.processDateRangeString(stringRange);

        Map<String,  Map<Date, Number>> data;
        if(dataType.equals(VIEWS)) {
            data = BannerDB.getBannerStatViewsTimeData(dateRangeArr[0], dateRangeArr[1], bannerId);
        } else if(dataType.equals(CLICKS)) {
            data = BannerDB.getBannerStatClicksTimeData(dateRangeArr[0], dateRangeArr[1], bannerId);
        } else {
            //Invalid parameter
            return null;
        }

        return hashtableToLineChartData(data, dataType);
    }

    private Map<String, List<BannerBean>> hashtableToLineChartData(Map<String,  Map<Date, Number>> hashtable, String dataType) {
        //represent chart data
        Map<String, List<BannerBean>> chartData = new HashMap<>();

        //setOfKeys represent baner adress
        Set<String> setOfKeys = hashtable.keySet();

        for(String key : setOfKeys) {
            //items is list of BannerBean values for one banner
            List<BannerBean> items = new ArrayList<>();
            Map<Date, Number> dayViews = hashtable.get(key);

            if(dataType.equals(CLICKS)) {
                //Clicks
                for (Map.Entry<Date, Number> entry : dayViews.entrySet()) {
                    BannerBean item = new BannerBean();
                    item.setNameLink(key);
                    item.setDayDate(entry.getKey());
                    item.setStatClicks(entry.getValue().intValue());
                    items.add(item);
                }
            } else if(dataType.equals(VIEWS)) {
                //Views
                for (Map.Entry<Date, Number> entry : dayViews.entrySet()) {
                    BannerBean item = new BannerBean();
                    item.setNameLink(key);
                    item.setDayDate(entry.getKey());
                    item.setStatViews(entry.getValue().intValue());
                    items.add(item);
                }
            }
            chartData.put(key, items);
        }
        return chartData;
    }

    @RequestMapping(value="/banner-title", params={"bannerId"}, produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String getDocTitle(@RequestParam("bannerId") long bannerId) {

        Optional<BannerBean> optBanner = bannerRepository.findFirstByIdAndDomainId(bannerId, CloudToolsForCore.getDomainId());
        if (optBanner.isPresent() && Tools.isNotEmpty(optBanner.get().getName())) return optBanner.get().getName();
        else return "Banner " + bannerId;
    }
}
