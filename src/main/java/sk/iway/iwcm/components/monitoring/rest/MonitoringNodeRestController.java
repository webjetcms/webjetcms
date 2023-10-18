package sk.iway.iwcm.components.monitoring.rest;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.components.monitoring.jpa.ExecutionEntry;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.datatable.NotifyBean.NotifyType;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;

@RestController
@Datatable
@RequestMapping(value = "/admin/rest/monitoring-node")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('cmp_server_monitoring')")
public class MonitoringNodeRestController extends DatatableRestControllerV2<ExecutionEntry, Long> {

    @Autowired
    public MonitoringNodeRestController() {
        super(null);
    }

    @Override
    public Page<ExecutionEntry> getAllItems(Pageable pageable) {
        MonitoringNodeService ms = new MonitoringNodeService(getRequest());
        List<ExecutionEntry> data = ms.getAll();
        DatatablePageImpl<ExecutionEntry> page = new DatatablePageImpl<>(data);

        Date lastUpdate = ms.getLastUpdate();
        if (lastUpdate != null) {
            NotifyBean notify = new NotifyBean(getProp().getText("monitoring.notify.lastUpdate.title"), getProp().getText("monitoring.notify.lastUpdate.text", Tools.formatDateTimeSeconds(lastUpdate)), NotifyType.INFO, 20000);
            addNotify(notify);
        }

        return page;
    }

    @RequestMapping(value="/all-nodes")
    @ResponseBody
    public List<String> getEnumerationTypes() {
        return MonitoringNodeService.getAllNodes();
    }

    @RequestMapping(value="/resetData", params={"showType", "selectedNode"})
    public void resetData(@RequestParam("showType") String showType, @RequestParam("selectedNode") String selectedNode) {
        MonitoringNodeService.resetData(showType, selectedNode);
    }

    @RequestMapping(value="/refreshData", params={"selectedNode"})
    public void refreshData(@RequestParam("selectedNode") String selectedNode) {
        MonitoringNodeService.refreshData(selectedNode);
    }

    //FE needs this info for timer and notification
    @RequestMapping(value="/clusterRefreshTime")
    public int getClusterRefreshTime() {
        return Constants.getInt("clusterRefreshTimeout");
    }
}
