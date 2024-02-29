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

import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.stat.ChartType;
import sk.iway.iwcm.stat.Column;
import sk.iway.iwcm.stat.FilterHeaderDto;
import sk.iway.iwcm.stat.StatGraphNewDB;
import sk.iway.iwcm.stat.StatNewDB;
import sk.iway.iwcm.stat.jpa.TopDTO;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/stat/top")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_stat')")
@Datatable
public class TopRestController extends DatatableRestControllerV2<TopDTO, Long> {
    private static final int MAX_SIZE = 100;
    private static final int PIE_CHART_SIZE = 10;
    private FilterHeaderDto filter;

    @Autowired
    public TopRestController() {
        super(null);
    }

    @Override
    public void beforeSave(TopDTO entity) {
        throwError("datatables.error.recordIsNotEditable");
    }

    @Override
    public Page<TopDTO> getAllItems(Pageable pageable) {
        //Process params from request into FilterHeaderDto
        filter = StatService.processRequestToStatFilter(getRequest(), null);
        DatatablePageImpl<TopDTO> page = new DatatablePageImpl<>(getDataAndConvertIntoPageItems());
        return page;
    }

    @Override
    public Page<TopDTO> searchItem(Map<String, String> params, Pageable pageable, TopDTO search) {
        //Process received params into FilterHeaderDto
        filter = StatService.processMapToStatFilter(params, null);

        DatatablePageImpl<TopDTO> page = new DatatablePageImpl<>(getDataAndConvertIntoPageItems());
        return page;
    }

    private List<TopDTO> getDataAndConvertIntoPageItems() {
        List<TopDTO> items = new ArrayList<>();
        if(filter.getChartType() == ChartType.NOT_CHART) {
            return columnsToPageItems(
                StatNewDB.getTopPages(MAX_SIZE, filter.getDateFrom(), filter.getDateTo(), filter.getRootGroupId(), null, filter.getFilterBotsOut()));
        } else if(filter.getChartType()  == ChartType.PIE) {
            return columnsToPieChartData(
                StatNewDB.getTopPages(MAX_SIZE, filter.getDateFrom(), filter.getDateTo(), filter.getRootGroupId(), null, filter.getFilterBotsOut()));
        }
        return items;
    }

    private List<TopDTO> columnsToPieChartData(List<Column> columns) {
        List<TopDTO> items = new ArrayList<>();

        //First pieChartSize - 1 convert and push int list
        for(int i = 0; i < PIE_CHART_SIZE -1 && i < columns.size(); i++) {
            Column column = columns.get(i);
            TopDTO item = new TopDTO();
            String[] fullPathNameArr = column.getColumn6().split("/");
            item.setName(fullPathNameArr[fullPathNameArr.length - 1]);
            item.setVisits(column.getIntColumn3());
            items.add(item);
        }

        //Rest of visits values accumulate
        int accVisits = 0;
        for(int i = PIE_CHART_SIZE; i < columns.size(); i++) {
            Column column = columns.get(i);
            accVisits += column.getIntColumn3();
        }

        //Create last chart value as combination of the rest
        TopDTO item = new TopDTO();
        item.setVisits(accVisits);

        Prop prop = Prop.getInstance();
        item.setName(prop.getText("components.stat.other"));

        //Set this last value into list
        items.add(item);

        return items;
    }

    private List<TopDTO> columnsToPageItems(List<Column> columns) {
        List<TopDTO> items = new ArrayList<>();

        int orderCount = 1;
        for(Column column : columns) {
            TopDTO item = new TopDTO();
            item.setOrder(orderCount);
            item.setName(column.getColumn6());
            item.setVisits(column.getIntColumn3());
            item.setSessions(column.getIntColumn4());
            item.setUniqueUsers(column.getIntColumn5());
            item.setDocId(Integer.parseInt(column.getColumn1()));
            orderCount++;
            items.add(item);
        }
        return items;
    }

    @RequestMapping(
        value="/lineChartData",
        params={"dayDate", "rootDir", "filterBotsOut"})
    @ResponseBody
    public Map<String, List<TopDTO>> getLineChartData(
                @RequestParam("dayDate") String stringRange,
                @RequestParam("rootDir") int rootGroupId,
                @RequestParam("filterBotsOut") boolean filterBotsOut) {

        Date[] dateRangeArr = StatService.processDateRangeString(stringRange);
        Map<String,  Map<Date, Number>> hashTableData = StatGraphNewDB.getTimeData(5, dateRangeArr[0], dateRangeArr[1], rootGroupId, null, "top5", filterBotsOut);
        return hashtableToLineChartData(hashTableData);
    }

    private Map<String, List<TopDTO>> hashtableToLineChartData(Map<String,  Map<Date, Number>> hashtable) {
        //represent chart data
        Map<String, List<TopDTO>> chartData = new HashMap<>();

        //setOfKeys represent page names
        Set<String> setOfKeys = hashtable.keySet();

        for(String key : setOfKeys) {
            //items is list of TopDTO values for one page
            List<TopDTO> items = new ArrayList<>();
            Map<Date, Number> dayViews = hashtable.get(key);
            for (Map.Entry<Date, Number> entry : dayViews.entrySet()) {
                TopDTO item = new TopDTO();
                item.setName(key);
                item.setDayDate(entry.getKey());
                item.setVisits(entry.getValue().intValue());
                items.add(item);
            }
            chartData.put(key, items);
        }
        return chartData;
    }
}
