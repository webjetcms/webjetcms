package sk.iway.iwcm.editor.rest;

import java.util.List;
import java.util.Map;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@Datatable
@RequestMapping(value = "/admin/rest/group-scheduler")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('menuWebpages')")
public class GroupSchedulerRestController extends DatatableRestControllerV2<GroupSchedulerDto, Long>{

    private final GroupSchedulerDtoRepository repository;

    public GroupSchedulerRestController(GroupSchedulerDtoRepository repository) {
        super(repository);
        this.repository = repository;
    }

    @Override
    public Page<GroupSchedulerDto> getAllItems(Pageable pageable) {

        String selectType = getRequest().getParameter("selectType");
        DatatablePageImpl<GroupSchedulerDto> page = null;
        Long groupId = Long.parseLong(getRequest().getParameter("groupId"));

        //if selectType is "plannedChanges", get all records by groupId and whenToPublish is NOT NULL
        //else if selectType is "changeHistory", get all records by groupId and whenToPublish is less that actual Date or is NULL
        if(selectType.equals("plannedChanges")) {
            page = new DatatablePageImpl<>(repository.findAllByGroupIdAndWhenToPublishIsNotNull(pageable, groupId));
            return page;
        } else if(selectType.equals("changeHistory")) {
            page = new DatatablePageImpl<>(repository.findAllByGroupIdAndWhenToPublishIsNull(pageable, groupId));
            return page;
        }

        return null;
    }

    @Override
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<GroupSchedulerDto> root, CriteriaBuilder builder) {
        //vyhladaj podla searchUserFullName
        super.addSpecSearch(params, predicates, root, builder);

        String selectType = getRequest().getParameter("selectType");
        if(selectType.equals("plannedChanges")) predicates.add(builder.isNotNull(root.get("whenToPublish")));
        else predicates.add(builder.isNull(root.get("whenToPublish")));
    }

    @Override
	public void beforeSave(GroupSchedulerDto entity) {
		throwError("datatables.error.recordIsNotEditable");
	}

	@Override
    public boolean beforeDelete(GroupSchedulerDto entity) {
		//zmazat je mozne len planovany zaznam v buducnosti
		if (entity.getId()>0 && entity.getWhenToPublish()!=null && entity.getWhenToPublish().getTime()>Tools.getNow()) return true;

		throwError("admin.cong_editor.youCanOnlyDeleteFutureRecords");
		return false;
    }
}
