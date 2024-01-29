package sk.iway.iwcm.components.calendar.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.calendar.jpa.CalendarTypesEntity;
import sk.iway.iwcm.components.calendar.jpa.CalendarTypesRepository;
import sk.iway.iwcm.components.users.userdetail.UserDetailsEntity;
import sk.iway.iwcm.components.users.userdetail.UserDetailsRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/calendar_types")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_calendar')")
@Datatable
public class CalendarTypesRestController extends DatatableRestControllerV2<CalendarTypesEntity, Long> {

    private final CalendarTypesRepository calendarTypesRepository;
    private final UserDetailsRepository userDetailsRepository;

    @Autowired
    public CalendarTypesRestController(CalendarTypesRepository calendarTypesRepository, UserDetailsRepository userDetailsRepository) {
        super(calendarTypesRepository);
        this.calendarTypesRepository = calendarTypesRepository;
        this.userDetailsRepository = userDetailsRepository;
    }

    @Override
    public Page<CalendarTypesEntity> getAllItems(Pageable pageable) {

        Page<CalendarTypesEntity> items =  calendarTypesRepository.findAllByDomainId(CloudToolsForCore.getDomainId(), pageable);

        //Create page
        DatatablePageImpl<CalendarTypesEntity> page = new DatatablePageImpl<>(items);

        List<UserDetailsEntity> approvers = userDetailsRepository.findAllByAdmin(true);
        //Add default value
        page.addOption("approverId", getProp().getText("calendar_type.approver_select.none"), "-1", false);
        for(UserDetailsEntity approver : approvers) {
            page.addOption("approverId", approver.getFirstName() + " " + approver.getLastName(), approver.getId().toString(), false);
        }

        return page;
    }

    @Override
    public CalendarTypesEntity getOneItem(long id) {
        CalendarTypesEntity entity;
        if(id == -1) {
            entity = new CalendarTypesEntity();
            entity.setApproverId(-1);
        } else
            entity = calendarTypesRepository.findByIdAndDomainId(id, CloudToolsForCore.getDomainId());

        return entity;
    }

	@Override
	public void beforeSave(CalendarTypesEntity entity) {
        //Set domain id, not null
        if(entity.getId() == null || entity.getId() == -1)
            entity.setDomainId(CloudToolsForCore.getDomainId());
	}

    @Override
    public void afterSave(CalendarTypesEntity entity, CalendarTypesEntity saved) {
        Cache c = Cache.getInstance();
        String cacheKey = "sk.iway.iwcm.calendar.EventTypeDB.domain=" + CloudToolsForCore.getDomainId();
        c.removeObject(cacheKey);
    }


}
