package sk.iway.iwcm.stat.rest;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.abtesting.ABTesting;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.stat.ChartType;
import sk.iway.iwcm.stat.Column;
import sk.iway.iwcm.stat.FilterHeaderDto;
import sk.iway.iwcm.stat.StatNewDB;
import sk.iway.iwcm.stat.jpa.DocNewDTO;
import sk.iway.iwcm.stat.jpa.InOutDTO;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/stat/doc-new")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_stat')")
@Datatable
public class DocNewRestController extends DatatableRestControllerV2<DocNewDTO, Long> {

    @Autowired
    public DocNewRestController() {
        super(null);
    }

    @Override
    public void beforeSave(DocNewDTO entity) {
        throwError("datatables.error.recordIsNotEditable");
    }

    @Override
    public Page<DocNewDTO> getAllItems(Pageable pageable) {
        //Process params from request into FilterHeaderDto
        FilterHeaderDto filter = StatService.processRequestToStatFilter(getRequest(), null);
        int docId = Tools.getIntValue(getRequest().getParameter("docId"), -1);

        List<Column> aVariantColumns = getAvariantColumns(docId, filter);
        DatatablePageImpl<DocNewDTO> page = new DatatablePageImpl<>(columnsToItems(aVariantColumns, filter));
        return page;
    }

    @Override
    public Page<DocNewDTO> searchItem(Map<String, String> params, Pageable pageable, DocNewDTO search) {
        //Process received params into FilterHeaderDto
        FilterHeaderDto filter = StatService.processMapToStatFilter(params, null);
        int docId = Tools.getIntValue(getRequest().getParameter("docId"), -1);

        List<Column> aVariantColumns = getAvariantColumns(docId, filter);
        DatatablePageImpl<DocNewDTO> page = new DatatablePageImpl<>(columnsToItems(aVariantColumns, filter));
        return page;
    }

    private List<Column> getAvariantColumns(int docId, FilterHeaderDto filter) {
        return StatNewDB.getPageViews(docId, filter.getDateFrom(), filter.getDateTo());
    }

    private List<DocNewDTO> columnsToItems(List<Column> columns, FilterHeaderDto filter) {
        List<DocNewDTO> items = new ArrayList<>();

        int order = 1;
        for(Column column : columns) {
            DocNewDTO item = new DocNewDTO();
            item.setOrder(order);
            item.setYear(column.getIntColumn1());
            item.setMonth(column.getIntColumn2());
            item.setDay(column.getIntColumn3());
            item.setVisits(column.getIntColumn4());
            item.setSessions(column.getIntColumn5());
            item.setUniqueUsers(column.getIntColumn6());
            items.add(item);
            order++;
        }


        if(filter.getChartType() == ChartType.LINE) {
            for(DocNewDTO item : items) {
                item.setDayDate(
                    new Date(
                        DB.getTimestamp(item.getDay() + "." + item.getMonth() + "." + item.getYear())
                    )
                );
            }
        }
        return items;
    }

    //Compute date from column values
    private Date getColumnDate(Column c) {
        return new Date(DB.getTimestamp(c.getIntColumn3() + "." + c.getIntColumn2() + "." + c.getIntColumn1()));
    }

    private List<DocNewDTO> getBvariantData(int docId, Boolean returnPercentual, FilterHeaderDto filter) {
        List<Integer> variantsDocIds = ABTesting.getAllVariantsDocIds(docId);

        //Return Variant B data as they are, dont do percentual computation
        if(!returnPercentual.booleanValue()) {
            List<Column> variantColumns = new ArrayList<>();
            for(int variantDocId : variantsDocIds) {
                variantColumns.addAll(StatNewDB.getPageViews(variantDocId, filter.getDateFrom(), filter.getDateTo()));
            }

            return columnsToItems(variantColumns, filter);
        }

        List<Map<Date, Column>> variantsDates = new ArrayList<>(); //kvoli datam do tabulky
        Map<Date, Column> mainDocs = new HashMap<>();
        TreeSet<Date> dates = new TreeSet<>(); //usporiadany zoznam unikatnych datumov hlavnej stranky a vsetkych variant
        Map<Date, Column> map = new HashMap<>();

        List<Column> aVariantColumns = getAvariantColumns(docId, filter);
        for(Column aVariantColumn : aVariantColumns) {
            dates.add(getColumnDate(aVariantColumn));
            mainDocs.put(getColumnDate(aVariantColumn), aVariantColumn);
        }

        for(int variantDocId : variantsDocIds) {
            List<Column> bVariantColumns = StatNewDB.getPageViews(variantDocId, filter.getDateFrom(), filter.getDateTo());
            for(Column bVariantColumn : bVariantColumns) {
                map.put(getColumnDate(bVariantColumn), bVariantColumn);
                dates.add(getColumnDate(bVariantColumn));
            }
            variantsDates.add(map);
        }

        List<DocNewDTO> chartData = valuesToChartData(dates, mainDocs, variantsDates);
        return chartData;
    }

    private List<DocNewDTO> valuesToChartData(TreeSet<Date> dates, Map<Date, Column> mainDocs, List<Map<Date, Column>> variantsDates) {
        List<DocNewDTO> items = new ArrayList<>();
	    if(dates == null || mainDocs == null || variantsDates == null || variantsDates.isEmpty() || dates.isEmpty())
            return new ArrayList<>();

        for(Date date : dates) {
            for(int i = 0; i < variantsDates.size(); i++) {
                DocNewDTO itemB = new DocNewDTO();
                itemB.setDayDate(date);

                String[] ratios = Constants.getString("ABTestingRatio").split(":");
                float ratio = 1;

                try {
                    ratio = (float) Tools.getIntValue(ratios[0], 1) / Tools.getIntValue(ratios[i + 1], 1);
                } catch(Exception e){
                    //
                }

                itemB.setVisits(variantsDates.get(i).get(date) != null ? (int) (variantsDates.get(i).get(date).getIntColumn4() * ratio) : 0 );
                itemB.setSessions(variantsDates.get(i).get(date) != null ? (int) (variantsDates.get(i).get(date).getIntColumn5() * ratio) : 0 );
                itemB.setUniqueUsers(variantsDates.get(i).get(date) != null ? (int) (variantsDates.get(i).get(date).getIntColumn6() * ratio) : 0 );

                items.add(itemB);
            }
        }
        return items;
    }

    @RequestMapping(value="/b-variant", params={"docId", "dayDate", "percentual"})
    @ResponseBody
    public List<DocNewDTO> getBvariantChartData(
                        @RequestParam("docId") String docId,
                        @RequestParam("dayDate") String stringRange,
                        @RequestParam("percentual") Boolean percentual) {

        Date[] dateRangeArr = StatService.processDateRangeString(stringRange);
        FilterHeaderDto filter = new FilterHeaderDto();
        filter.setDateFrom(dateRangeArr[0]);
        filter.setDateTo(dateRangeArr[1]);
        filter.setChartType(ChartType.LINE);
        List<DocNewDTO> bVariantChartData = getBvariantData(Integer.valueOf(docId), percentual, filter);
        return bVariantChartData;
    }

    @RequestMapping(value="/incoming", params={"docId", "dayDate"})
    @ResponseBody
    public Page<InOutDTO> getInLineChartData(
                        @RequestParam("docId") String docId,
                        @RequestParam("dayDate") String stringRange) {
        Date[] dateRangeArr = StatService.processDateRangeString(stringRange);
        List<Column> incomingStats = StatNewDB.getIncomingStats(Integer.parseInt(docId), dateRangeArr[0], dateRangeArr[1], null, getRequest());
        DatatablePageImpl<InOutDTO> page = new DatatablePageImpl<>(columnsToInOutItems(incomingStats));
        return page;
    }

    @RequestMapping(value="/outgoing", params={"docId", "dayDate"})
    @ResponseBody
    public Page<InOutDTO> getOutLineChartData(
                        @RequestParam("docId") String docId,
                        @RequestParam("dayDate") String stringRange) {
        Date[] dateRangeArr = StatService.processDateRangeString(stringRange);
        List<Column> incomingStats = StatNewDB.getOutgoingStats(Integer.parseInt(docId), dateRangeArr[0], dateRangeArr[1]);
        DatatablePageImpl<InOutDTO> page = new DatatablePageImpl<>(columnsToInOutItems(incomingStats));
        return page;
    }

    private List<InOutDTO> columnsToInOutItems(List<Column> columns) {
        List<InOutDTO> items = new ArrayList<>();

        int order = 1;
        for(Column column : columns) {
            InOutDTO item = new InOutDTO();
            item.setOrder(order);
            item.setName(column.getColumn1());
            item.setCount(column.getIntColumn2());
            items.add(item);
            order++;
        }
        return items;
    }

    @RequestMapping(value="/doc-title", params={"docId"}, produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String getDocTitle(@RequestParam("docId") int docId) {

        DocDB docDB = DocDB.getInstance();
        DocDetails doc = docDB.getBasicDocDetails(docId, false);
        if (doc != null) return doc.getTitle();
        else return "Page "+docId;

    }
}
