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

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.stat.Column;
import sk.iway.iwcm.stat.FilterHeaderDto;
import sk.iway.iwcm.stat.SessionDetails;
import sk.iway.iwcm.stat.SessionHolder;
import sk.iway.iwcm.stat.StatTableDB;
import sk.iway.iwcm.stat.jpa.ActualLogonUserDTO;
import sk.iway.iwcm.stat.jpa.LogonUserDTO;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/stat/logon-user")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_stat&menuUsers')")
@Datatable
public class LogonUserRestController extends DatatableRestControllerV2<LogonUserDTO, Long> {
    private static final int MAX_ROWS = 100;

    @Autowired
    public LogonUserRestController() {
        super(null);
    }

    @Override
    public void beforeSave(LogonUserDTO entity) {
        throwError("datatables.error.recordIsNotEditable");
    }

    @Override
    public Page<LogonUserDTO> getAllItems(Pageable pageable) {
        //Process params from request into FilterHeaderDto
        FilterHeaderDto filter = StatService.processRequestToStatFilter(getRequest(), null);

	    List<Column> columns = StatTableDB.getUsrlogon(MAX_ROWS, filter.getDateFrom(), filter.getDateTo());
        DatatablePageImpl<LogonUserDTO> page = new DatatablePageImpl<>(convertColumnsIntoItems(columns));
        return page;
    }

    @Override
    public Page<LogonUserDTO> searchItem(Map<String, String> params, Pageable pageable, LogonUserDTO search) {
        //Process received params into FilterHeaderDto
        FilterHeaderDto filter = StatService.processMapToStatFilter(params, null, getUser());
        Logger.debug(getClass(), "filter="+filter);

	    List<Column> columns = StatTableDB.getUsrlogon(MAX_ROWS, filter.getDateFrom(), filter.getDateTo());
        DatatablePageImpl<LogonUserDTO> page = new DatatablePageImpl<>(convertColumnsIntoItems(columns));
        return page;
    }

    private List<LogonUserDTO> convertColumnsIntoItems(List<Column> columns) {
        List<LogonUserDTO> items = new ArrayList<>();

        int order = 1;
        for(Column column : columns) {
            LogonUserDTO item = new LogonUserDTO();
            item.setOrder(order);

            item.setAdmin(column.isBooleanColumn1());
            item.setUserName(column.getColumn2());
            item.setCompany(column.getColumn3());
            item.setCity(column.getColumn4());
            item.setLogsCount(column.getIntColumn5());
            item.setLogonMinutes(column.getIntColumn6());
            item.setDayDate(column.getDateColumn1());
            item.setUserId(Integer.parseInt(column.getColumn1()));
            items.add(item);
            order++;
        }
        return items;
    }

    @RequestMapping(path="/actuals")
    public Page<ActualLogonUserDTO> getActualLogonUsers() {
        List<SessionDetails> sessionList = SessionHolder.getInstance().getList();
        DatatablePageImpl<ActualLogonUserDTO> page = new DatatablePageImpl<>(convertSessionsIntoItems(sessionList));
        return page;
    }

    private List<ActualLogonUserDTO> convertSessionsIntoItems(List<SessionDetails> sessionList) {
        List<ActualLogonUserDTO> items = new ArrayList<>();

        for(SessionDetails session : sessionList) {
            ActualLogonUserDTO item = new ActualLogonUserDTO();
            item.setLogonTime(session.getLogonTimeAsDate());
            item.setLastActivity(session.getLastActivityAsDate());
            item.setUserName(session.getLoggedUserName());
            item.setLastUrl(session.getLastURL());
            item.setUserIp(session.getRemoteAddr());
            item.setUserHost(Tools.getRemoteHost(session.getRemoteAddr()));
            item.setUserId(session.getLoggedUserId());
            item.setAdmin(session.isAdmin());

            items.add(item);
        }
        return items;
    }
}
