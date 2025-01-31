package sk.iway.iwcm.stat.rest;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.stat.Column;
import sk.iway.iwcm.stat.FilterHeaderDto;
import sk.iway.iwcm.stat.StatGraphDB;
import sk.iway.iwcm.stat.StatTableDB;
import sk.iway.iwcm.stat.jpa.CountryDTO;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/stat/country")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_stat')")
@Datatable
public class CountryRestController extends DatatableRestControllerV2<CountryDTO, Long> {
    private static final int MAX_ROWS = 100;

    @Autowired
    public CountryRestController() {
        super(null);
    }

    @Override
    public void beforeSave(CountryDTO entity) {
        throwError("datatables.error.recordIsNotEditable");
    }

    @Override
    public Page<CountryDTO> getAllItems(Pageable pageable) {
        //Process params from request into FilterHeaderDto
        FilterHeaderDto filter = StatService.processRequestToStatFilter(getRequest(), null);

        DatatablePageImpl<CountryDTO> page = new DatatablePageImpl<>(getDataAndConvertIntoPageItems(filter));
        return page;
    }

    @Override
    public Page<CountryDTO> searchItem(Map<String, String> params, Pageable pageable, CountryDTO search) {
        //Process received params into FilterHeaderDto
        FilterHeaderDto filter = StatService.processMapToStatFilter(params, null, getUser());

        DatatablePageImpl<CountryDTO> page = new DatatablePageImpl<>(getDataAndConvertIntoPageItems(filter));
        return page;
    }

    private List<CountryDTO> getDataAndConvertIntoPageItems(FilterHeaderDto filter) {
        List<Column> columns = StatTableDB.getNamedCountries(MAX_ROWS, filter.getDateFrom(), filter.getDateTo(), filter.getRootGroupIdQuery(), PageLng.getUserLng(getRequest()), filter.getFilterBotsOut());
        List<CountryDTO> items = columnsToPageItems(columns);
        return items;
    }

    private List<CountryDTO> columnsToPageItems(List<Column> columns) {
        List<CountryDTO> items = new ArrayList<>();

        //Compute sum of all visits
        Double sum = 0.0;
        for(Column column : columns)
            sum += column.getIntColumn2();

        if (sum > 0) {
            int order = 1;
            for(Column column : columns) {
                CountryDTO item = new CountryDTO();
                item.setOrder(order);
                item.setCountry(column.getColumn1());
                item.setVisits(column.getIntColumn2());
                item.setPercentage(item.getVisits() * 100.0 / sum);
                items.add(item);
                order++;
            }
        }
        return items;
    }

    @RequestMapping(
        value="/lineChartData",
        params={"dayDate", "rootDir", "filterBotsOut"})
    @ResponseBody
    public Map<String, List<CountryDTO>> test(
                @RequestParam("dayDate") String stringRange,
                @RequestParam("rootDir") int rootGroupId,
                @RequestParam("filterBotsOut") boolean filterBotsOut) {

        Date[] dateRangeArr = StatService.processDateRangeString(stringRange);
        HashMap<String,  Map<Date, Number>> hashTableData = new HashMap<>(StatGraphDB.getCountryTimeData(5, dateRangeArr[0], dateRangeArr[1], FilterHeaderDto.groupIdToQuery(rootGroupId), StatGraphDB.FULL_COUNTRY_NAMES, getRequest(), filterBotsOut));
        Map<String, List<CountryDTO>> chartData = hashtableToLineChartData(hashTableData);
        return chartData;
    }

    private Map<String, List<CountryDTO>> hashtableToLineChartData(HashMap<String,  Map<Date, Number>> hashtable) {
        //represent chart data
        Map<String, List<CountryDTO>> chartData = new HashMap<>();

        //setOfKeys represent page names
        Set<String> setOfKeys = hashtable.keySet();

        for(String key : setOfKeys) {
            //items is list of CountryDTO values for one page
            List<CountryDTO> items = new ArrayList<>();
            Map<Date, Number> dayViews = hashtable.get(key);
            for (Map.Entry<Date, Number> entry : dayViews.entrySet()) {
                CountryDTO item = new CountryDTO();
                item.setCountry(key);
                item.setDayDate(entry.getKey());
                item.setVisits(entry.getValue().intValue());
                items.add(item);
            }
            chartData.put(key, items);
        }
        return chartData;
    }
}