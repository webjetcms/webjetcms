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
import sk.iway.iwcm.stat.jpa.SearchEnginesDetailsDTO;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/stat/search-engines-details")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_stat')")
@Datatable
public class SearchEnginesDetailsRestController extends DatatableRestControllerV2<SearchEnginesDetailsDTO, Long> {

    @Autowired
    public SearchEnginesDetailsRestController() {
        super(null);
    }

    @Override
    public void beforeSave(SearchEnginesDetailsDTO entity) {
        throwError("datatables.error.recordIsNotEditable");
    }

    @Override
    public Page<SearchEnginesDetailsDTO> getAllItems(Pageable pageable) {
        DatatablePageImpl<SearchEnginesDetailsDTO> page = new DatatablePageImpl<>(convertColumnsIntoItems());
        return page;
    }

    @Override
    public Page<SearchEnginesDetailsDTO> searchItem(Map<String, String> params, Pageable pageable, SearchEnginesDetailsDTO search) {
        //Process received params into FilterHeaderDto
        DatatablePageImpl<SearchEnginesDetailsDTO> page = new DatatablePageImpl<>(convertColumnsIntoItems());
        return page;
    }

    private List<SearchEnginesDetailsDTO> convertColumnsIntoItems() {

        FilterHeaderDto filter = StatService.processRequestToStatFilter(getRequest(), "searchdayDate");
        List<Column> columns = StatTableDB.getQueries(filter.getDateFrom(), filter.getDateTo(), filter.getUrl());

        List<SearchEnginesDetailsDTO> items = new ArrayList<>();

        int order = 1;
        for(Column column : columns) {
            SearchEnginesDetailsDTO item = new SearchEnginesDetailsDTO();
            item.setOrder(order);
            item.setDayDate(column.getDateColumn1());
            item.setServerName(column.getColumn1());
            item.setName(column.getColumn2());
            item.setRemoteHost(column.getColumn3());
            items.add(item);
            order++;
        }
        return items;
    }
}
