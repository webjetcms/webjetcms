package sk.iway.iwcm.system.audit.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.system.audit.jpa.LogLevelBean;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

/**
 * Rest controller for log levels. Conf. value logLevel and logLevels
 */
@RestController
@Datatable
@RequestMapping(value = "/admin/rest/audit/log-levels")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('cmp_adminlog_logging')")
public class LogLevelRestController extends DatatableRestControllerV2<LogLevelBean, Long> {

    @Autowired
    public LogLevelRestController() {
        super(null);
    }

    @Override
	public Page<LogLevelBean> getAllItems(Pageable pageable) {
        return new DatatablePageImpl<>( LogLevelsService.getLogLevelData(getMainLogLevelTitle()));
    }

    @Override
    public LogLevelBean editItem(LogLevelBean entity, long id) {
        setForceReload(true);
        return LogLevelsService.editItem(entity, id, getMainLogLevelTitle());
    }

    @Override
    public LogLevelBean insertItem(LogLevelBean entity) {
        setForceReload(true);
        return LogLevelsService.insertItem(entity, getMainLogLevelTitle());
    }

    @Override
    public boolean deleteItem(LogLevelBean entity, long id) {
        setForceReload(true);
        return LogLevelsService.deleteItem(entity, id, getMainLogLevelTitle());
    }

    @Override
    public void afterSave(LogLevelBean entity, LogLevelBean saved) {
        LogLevelsService.afterSave(entity, getMainLogLevelTitle());
    }

    private String getMainLogLevelTitle() {
        return getProp().getText("audit_log_level.main_log_level.js");
    }
}
