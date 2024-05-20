package sk.iway.iwcm.components.calendar.jpa;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UserGroupDetails;
import sk.iway.iwcm.users.UserGroupsDB;
import sk.iway.iwcm.users.UsersDB;

@Getter
@Setter
public class CalendarEventsEditorFields extends BaseEditorFields implements Serializable {

    public CalendarEventsEditorFields(){
        //constructor
    }

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="calendar.creator",
        sortAfter = "typeId",
        hiddenEditor = true
    )
    private String creator;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="calendar.stav",
        hiddenEditor = true
    )
    private String condition;

    @DataTableColumn(
        inputType = DataTableColumnType.CHECKBOX,
        title="calendar_edit.notify_emails",
        tab = "notification",
        sortAfter = "notifyHoursBefore",
        hidden = true,
        editor = {
        @DataTableColumnEditor(attr = {
            @DataTableColumnEditorAttr(key = "unselectedValue", value = "") }) })
    private Integer[] notifyEmailsUserGroups;

    public void fromCalendarEventsEntity(CalendarEventsEntity originalEntity, HttpServletRequest request, Prop prop) {

        if(originalEntity.getId() == null || originalEntity.getId() == -1) {
            originalEntity.setDateFrom(new Date());
            originalEntity.setDateTo(new Date());
            originalEntity.setNotifyIntrotext(prop.getText("calendar_edit.notify_introtext_default"));

            Identity loggedUser = UsersDB.getCurrentUser(request);
            originalEntity.setNotifySender(loggedUser.getEmail());
            originalEntity.setCreatorId(loggedUser.getUserId());
        } else {
            int approve = originalEntity.getApprove();
            String approveText = "";
            if(approve == -1)
                approveText = prop.getText("calendar.cakana_na_schvalenie");
            else if(approve == 0)
                approveText = prop.getText("calendar.neschvalena");
            else if(approve == 1)
                approveText = prop.getText("calendar.schvalena");
            setCondition(approveText);

            UserDetails creatorUser = UsersDB.getUser(originalEntity.getCreatorId());
            setCreator(creatorUser.getFullName());
        }

        if(originalEntity.getNotifyEmails() != null && !originalEntity.getNotifyEmails().isEmpty()) {
            UserGroupsDB ugdb = UserGroupsDB.getInstance();
            List<UserGroupDetails> userGroups = ugdb.getUserGroups();

            String[] userGroupsNames =  originalEntity.getNotifyEmails().split(",");

            List<Integer> selectedUsergroupIds = new ArrayList<>();
            for(UserGroupDetails userGroup : userGroups)
                for(String groupname : userGroupsNames)
                    if(groupname.equals(userGroup.getUserGroupName())) {
                        selectedUsergroupIds.add(userGroup.getUserGroupId());
                        break;
                    }
            notifyEmailsUserGroups = selectedUsergroupIds.toArray(new Integer[0]);
        }

        originalEntity.setEditorFields(this);
    }

    public void toCalendarEventsEntity(CalendarEventsEntity originalEntity, CalendarTypesRepository ctr, Prop prop, CalendarEventsRepository cer, HttpServletRequest request, ProcessItemAction action) {

        StringBuilder notifySendertext = new StringBuilder();
        UserGroupsDB ugdb = UserGroupsDB.getInstance();
        for(Integer userGroupId : notifyEmailsUserGroups) {
            if(userGroupId != null) {
                String userGroupName = ugdb.getUserGroupName(userGroupId);
                notifySendertext.append(userGroupName).append(",");
            }
        }
        originalEntity.setNotifyEmails(notifySendertext.toString());

        //Set domainId - if create
        if(action == ProcessItemAction.CREATE) originalEntity.setDomainId(CloudToolsForCore.getDomainId());

        boolean newEntity = false;
        boolean changedType = false;

        //Check if entity is new
        if(originalEntity.getId() == null || originalEntity.getId() == -1) newEntity = true;

        //Check if type was changed
        CalendarEventsEntity fromDB = null;
        if (originalEntity.getId()!=null) fromDB = cer.findFirstByIdAndDomainId(originalEntity.getId(), CloudToolsForCore.getDomainId()).orElse(null);
        if(fromDB != null && fromDB.getTypeId().equals(originalEntity.getTypeId())==false) changedType = true;

        if(newEntity || changedType) {
            //Get selected calendar type
            CalendarTypesEntity cte = ctr.findFirstByIdAndDomainId(Long.valueOf(originalEntity.getTypeId()), CloudToolsForCore.getDomainId()).orElse(null);
            Integer approverId = cte.getApproverId();
            UserDetails loggedUser = UsersDB.getUser(originalEntity.getCreatorId());

            if(approverId == null || approverId == -1) {
                //Approver is not set, approve this event
                originalEntity.setApprove(1);
            } else if(approverId == loggedUser.getUserId()) {
                //Approver and creator are same person, approve event
                originalEntity.setApprove(1);
            } else {
                //Approver and creator are NOT same person, wait for approve
                originalEntity.setApprove(-1);

                sendMailToApprover(originalEntity, approverId, loggedUser, prop, request);
            }
        }

        if(newEntity) originalEntity.setSuggest(false);
    }

    private void sendMailToApprover(CalendarEventsEntity originalEntity, Integer approverId, UserDetails creator, Prop prop, HttpServletRequest request) {
        //UserDetails approver = UsersDB.getUser(approverId);

        String eventDate = "";
        if(originalEntity.getDateFrom() != null)
            eventDate = "" + originalEntity.getDateFrom();
        if(originalEntity.getDateTo() != null)
            eventDate +=  " - " + originalEntity.getDateTo();
        if(originalEntity.getTimeRange() != null)
            eventDate += " o " + originalEntity.getTimeRange();

        StringBuilder emailClientData=new StringBuilder();
        emailClientData.append("<html><head>");
        emailClientData.append("<style>");
        emailClientData.append("body{");
        emailClientData.append("font-family: Arial;");
        emailClientData.append("font-size: 11pt;");
        emailClientData.append('}');
        emailClientData.append("</style></head><body>");
        emailClientData.append(prop.getText("calendar.vytvoril_akciu", creator.getFullName(), originalEntity.getTitle(), eventDate));
        emailClientData.append("</body></html>");
        SendMail.send(creator.getFullName(), creator.getEmailAddress(), creator.getEmail(), prop.getText("calendar.na_schvalenie"), emailClientData.toString(), request);
    }
}
