package sk.iway.iwcm.stat.rest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.stat.Column;
import sk.iway.iwcm.stat.FilterHeaderDto;
import sk.iway.iwcm.stat.StatGraphDB;
import sk.iway.iwcm.stat.StatTableDB;
import sk.iway.iwcm.stat.jpa.BrowsersDTO;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/stat/browser")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_stat')")
@Datatable
public class BrowsersRestController extends DatatableRestControllerV2<BrowsersDTO, Long> {
    private static final int MAX_ROWS = 100;

    @Autowired
    public BrowsersRestController() {
        super(null);
    }

    @Override
    public void beforeSave(BrowsersDTO entity) {
        throwError("datatables.error.recordIsNotEditable");
    }

    @Override
    public Page<BrowsersDTO> getAllItems(Pageable pageable) {
        //Process params from request into FilterHeaderDto
        FilterHeaderDto filter = StatService.processRequestToStatFilter(getRequest(), null);

        List<Column> columns = StatTableDB.getBrowser(MAX_ROWS, filter.getDateFrom(), filter.getDateTo(), filter.getRootGroupIdQuery(), filter.getFilterBotsOut());
        DatatablePageImpl<BrowsersDTO> page = new DatatablePageImpl<>(columnsToItems(columns));
        return page;
    }

    @Override
    public Page<BrowsersDTO> searchItem(Map<String, String> params, Pageable pageable, BrowsersDTO search) {
        //Process received params into FilterHeaderDto
        FilterHeaderDto filter = StatService.processMapToStatFilter(params, null);

        List<Column> columns = StatTableDB.getBrowser(MAX_ROWS, filter.getDateFrom(), filter.getDateTo(), filter.getRootGroupIdQuery(), filter.getFilterBotsOut());
        DatatablePageImpl<BrowsersDTO> page = new DatatablePageImpl<>(columnsToItems(columns));
        return page;
    }

    private List<BrowsersDTO> columnsToItems(List<Column> columns) {
        List<BrowsersDTO> items = new ArrayList<>();

        //Compute sum of all visits
        int sum = 0;
        for(Column column : columns) {
            sum += column.getIntColumn3();
        }

        if (sum > 0) {
            int order = 1;
            for(Column column : columns) {
                BrowsersDTO item = new BrowsersDTO();
                item.setOrder(order);
                item.setBrowser(column.getColumn1());
                item.setPlatform(column.getColumn2());
                item.setVisits(column.getIntColumn3());
                item.setPercentage((double) item.getVisits() * 100 / sum);
                items.add(item);
                order++;
            }
        }
        return items;
    }

    @RequestMapping(
        value="/pieChartData",
        params={"dayDate", "rootDir", "filterBotsOut", "keyValueName"})
    @ResponseBody
    public List<BrowsersDTO> getPieChartData(
                @RequestParam("dayDate") String stringRange,
                @RequestParam("rootDir") int rootGroupId,
                @RequestParam("filterBotsOut") boolean filterBotsOut,
                @RequestParam("keyValueName") String keyValueName) {

        //Key - browser/platform, value - sum of visits
        Map<String, Integer> dataMap = new HashMap<>();
        Date[] dateRangeArr = StatService.processDateRangeString(stringRange);
        List<Column> columns = StatTableDB.getBrowser(5000, dateRangeArr[0], dateRangeArr[1], FilterHeaderDto.groupIdToQuery(rootGroupId), filterBotsOut);

        for(Column column : columns) {
            String key = "";
            if(keyValueName.equals("browser")) {
                key = column.getColumn1(); // get browser
                int space = key.lastIndexOf(" ");
                //Logger.debug(getClass(), "key="+key);
                //odstran verziu prehliadaca
                if (space > 4) key = key.substring(0, space).trim();
            }
            else if(keyValueName.equals("platform"))
                key = column.getColumn2(); //get platform

            Integer visitsCount = dataMap.get(key);

            if(visitsCount == null)
                dataMap.put(key, Integer.valueOf(column.getIntColumn3()));
            else
                dataMap.put(key, Integer.valueOf(column.getIntColumn3()) + visitsCount);
        }

        List<BrowsersDTO> items = new ArrayList<>();
        for(Map.Entry<String, Integer> entry : dataMap.entrySet()) {
            BrowsersDTO item = new BrowsersDTO();
            item.setVisits(entry.getValue());

            if(keyValueName.equals("browser"))
                item.setBrowser(entry.getKey());
            else if(keyValueName.equals("platform"))
                item.setPlatform(entry.getKey());
            items.add(item);
        }

        //usporiadaj podla hodnoty
        List<BrowsersDTO> sortedList = items.stream()
			.sorted(Comparator.comparing(BrowsersDTO::getVisits, Comparator.nullsFirst(Comparator.naturalOrder())).reversed())
            .limit(5)
			.collect(Collectors.toList());

        return sortedList;
    }

    @RequestMapping(
        value="/lineChartData",
        params={"dayDate", "rootDir", "filterBotsOut"})
    @ResponseBody
    public Map<String, List<BrowsersDTO>> getLineChartData(
                @RequestParam("dayDate") String stringRange,
                @RequestParam("rootDir") int rootGroupId,
                @RequestParam("filterBotsOut") boolean filterBotsOut) {

        Date[] dateRangeArr = StatService.processDateRangeString(stringRange);
        HashMap<String, Map<Date, Number>> hashTableData = new HashMap<>(StatGraphDB.getBrowserTimeData(5, dateRangeArr[0], dateRangeArr[1], FilterHeaderDto.groupIdToQuery(rootGroupId), filterBotsOut));
        Map<String, List<BrowsersDTO>> chartData = hashtableToLineChartData(hashTableData);
        return chartData;
    }

    private Map<String, List<BrowsersDTO>> hashtableToLineChartData(HashMap<String,  Map<Date, Number>> hashtable) {
        //represent chart data
        Map<String, List<BrowsersDTO>> chartData = new HashMap<>();

        //setOfKeys represent page names
        Set<String> setOfKeys = hashtable.keySet();

        for(String key : setOfKeys) {
            //items is list of BrowserBean values for one page
            List<BrowsersDTO> items = new ArrayList<>();
            Map<Date, Number> dayViews = hashtable.get(key);
            for (Map.Entry<Date, Number> entry : dayViews.entrySet()) {
                BrowsersDTO item = new BrowsersDTO();
                item.setBrowser(key);
                item.setDayDate(entry.getKey());
                item.setVisits(entry.getValue().intValue());
                items.add(item);
            }
            chartData.put(key, items);
        }
        return chartData;
    }
}
