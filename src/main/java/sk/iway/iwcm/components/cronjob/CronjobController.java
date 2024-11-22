package sk.iway.iwcm.components.cronjob;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.system.cron.CronDB;
import sk.iway.iwcm.system.cron.CronFacade;
import sk.iway.iwcm.system.cron.CronTask;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.datatable.NotifyBean.NotifyType;

@RestController
@Datatable
@RequestMapping(value = "/admin/rest/settings/cronjob")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('cmp_crontab')")
public class CronjobController extends DatatableRestControllerV2<CronTask, Long> {

    private final CronjobService cronjobService;

    @Autowired
    public CronjobController(CronjobService cronjobService) {
        super(null);
        this.cronjobService = cronjobService;
    }

    @Override
    public Page<CronTask> getAllItems(Pageable pageable) {
        List<CronTask> tasks = cronjobService.getCronTasks();
        return new DatatablePageImpl<>(tasks);
    }

    @Override
    public CronTask insertItem(CronTask entity) {
        return cronjobService.saveCronTask(entity);
    }

    @Override
    public CronTask editItem(CronTask entity, long id) {
        return cronjobService.editCronTask(entity, id);
    }

    @Override
    public boolean deleteItem(CronTask entity, long id) {
        return cronjobService.deleteCronTask(id);
    }

    @Override
    public CronTask getOneItem(long id) {
        return CronDB.getById(id);
    }

    @Override
    public boolean processAction(CronTask entity, String action) {
        if ("play".equals(action)) {
            try {
                CronFacade.getInstance().runSimpleTaskOnce(entity);
                Adminlog.add(Adminlog.TYPE_CRON, getProp().getText("admin.crontab.task_launched", entity.getTask()), -1, -1);
                addNotify(new NotifyBean(getProp().getText("admin.crontab.view"), getProp().getText("admin.crontab.task_launched", entity.getTask()), NotifyType.SUCCESS, 15000));
                return true;
            } catch (ClassNotFoundException e) {
                Logger.error(CronjobController.class, e);
            }
        }
        return false;
    }

    @Override
    public void getOptions(DatatablePageImpl<CronTask> page) {
        page.addOption("clusterNode", "all", "all", false);
        page.addOptions("clusterNode", ClusterDB.getClusterNodeNamesExpandedAuto(), null, null, false);
    }

    @Override
    public void afterSave(CronTask entity, CronTask saved) {
        //restart cron
        ClusterDB.addRefresh(CronFacade.class);
		CronFacade.getInstance(true);
    }

    @Override
    public void afterDelete(CronTask entity, long id) {
        afterSave(entity, null);
    }

}
