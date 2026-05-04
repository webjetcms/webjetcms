package sk.iway.iwcm.components.calendar.rest;

import java.util.List;
import java.util.Map;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.calendar.jpa.CalendarTypesEntity;
import sk.iway.iwcm.components.calendar.jpa.CalendarTypesRepository;
import sk.iway.iwcm.components.calendar.jpa.NonApprovedEventEntity;
import sk.iway.iwcm.components.calendar.jpa.NonApprovedEventsRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.datatable.NotifyBean.NotifyType;

@RestController
@RequestMapping("/admin/rest/calendar/non-approved-events")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_calendar')")
@Datatable
public class NonApprovedEventsRestController extends DatatableRestControllerV2<NonApprovedEventEntity, Long> {

    private final NonApprovedEventsRepository nonApprovedEventsRepository;
    private final CalendarTypesRepository calendarTypesRepository;
    private static final String TYPE_ID = "typeId";

    @Autowired
    public NonApprovedEventsRestController(NonApprovedEventsRepository nonApprovedEventsRepository, CalendarTypesRepository calendarTypesRepository) {
        super(nonApprovedEventsRepository);
        this.nonApprovedEventsRepository = nonApprovedEventsRepository;
        this.calendarTypesRepository = calendarTypesRepository;
    }

    @Override
    public Page<NonApprovedEventEntity> getAllItems(Pageable pageable) {
        DatatablePageImpl<NonApprovedEventEntity> page = new DatatablePageImpl<>( super.getAllItemsIncludeSpecSearch(new NonApprovedEventEntity(), pageable) );

        List<CalendarTypesEntity> calendarTypes = calendarTypesRepository.findAllByDomainId(CloudToolsForCore.getDomainId());
        for(CalendarTypesEntity type : calendarTypes) {
            page.addOption(TYPE_ID, type.getName(), type.getId().toString(), false);
        }

        return page;
    }

    @Override
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<NonApprovedEventEntity> root, CriteriaBuilder builder) {
        super.addSpecSearch(params, predicates, root, builder);

        List<Integer> approvingTypes = calendarTypesRepository.getTypeIdsByApproverAndDomain(getUser().getUserId(), CloudToolsForCore.getDomainId());

        if(approvingTypes.isEmpty()) //Set unreal value -> return nothing
            predicates.add(builder.equal(root.get(TYPE_ID), -1));
        else {
            predicates.add(root.get(TYPE_ID).in(approvingTypes));
            predicates.add(builder.equal(root.get("approve"), -1)); //Approve = -1 -> waiting for approval
        }
    }

    //Needed for DatatableRestControllerV2 -> when calling action from FE
    @Override
    public NonApprovedEventEntity getOneItem(long id) {
        if(id < 1) throwError("");

        return nonApprovedEventsRepository.findFirstByIdAndDomainId(id, CloudToolsForCore.getDomainId()).orElse(null);
    }

    @Override
    public boolean processAction(NonApprovedEventEntity entity, String action) {
        if ("approveEvent".equals(action)) {
            //Approve
            int updatedRow = nonApprovedEventsRepository.updateApprove(entity.getId(), 1, CloudToolsForCore.getDomainId());

            if(updatedRow == 1) {
                addNotify(new NotifyBean(getProp().getText("calendar.approve_event_action"), getProp().getText("calendar.approve_event_action_success", entity.getTitle()), NotifyType.SUCCESS, 15000));
            } else {
                addNotify(new NotifyBean(getProp().getText("calendar.approve_event_action"), getProp().getText("calendar.approve_event_action_failed", entity.getTitle()), NotifyType.ERROR, 15000));
            }

            return true;
        } else if ("rejectEvent".equals(action)) {
            //Reject (dis-approve)
            int updatedRow = nonApprovedEventsRepository.updateApprove(entity.getId(), 0, CloudToolsForCore.getDomainId());

            if(updatedRow == 1) {
                addNotify(new NotifyBean(getProp().getText("calendar.disapprove_event_action"), getProp().getText("calendar.disapprove_event_action_success", entity.getTitle()), NotifyType.SUCCESS, 15000));
            } else {
                addNotify(new NotifyBean(getProp().getText("calendar.disapprove_event_action"), getProp().getText("calendar.disapprove_event_action_failed", entity.getTitle()), NotifyType.ERROR, 15000));
            }

            return true;
        }

        //Uknown action
        return false;
    }
}
