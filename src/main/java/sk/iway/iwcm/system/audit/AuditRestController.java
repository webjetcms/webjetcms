package sk.iway.iwcm.system.audit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@Datatable
@RequestMapping(value = "/admin/rest/audit/log")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('cmp_adminlog')")
public class AuditRestController extends DatatableRestControllerV2<AuditLogEntity, Long> {

	private AuditRepository auditRepository;
	private AuditService auditService;

	@Autowired
	public AuditRestController(AuditRepository auditRepository, AuditService auditService) {
		super(auditRepository);
		this.auditRepository = auditRepository;
		this.auditService = auditService;
	}

	@Override
	public Page<AuditLogEntity> getAllItems(Pageable pageable) {
		DatatablePageImpl<AuditLogEntity> pages = new DatatablePageImpl<>(auditRepository.findAll(pageable));
		pages.addOptions("logType", auditService.getTypes(getRequest()), "label", "value", false);

		return pages;
	}

	@Override
	public AuditLogEntity editItem(AuditLogEntity entity, long logId) {
		throwError("datatables.error.recordIsNotEditable");
        return null;
	}

	@Override
    public boolean beforeDelete(AuditLogEntity entity) {
        return false;
    }
}
