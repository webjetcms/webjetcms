package sk.iway.iwcm.stat.rest;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.stat.Column;
import sk.iway.iwcm.stat.StatNewDB;
import sk.iway.iwcm.stat.jpa.VisitsDTO;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;


@RestController
@RequestMapping("/admin/rest/stat/views")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_stat')")
@Datatable
public class ViewsRestController extends DatatableRestControllerV2<VisitsDTO, Long> {

    private Date dateFrom = new Date();
    private Date dateTo = new Date();
    private int rootGroupId;
    private Boolean filterBotsOut;
    private String statType;

    @Autowired
    public ViewsRestController() {
        super(null);
    }

    @Override
    public void beforeSave(VisitsDTO entity) {
        throwError("datatables.error.recordIsNotEditable");
    }

    @Override
    public Page<VisitsDTO> getAllItems(Pageable pageable) {

        //Set default values
        Calendar cld = Calendar.getInstance();
        dateTo = new Date();
        rootGroupId = -1;
        filterBotsOut = false;
        statType = getRequest().getParameter("statType");

        //Default range for type months is 6 months, 1 month for the rest
        setDefaultDateRange(cld);

        List<VisitsDTO> items = getDataAndConvertIntoPageItems();

        DatatablePageImpl<VisitsDTO> page = new DatatablePageImpl<>(items);

        return page;
    }

    @Override
    public Page<VisitsDTO> searchItem(Map<String, String> params, Pageable pageable, VisitsDTO search) {

        String stringRange = "";
        String stringFrom = "";
        String stringTo = "";
        Calendar cld = Calendar.getInstance();

        //Get params from map and set then set global variables
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if(entry.getKey().equals("searchDayDate") || entry.getKey().equals("searchdayDate")) {
                stringRange = entry.getValue();
                stringRange = stringRange.substring("daterange:".length());
            } else if(entry.getKey().equals("searchRootDir")) {
                String rootGroupIdString = entry.getValue();
                rootGroupId = Tools.getIntValue(rootGroupIdString, -1);
            } else if(entry.getKey().equals("searchFilterBotsOut")) {
                String filterBotsOutString = entry.getValue();
                filterBotsOut = Boolean.parseBoolean(filterBotsOutString);
            } else if(entry.getKey().equals("statType")) {
                statType = entry.getValue();
            }
        }

        if(stringRange.contains("-")) {
            if(stringRange.startsWith("-")) {
                //We have only set dateTo
                stringTo = stringRange.split("-")[1];

                //First check if its valid string
                if(Tools.isEmpty(stringTo)) {
                    //String is no valid, set default range
                    setDefaultDateRange(cld);
                } else {
                    //set dateTo from value
                    cld.setTimeInMillis(Long.parseLong(stringTo));
                    dateTo = cld.getTime();

                    //set dateFrom range
                    setDefaultDateFrom(cld, stringTo);
                }
            } else {
                //We have set both dateFrom and dateTo
                stringFrom = stringRange.split("-")[0];
                cld.setTimeInMillis(Long.parseLong(stringFrom));
                dateFrom = cld.getTime();

                stringTo = stringRange.split("-")[1];
                cld.setTimeInMillis(Long.parseLong(stringTo));
                dateTo = cld.getTime();
            }
        } else {
            //We have set only dateFrom
            stringFrom = stringRange;

            //First check if its valid string
            if(Tools.isEmpty(stringFrom)) {
                 //String is no valid, set default range
                 setDefaultDateRange(cld);
            } else {
                cld.setTimeInMillis(Long.parseLong(stringFrom));
                dateFrom = cld.getTime();

                //dateTo we set to actual day
                dateTo = new Date();
            }
        }

        List<VisitsDTO> items =  getDataAndConvertIntoPageItems();
        DatatablePageImpl<VisitsDTO> page = new DatatablePageImpl<>(items);

        return page;
    }

    //If no date  is set, set default range
    //Range depends on statType
    private void setDefaultDateRange(Calendar cld) {
        if(statType != null && statType.equals("months")) {
            //If stat type is MONTHS, set range 6 months
            cld.add(Calendar.MONTH, -6);
            dateFrom = cld.getTime();
            dateTo = new Date();
        } else {
            //Else set range 1 month
            cld.add(Calendar.MONTH, -1);
            dateFrom = cld.getTime();
            dateTo = new Date();
        }
    }

    //If dateTo is set we need set dateFrom
    //Range depends on statType
    private void setDefaultDateFrom(Calendar cld, String stringTo) {
        if(statType != null && statType.equals("months")) {
            //If stat type is MONTHS, set range 6 months
            cld.setTimeInMillis(Long.parseLong(stringTo));
            cld.add(Calendar.MONTH, -6);
            dateFrom = cld.getTime();
        } else {
            //Else set range 1 month
            cld.setTimeInMillis(Long.parseLong(stringTo));
            cld.add(Calendar.MONTH, -1);
            dateFrom = cld.getTime();
        }
    }

    private List<VisitsDTO> getDataAndConvertIntoPageItems() {
        //If statType is not set, default option wil be statDays
        if(statType == null || statType.equals("days"))
            return convertToDaysPageItems(StatNewDB.getDayViews(dateFrom, dateTo, rootGroupId, filterBotsOut));
        else if(statType.equals("weeks"))
            return convertToWeeksPageItems(StatNewDB.getWeekViews(dateFrom, dateTo, rootGroupId, filterBotsOut));
        else if(statType.equals("months"))
            return convertToMonthsPageItems(StatNewDB.getMonthViews(dateFrom, dateTo, rootGroupId, filterBotsOut));
        else if(statType.equals("hours"))
            return convertToHoursPageItems(StatNewDB.getHours(dateFrom, dateTo, rootGroupId, filterBotsOut));

        return new ArrayList<>();
    }

    private List<VisitsDTO> convertToDaysPageItems(List<Column> viewStats) {
        List<VisitsDTO> pageItems = new ArrayList<>();
        int order = 1;

        for(Column viewStat : viewStats) {
            VisitsDTO item = new VisitsDTO();

            item.setOrder(order);
            item.setDayDate(viewStat.getDateColumn1());
            item.setVisits(viewStat.getIntColumn2());
            item.setSessions(viewStat.getIntColumn3());
            item.setUniqueUsers(viewStat.getIntColumn4());

            pageItems.add(item);
            order++;
        }

        return pageItems;
    }

    private List<VisitsDTO> convertToWeeksPageItems(List<Column> viewStats) {
        List<VisitsDTO> pageItems = new ArrayList<>();
        int order = 1;

        for(Column viewStat : viewStats) {
            VisitsDTO item = new VisitsDTO();

            item.setOrder(order);
            item.setYear(viewStat.getIntColumn1());
            item.setWeek(viewStat.getIntColumn2());
            item.setVisits(viewStat.getIntColumn3());
            item.setSessions(viewStat.getIntColumn4());
            item.setUniqueUsers(viewStat.getIntColumn5());

            //Compute date
            Calendar cld = Calendar.getInstance();
            cld.clear();
            cld.set(Calendar.YEAR, viewStat.getIntColumn1());
            cld.set(Calendar.WEEK_OF_YEAR, viewStat.getIntColumn2());
            item.setDayDate(cld.getTime());

            pageItems.add(item);
            order++;
        }

        return pageItems;
    }

    private List<VisitsDTO> convertToMonthsPageItems(List<Column> viewStats) {
        List<VisitsDTO> pageItems = new ArrayList<>();
        int order = 1;

        for(Column viewStat : viewStats) {
            VisitsDTO item = new VisitsDTO();

            item.setOrder(order);
            item.setYear(viewStat.getIntColumn1());
            item.setMonth(viewStat.getIntColumn2());
            item.setVisits(viewStat.getIntColumn3());
            item.setSessions(viewStat.getIntColumn4());
            item.setUniqueUsers(viewStat.getIntColumn5());

            //Compute date
            Calendar cld = Calendar.getInstance();
            cld.clear();
            cld.set(Calendar.YEAR, viewStat.getIntColumn1());
            cld.set(Calendar.MONTH, viewStat.getIntColumn2()-1);
            cld.set(Calendar.DAY_OF_MONTH, 15);
            item.setDayDate(cld.getTime());

            pageItems.add(item);
            order++;
        }

        return pageItems;
    }

    private List<VisitsDTO> convertToHoursPageItems(List<Column> viewStats) {
        List<VisitsDTO> pageItems = new ArrayList<>();
        int order = 1;

        for(Column viewStat : viewStats) {
            VisitsDTO item = new VisitsDTO();

            item.setOrder(order);
            item.setHour(viewStat.getIntColumn1());
            item.setVisits(viewStat.getIntColumn2());
            item.setSessions(viewStat.getIntColumn3());
            item.setUniqueUsers(viewStat.getIntColumn4());

            pageItems.add(item);
            order++;
        }

        return pageItems;
    }
}
