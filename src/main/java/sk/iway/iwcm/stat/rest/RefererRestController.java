package sk.iway.iwcm.stat.rest;

import java.util.ArrayList;
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
import sk.iway.iwcm.stat.jpa.RefererDTO;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/stat/referer")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_stat')")
@Datatable
public class RefererRestController extends DatatableRestControllerV2<RefererDTO, Long> {
    private static final int MAX_ROWS = 100;

    @Autowired
    public RefererRestController() {
        super(null);
    }

    @Override
    public void beforeSave(RefererDTO entity) {
        throwError("datatables.error.recordIsNotEditable");
    }

    @Override
    public Page<RefererDTO> getAllItems(Pageable pageable) {
        //Process params from request into FilterHeaderDto
        DatatablePageImpl<RefererDTO> page = new DatatablePageImpl<>(getDataAndConvertIntoItems());
        return page;
    }

    @Override
    public Page<RefererDTO> searchItem(Map<String, String> params, Pageable pageable, RefererDTO search) {
        //Process received params into FilterHeaderDto
        DatatablePageImpl<RefererDTO> page = new DatatablePageImpl<>(getDataAndConvertIntoItems());
        return page;
    }

    private List<RefererDTO> getDataAndConvertIntoItems() {
        FilterHeaderDto filter = StatService.processRequestToStatFilter(getRequest(), "searchdayDate");
        List<RefererDTO> items = new ArrayList<>();
        int maxRows = MAX_ROWS;
        if (getRequest().getParameter("searchChartType")==null) maxRows = 5;
        List<Column> columns = StatNewDB.getStatReferer(filter.getDateFrom(), filter.getDateTo(), maxRows, filter.getRootGroupIdQuery());

        //Compute sum of all visits
        Double sum = 0.0;
        for(Column column : columns) {
            sum += column.getIntColumn2();
        }

        if (sum > 0) {
            int order = 1;
            for(Column column : columns) {
                RefererDTO item = new RefererDTO();
                item.setOrder(order);
                item.setServerName(column.getColumn1());
                item.setVisits(column.getIntColumn2());
                item.setPercentage(item.getVisits() * 100.0 / sum);
                items.add(item);
                order++;
            }
        }
        return items;
    }
}