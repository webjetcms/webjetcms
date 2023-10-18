package sk.iway.iwcm.components.calendar.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.calendar.jpa.CalendarEventsEditorFields;
import sk.iway.iwcm.components.calendar.jpa.CalendarEventsEntity;
import sk.iway.iwcm.components.calendar.jpa.CalendarEventsRepository;
import sk.iway.iwcm.components.calendar.jpa.CalendarTypesEntity;
import sk.iway.iwcm.components.calendar.jpa.CalendarTypesRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.users.UserGroupDetails;
import sk.iway.iwcm.users.UserGroupsDB;

@RestController
@RequestMapping("/admin/rest/calendar_events")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_calendar')")
@Datatable
public class CalendarEventsRestController extends DatatableRestControllerV2<CalendarEventsEntity, Long> {

    private final CalendarEventsRepository calendarEventsRepository;
    private final CalendarTypesRepository calendarTypesRepository;

    @Autowired
    public CalendarEventsRestController(CalendarEventsRepository calendarEventsRepository, CalendarTypesRepository calendarTypesRepository) {
        super(calendarEventsRepository);
        this.calendarEventsRepository = calendarEventsRepository;
        this.calendarTypesRepository = calendarTypesRepository;
    }

    @Override
    public Page<CalendarEventsEntity> getAllItems(Pageable pageable) {
        Page<CalendarEventsEntity> items =  calendarEventsRepository.findAllByDomainId(CloudToolsForCore.getDomainId(), pageable);
        DatatablePageImpl<CalendarEventsEntity> page = new DatatablePageImpl<>(items);

        processFromEntity(page, ProcessItemAction.GETALL);

        List<CalendarTypesEntity> calendarTypes = calendarTypesRepository.findAllByDomainId(CloudToolsForCore.getDomainId());
        for(CalendarTypesEntity type : calendarTypes) {
            page.addOption("typeId", type.getName(), type.getId().toString(), false);
        }

        page.addOptions("editorFields.notifyEmailsUserGroups", UserGroupsDB.getInstance().getUserGroupsByTypeId(UserGroupDetails.TYPE_PERMS), "userGroupName", "userGroupId", false);
        page.addOptions("editorFields.notifyEmailsUserGroups", UserGroupsDB.getInstance().getUserGroupsByTypeId(UserGroupDetails.TYPE_EMAIL), "userGroupName", "userGroupId", false);

        return page;
    }

    @Override
    public CalendarEventsEntity getOneItem(long id) {
        CalendarEventsEntity entity;
        if(id == -1) entity = new CalendarEventsEntity();
        else entity = calendarEventsRepository.findByIdAndDomainId(id, CloudToolsForCore.getDomainId());

        processFromEntity(entity, ProcessItemAction.CREATE);

        return entity;
    }

    @Override
    public CalendarEventsEntity processFromEntity(CalendarEventsEntity entity, ProcessItemAction action) {
        if(entity != null && entity.getEditorFields() == null) {
            CalendarEventsEditorFields ceef = new CalendarEventsEditorFields();
            ceef.fromCalendarEventsEntity(entity, getRequest(), getProp());

            //nastav volne polia
            ceef.setFieldsDefinition(ceef.getFields(entity, "calendar_events", 'E'));
        }

        return entity;
    }

    @Override
    public CalendarEventsEntity processToEntity(CalendarEventsEntity entity, ProcessItemAction action) {
        if(entity != null) {
            //Call toUserDetailsEntity to set new entity values from EditorFields
            CalendarEventsEditorFields ceef = entity.getEditorFields();
            ceef.toCalendarEventsEntity(entity, calendarTypesRepository, getProp(), calendarEventsRepository, getRequest());
        }

        return entity;
    }
}
