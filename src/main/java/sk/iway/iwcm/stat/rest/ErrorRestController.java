package sk.iway.iwcm.stat.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.support.MutableSortDefinition;
import org.springframework.beans.support.PagedListHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Constants;
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

        List<ErrorDTO> items = columnsToItems(StatTableDB.getErrorPages(Constants.getInt("datatablesExportMaxRows", 1000), filter.getDateFrom(), filter.getDateTo(), filter.getUrl(), getRequest().getParameter("searchErrorMessage"), getRequest().getParameter("searchCount"), filter.getFilterBotsOut()));

        PagedListHolder<ErrorDTO> holder = new PagedListHolder<>(items);

        // Set the sort order on the PagedListHolder
        if (pageable.getSort() != null) {
            Sort sort = pageable.getSort();
            holder.setSort(new MutableSortDefinition(sort.iterator().next().getProperty(), true, sort.iterator().next().getDirection() == Sort.Direction.ASC));
            holder.resort();
        }

        holder.setPageSize(pageable.getPageSize());
        holder.setPage(pageable.getPageNumber());

        DatatablePageImpl<ErrorDTO> pageImpl = new DatatablePageImpl<>(holder.getPageList(), pageable, items.size());

        //set summary if needed
        long count = 0;
        for (ErrorDTO item : items) {
            count += item.getCount();
        }
        pageImpl.addSummary("count", count);

        return pageImpl;
    }

    @Override
    public Page<ErrorDTO> searchItem(Map<String, String> params, Pageable pageable, ErrorDTO search) {
        return getAllItems(pageable);
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
