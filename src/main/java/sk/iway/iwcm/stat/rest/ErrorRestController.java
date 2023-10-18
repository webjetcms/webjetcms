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
import sk.iway.iwcm.stat.StatTableDB;
import sk.iway.iwcm.stat.jpa.ErrorDTO;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/stat/error")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_stat')")
@Datatable
public class ErrorRestController extends DatatableRestControllerV2<ErrorDTO, Long> {
    private static final int MAX_ROWS = 1000;

    @Autowired
    public ErrorRestController() {
        super(null);
    }

    @Override
    public void beforeSave(ErrorDTO entity) {
        throwError("datatables.error.recordIsNotEditable");
    }

    @Override
    public Page<ErrorDTO> getAllItems(Pageable pageable) {
        //Process params from request into FilterHeaderDto
        FilterHeaderDto filter = StatService.processRequestToStatFilter(getRequest(), null);

        List<ErrorDTO> items = columnsToItems(StatTableDB.getErrorPages(MAX_ROWS, filter.getDateFrom(), filter.getDateTo(), filter.getUrl()));
        DatatablePageImpl<ErrorDTO> page = new DatatablePageImpl<>(items);
        return page;
    }

    @Override
    public Page<ErrorDTO> searchItem(Map<String, String> params, Pageable pageable, ErrorDTO search) {
        //Process received params into FilterHeaderDto
        FilterHeaderDto filter = StatService.processMapToStatFilter(params, null);

        List<ErrorDTO> items = columnsToItems(StatTableDB.getErrorPages(MAX_ROWS, filter.getDateFrom(), filter.getDateTo(), filter.getUrl()));
        DatatablePageImpl<ErrorDTO> page = new DatatablePageImpl<>(items);
        return page;
    }

    private List<ErrorDTO> columnsToItems(List<Column> columns) {
        List<ErrorDTO> items = new ArrayList<>();

        int order = 1;
        for(Column column : columns) {
            ErrorDTO item = new ErrorDTO();
            item.setOrder(order);
            item.setYear(column.getIntColumn1());
            item.setWeek(column.getIntColumn2());
            item.setUrl(column.getColumn3());
            item.setCount(column.getIntColumn5());
            if(column.getColumn4().equals(""))
                item.setErrorMessage("&nbsp;");
            else
                item.setErrorMessage(column.getColumn4());
            items.add(item);
            order++;
        }
        return items;
    }
}
