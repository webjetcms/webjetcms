package sk.iway.iwcm.system.audit.rest;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.system.audit.jpa.LogFileBean;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

/**
 * Rest controller for log files.
 */
@RestController
@Datatable
@RequestMapping(value = "/admin/rest/audit/log-files")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('cmp_in-memory-logging')")
public class LogFilesRestController extends DatatableRestControllerV2<LogFileBean, Long> {

    @Autowired
    public LogFilesRestController() {
        super(null);
    }

    @Override
	public Page<LogFileBean> getAllItems(Pageable pageable) {
        return new DatatablePageImpl<>( LogLevelsService.getLogFiles() );
    }

    @GetMapping("/log-dir")
    public String getLogDir() {
        File logDir = new File(System.getProperty("catalina.base"),"logs");
        return logDir.getAbsolutePath();
    }
}
