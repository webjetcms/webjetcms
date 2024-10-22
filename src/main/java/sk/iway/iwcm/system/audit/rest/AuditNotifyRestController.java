package sk.iway.iwcm.system.audit.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.system.audit.jpa.AuditNotifyEntity;
import sk.iway.iwcm.system.audit.jpa.AuditNotifyRepository;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.system.adminlog.AdminlogNotifyManager;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@Datatable
@RequestMapping(value = "/admin/rest/audit/notify")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('cmp_adminlog')")
public class AuditNotifyRestController extends DatatableRestControllerV2<AuditNotifyEntity, Long> {

	private AuditNotifyRepository auditNotifyRepository;
	private AuditService auditService;

	@Autowired
	public AuditNotifyRestController(AuditNotifyRepository auditNotifyRepository, AuditService auditService) {
		super(auditNotifyRepository);
		this.auditNotifyRepository = auditNotifyRepository;
		this.auditService = auditService;
	}

	@Override
	public Page<AuditNotifyEntity> getAllItems(Pageable pageable) {
		DatatablePageImpl<AuditNotifyEntity> pages = new DatatablePageImpl<>(auditNotifyRepository.findAll(pageable));
		pages.addOptions("adminlogType", auditService.getTypes(getRequest()), "label", "value", false);

		return pages;
	}

	@Override
	public boolean beforeDelete(AuditNotifyEntity entity) {
		beforeSave(entity);
		return true;
	}

	@Override
	public void beforeSave(AuditNotifyEntity entity) {
		//we better refresh the cache because of deadlock in MS SQL
		AdminlogNotifyManager.getNotifyEmails(Adminlog.TYPE_ADMINLOG_NOTIFY);
	}

	@Override
	public void afterDelete(AuditNotifyEntity entity, long id) {
		afterSave(entity, null);
		Cache c = Cache.getInstance();
    	c.removeObjectStartsWithName("AdminlogNotifyEmails.");
	}

	@Override
	public void afterDuplicate(AuditNotifyEntity entity, Long originalId) {
		afterSave(entity, null);
	}

	@Override
	public void afterSave(AuditNotifyEntity entity, AuditNotifyEntity saved) {
		AdminlogNotifyManager.clearCache();
		Cache c = Cache.getInstance();
    	c.removeObjectStartsWithName("AdminlogNotifyEmails.");
	}
}
