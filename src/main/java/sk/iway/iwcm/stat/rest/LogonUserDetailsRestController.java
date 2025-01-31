package sk.iway.iwcm.stat.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.stat.Column;
import sk.iway.iwcm.stat.FilterHeaderDto;
import sk.iway.iwcm.stat.StatTableDB;
import sk.iway.iwcm.stat.jpa.LogonUserDetailsDTO;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

@RestController
@RequestMapping("/admin/rest/stat/logon-user-details")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_stat&menuUsers')")
@Datatable
public class LogonUserDetailsRestController extends DatatableRestControllerV2<LogonUserDetailsDTO, Long> {
    private static final int MAX_ROWS = 100;

    @Autowired
    public LogonUserDetailsRestController() {
        super(null);
    }

    @Override
    public void beforeSave(LogonUserDetailsDTO entity) {
        throwError("datatables.error.recordIsNotEditable");
    }

    @Override
    public Page<LogonUserDetailsDTO> getAllItems(Pageable pageable) {
        //Process params from request into FilterHeaderDto
        FilterHeaderDto filter = StatService.processRequestToStatFilter(getRequest(), null);
        int userId = Tools.getIntValue(getRequest().getParameter("userId"), -1);

	    List<Column> columns = StatTableDB.getUsrlogonDetails(MAX_ROWS, userId, filter.getDateFrom(), filter.getDateTo());
        DatatablePageImpl<LogonUserDetailsDTO> page = new DatatablePageImpl<>(convertColumnsIntoItems(columns));
        return page;
    }

    @Override
    public Page<LogonUserDetailsDTO> searchItem(Map<String, String> params, Pageable pageable, LogonUserDetailsDTO search) {
        //Process received params into FilterHeaderDto
        FilterHeaderDto filter = StatService.processMapToStatFilter(params, null, getUser());
        int userId = Tools.getIntValue(getRequest().getParameter("userId"), -1);

        Logger.debug(getClass(), "filter="+filter);

	    List<Column> columns = StatTableDB.getUsrlogonDetails(MAX_ROWS, userId, filter.getDateFrom(), filter.getDateTo());
        DatatablePageImpl<LogonUserDetailsDTO> page = new DatatablePageImpl<>(convertColumnsIntoItems(columns));
        return page;
    }

    private List<LogonUserDetailsDTO> convertColumnsIntoItems(List<Column> columns) {
        List<LogonUserDetailsDTO> items = new ArrayList<>();

        int order = 1;
        for(Column column : columns) {
            LogonUserDetailsDTO item = new LogonUserDetailsDTO();
            item.setOrder(order);
            item.setDayDate(column.getDateColumn1());
            item.setLogonMinutes(column.getIntColumn2());
            item.setHostName(column.getColumn3());
            items.add(item);
            order++;
        }
        return items;
    }

    @RequestMapping(value="/user-name", params={"userId"}, produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String getUserName(@RequestParam("userId") int userId) {

        UserDetails user = UsersDB.getUser(userId);
        if (user != null) return user.getFullName();
        else return "User "+userId;

    }
}
