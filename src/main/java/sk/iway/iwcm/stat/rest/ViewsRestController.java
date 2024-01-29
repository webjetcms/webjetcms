package sk.iway.iwcm.stat.rest;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.stat.Column;
import sk.iway.iwcm.stat.FilterHeaderDto;
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

    private FilterHeaderDto filter;

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
        //Process params from request into FilterHeaderDto
        filter = StatService.processRequestToStatFilter(getRequest(), null);
        List<VisitsDTO> items = getDataAndConvertIntoPageItems();
        DatatablePageImpl<VisitsDTO> page = new DatatablePageImpl<>(items);
        return page;
    }

    @Override
    public Page<VisitsDTO> searchItem(Map<String, String> params, Pageable pageable, VisitsDTO search) {
        //Process received params into FilterHeaderDto
        filter = StatService.processMapToStatFilter(params, null);

        DatatablePageImpl<VisitsDTO> page = new DatatablePageImpl<>( getDataAndConvertIntoPageItems() );
        return page;
    }

    private List<VisitsDTO> getDataAndConvertIntoPageItems() {
        //If statType is not set, default option wil be statDays
        if(filter.getStatType() == null || "days".equals( filter.getStatType() ))
            return convertToDaysPageItems(StatNewDB.getDayViews(filter.getDateFrom(), filter.getDateTo(), filter.getRootGroupId(), filter.getFilterBotsOut()));
        else if("weeks".equals( filter.getStatType() ))
            return convertToWeeksPageItems(StatNewDB.getWeekViews(filter.getDateFrom(), filter.getDateTo(), filter.getRootGroupId(), filter.getFilterBotsOut()));
        else if("months".equals( filter.getStatType() ))
            return convertToMonthsPageItems(StatNewDB.getMonthViews(filter.getDateFrom(), filter.getDateTo(), filter.getRootGroupId(), filter.getFilterBotsOut()));
        else if("hours".equals( filter.getStatType() ))
            return convertToHoursPageItems(StatNewDB.getHours(filter.getDateFrom(), filter.getDateTo(), filter.getRootGroupId(), filter.getFilterBotsOut()));

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
