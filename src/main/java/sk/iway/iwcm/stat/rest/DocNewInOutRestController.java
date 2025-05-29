package sk.iway.iwcm.stat.rest;

import java.util.ArrayList;
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
import sk.iway.iwcm.stat.jpa.InOutDTO;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/stat/doc-new-in-out")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_stat')")
@Datatable
public class DocNewInOutRestController extends DatatableRestControllerV2<InOutDTO, Long> {

    @Autowired
    public DocNewInOutRestController() {
        super(null);
    }

    @Override
    public void beforeSave(InOutDTO entity) {
        throwError("datatables.error.recordIsNotEditable");
    }

    @Override
    public Page<InOutDTO> findByColumns(Map<String, String> params, Pageable pageable, InOutDTO search) {
        String type = Tools.getStringValue(getRequest().getParameter("type"), "");
        Integer docId = Tools.getIntValue(getRequest().getParameter("docId"), -1);
        String dayDate = Tools.getStringValue(getRequest().getParameter("dayDate"), "");
        Date[] dateRangeArr = StatService.processDateRangeString(dayDate);

        List<Column> stats = new ArrayList<>();
        if("incoming".equals(type)) {
            stats = StatNewDB.getIncomingStats(docId, dateRangeArr[0], dateRangeArr[1], null, getRequest());
        } else if("outgoing".equals(type)) {
            stats = StatNewDB.getOutgoingStats(docId, dateRangeArr[0], dateRangeArr[1]);
        } else {
            // Nothing, INVALID type
        }

        return new DatatablePageImpl<>(columnsToInOutItems(stats));
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
}
