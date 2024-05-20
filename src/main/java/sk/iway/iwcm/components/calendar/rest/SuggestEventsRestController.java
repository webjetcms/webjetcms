package sk.iway.iwcm.components.calendar.rest;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.calendar.jpa.CalendarTypesEntity;
import sk.iway.iwcm.components.calendar.jpa.CalendarTypesRepository;
import sk.iway.iwcm.components.calendar.jpa.SuggestEventEntity;
import sk.iway.iwcm.components.calendar.jpa.SuggestEventsRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.datatable.NotifyBean.NotifyType;

@RestController
@RequestMapping("/admin/rest/calendar-suggest-events")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_calendar')")
@Datatable
public class SuggestEventsRestController extends DatatableRestControllerV2<SuggestEventEntity, Long> {

    private final SuggestEventsRepository suggestEventsRepository;
    private final CalendarTypesRepository calendarTypesRepository;

    @Autowired
    public SuggestEventsRestController(SuggestEventsRepository suggestEventsRepository, CalendarTypesRepository calendarTypesRepository) {
        super(suggestEventsRepository);
        this.suggestEventsRepository = suggestEventsRepository;
        this.calendarTypesRepository = calendarTypesRepository;
    }

    @Override
    public Page<SuggestEventEntity> getAllItems(Pageable pageable) {
        DatatablePageImpl<SuggestEventEntity> page = new DatatablePageImpl<>( super.getAllItemsIncludeSpecSearch(new SuggestEventEntity(), pageable) );

        List<CalendarTypesEntity> calendarTypes = calendarTypesRepository.findAllByDomainId(CloudToolsForCore.getDomainId());
        for(CalendarTypesEntity type : calendarTypes) {
            page.addOption("typeId", type.getName(), type.getId().toString(), false);
        }

        return page;
    }

    @Override
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<SuggestEventEntity> root, CriteriaBuilder builder) {
        super.addSpecSearch(params, predicates, root, builder);

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        predicates.add(builder.greaterThanOrEqualTo(root.get("dateFrom"), cal.getTime())); //Only future events

        predicates.add(builder.equal(root.get("approve"), 1)); //Only approved events
    }

    //Needed for DatatableRestControllerV2 -> when calling action from FE
    @Override
    public SuggestEventEntity getOneItem(long id) {
        if(id < 1) throwError("");

        return suggestEventsRepository.findFirstByIdAndDomainId(id, CloudToolsForCore.getDomainId()).orElse(null);
    }

    @Override
    public boolean processAction(SuggestEventEntity entity, String action) {
        if ("suggestEvent".equals(action)) {
            //Approve
            int updatedRow = suggestEventsRepository.updateSuggest(entity.getId(), true, CloudToolsForCore.getDomainId());

            if(updatedRow == 1) {
                addNotify(new NotifyBean(getProp().getText("calendar.suggest_event_action"), getProp().getText("calendar.suggest_event_action_success", entity.getTitle()), NotifyType.SUCCESS, 15000));
            } else {
                addNotify(new NotifyBean(getProp().getText("calendar.suggest_event_action"), getProp().getText("calendar.suggest_event_action_failed", entity.getTitle()), NotifyType.ERROR, 15000));
            }

            return true;
        } else if ("notSuggestEvent".equals(action)) {
            //Reject (dis-approve)
            int updatedRow = suggestEventsRepository.updateSuggest(entity.getId(), false, CloudToolsForCore.getDomainId());

            if(updatedRow == 1) {
                addNotify(new NotifyBean(getProp().getText("calendar.not_suggest_event_action"), getProp().getText("calendar.not_suggest_event_action_success", entity.getTitle()), NotifyType.SUCCESS, 15000));
            } else {
                addNotify(new NotifyBean(getProp().getText("calendar.not_suggest_event_action"), getProp().getText("calendar.not_suggest_event_action_failed", entity.getTitle()), NotifyType.ERROR, 15000));
            }

            return true;
        }

        //Uknown action
        return false;
    }
}