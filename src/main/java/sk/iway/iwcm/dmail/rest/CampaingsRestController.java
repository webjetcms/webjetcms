package sk.iway.iwcm.dmail.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.users.userdetail.UserDetailsController;
import sk.iway.iwcm.components.users.userdetail.UserDetailsRepository;
import sk.iway.iwcm.components.users.userdetail.UserDetailsService;
import sk.iway.iwcm.dmail.EmailDB;
import sk.iway.iwcm.dmail.Sender;
import sk.iway.iwcm.dmail.jpa.CampaingsEditorFields;
import sk.iway.iwcm.dmail.jpa.CampaingsEntity;
import sk.iway.iwcm.dmail.jpa.CampaingsRepository;
import sk.iway.iwcm.dmail.jpa.EmailsEntity;
import sk.iway.iwcm.dmail.jpa.EmailsRepository;
import sk.iway.iwcm.dmail.jpa.StatClicksRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.system.datatable.NotifyBean.NotifyType;
import sk.iway.iwcm.users.UserGroupDetails;
import sk.iway.iwcm.users.UserGroupsDB;
import sk.iway.iwcm.users.UsersDB;

@RestController
@RequestMapping("/admin/rest/dmail/campaings")
@PreAuthorize("@WebjetSecurityService.hasPermission('menuEmail')")
@Datatable
public class CampaingsRestController extends DatatableRestControllerV2<CampaingsEntity, Long> {

    private final CampaingsRepository campaingsRepository;
    private final EmailsRepository emailsRepository;
    private final UserDetailsRepository userDetailsRepository;
    private final StatClicksRepository statClicksRepository;

    @Autowired
    public CampaingsRestController(
        CampaingsRepository campaingsRepository,
        EmailsRepository emailsRepository,
        UserDetailsRepository userDetailsRepository,
        StatClicksRepository statClicksRepository) {

            super(campaingsRepository);
            this.campaingsRepository = campaingsRepository;
            this.emailsRepository = emailsRepository;
            this.userDetailsRepository = userDetailsRepository;
            this.statClicksRepository = statClicksRepository;
    }

    @Override
    public Page<CampaingsEntity> getAllItems(Pageable pageable) {

        DatatablePageImpl<CampaingsEntity> page;

        page = new DatatablePageImpl<>(campaingsRepository.findAll(pageable));

        processFromEntity(page, ProcessItemAction.GETALL);

        //Add both perms and emails (The receiver field represents a list of groups that will receive an email)
        page.addOptions("editorFields.emails", UserGroupsDB.getInstance().getUserGroupsByTypeId(UserGroupDetails.TYPE_EMAIL), "userGroupName", "userGroupId", false);
        page.addOptions("editorFields.permisions", UserGroupsDB.getInstance().getUserGroupsByTypeId(UserGroupDetails.TYPE_PERMS), "userGroupName", "userGroupId", false);

        return page;
    }

    @Override
    public CampaingsEntity getOneItem(long id) {

        CampaingsEntity entity;
        Identity user = UsersDB.getCurrentUser(getRequest());

        if(id == -1) {
            entity = new CampaingsEntity();
            entity.setSenderName(user.getFullName());
            entity.setSenderEmail(user.getEmail());
            entity.setId(id);
            entity.setCountOfSentMails(0);
        } else {
            entity = campaingsRepository.getById(id);
        }

        processFromEntity(entity, ProcessItemAction.GETALL);

        //Delete temporaly saved emails (temporaly saved have reverse user id)
        List<EmailsEntity> tempSavedEmails = emailsRepository.findAllByCreatedByUserId(-user.getUserId());

        for(EmailsEntity email : tempSavedEmails) {
            //If emails are temporaly but, under this one campain, dont delete them now
            if(email.getCampainId()!=null && email.getCampainId().intValue()>0 && email.getCampainId().intValue() != entity.getId().intValue()) emailsRepository.delete(email);
        }

        return entity;
    }

    @Override
	public void beforeSave(CampaingsEntity entity) {
        Identity user = UsersDB.getCurrentUser(getRequest());

        int[] selectedGroups = Tools.getTokensInt(UserDetailsService.getUserGroupIds(entity.getEditorFields().getPermisions(), entity.getEditorFields().getEmails()), ",");
        int[] originalGroups = Tools.getTokensInt(entity.getUserGroupsIds(), ",");

        entity = processToEntity(entity, ProcessItemAction.CREATE);

        if(entity.getId()==null || entity.getId().longValue() <1) {
            entity.setCreateDate(new Date());
            entity.setCreatedByUserId(user.getUserId());

            addEmails(Arrays.stream(selectedGroups).boxed().collect(Collectors.toList()), entity);
        } else {
            if(!Arrays.equals(selectedGroups, originalGroups)) {
                handleEmails(selectedGroups, originalGroups, entity);
            }
        }

        //Set count of recipients
        entity.setCountOfRecipients(emailsRepository.getNumberOfCampaingEmails(entity.getId()));
	}

    @Override
    public void afterSave(CampaingsEntity entity, CampaingsEntity saved) {
        Long oldCampaignId = entity.getId();
        if (oldCampaignId == null) oldCampaignId = Long.valueOf(-1);
        int userId = getUser().getUserId();
        int oldUserId = userId;
        if (entity.getId()==null || entity.getId()<1L) {
            //pri novej entite musime zmenit IDecka v databaze, ktore su ulozene ako -userId
            oldUserId = -userId;
        }
        //update vykoname vzdy, co ked sa zmenil predmet, alebo odosielatel
        emailsRepository.updateCampaingEmails(Integer.valueOf(userId), saved.getUrl(), saved.getSubject(), saved.getSenderName(), saved.getSenderEmail(), saved.getReplyTo(), saved.getCcEmail(), saved.getBccEmail(), saved.getSendAt(), saved.getAttachments(), saved.getId(), Integer.valueOf(oldUserId), oldCampaignId);

        //Set count of recipients
        saved.setCountOfRecipients(emailsRepository.getNumberOfCampaingEmails(saved.getId()));
        saved.setCountOfSentMails(emailsRepository.getNumberOfSentEmails(saved.getId()));
        campaingsRepository.save(saved);

        //resetni sender
        Sender.resetSenderWait();
    }

    //Add and/or remove emails which belongs to campain by ceratin user group
    private void handleEmails(int[] selectedGroups, int[] originalGroups, CampaingsEntity entity) {
        /* We need compare saved groups ids and now selected group ids
            so we can add/delete emails mapped on current domain, where reciver belongs to one of that groups*/
        List<Integer> groupsRemoved = new ArrayList<>();
        List<Integer> groupsAdded = new ArrayList<>();
        List<Integer> selectedGroupsList = Arrays.stream(selectedGroups).boxed().collect(Collectors.toList());

        //Fn handleEmails is called only if selectedGroups != originalGroups
        if(selectedGroups == null || selectedGroups.length == 0) {
            groupsRemoved = Arrays.stream(originalGroups).boxed().collect(Collectors.toList());

        } else if(originalGroups == null || originalGroups.length == 0) {
            groupsAdded = Arrays.stream(selectedGroups).boxed().collect(Collectors.toList());
        } else {
            //Find of groups witch ids were removed from selected
            for(int originalId : originalGroups) {
                boolean find = false;
                for(int selectedId : selectedGroups) {
                    if(originalId == selectedId) {
                        find = true;
                        break;
                    }
                }
                if(!find) groupsRemoved.add(originalId);
            }

            //Find of groups witch ids were added to selected
            for(int selectedId : selectedGroups) {
                boolean find = false;
                for(int originalId : originalGroups) {
                    if(selectedId == originalId) {
                        find = true;
                        break;
                    }
                }
                if(!find) groupsAdded.add(selectedId);
            }
        }

        //toto je zoznam userId ktore musia zostat zachovane
        //jeden user moze byt vo viacerych user skupinach, ked odoberieme, musi zostat ak je v inej
        Set<Integer> mustContainUserId = new HashSet<>();
        List<Integer> userIds = UserDetailsController.getUserIdsByUserGroupsIds(userDetailsRepository, selectedGroupsList);
        for (Integer uid : userIds) {
            mustContainUserId.add(uid);
        }

        if(groupsRemoved.isEmpty()==false) removeEmails(groupsRemoved, mustContainUserId, entity.getId());
        if(groupsAdded.isEmpty()==false) addEmails(groupsAdded, entity);
    }

    /**
     * Odstrani pouzivatelov v skupine groupsRemoved, zachova ale pouzivatelov v mustContainUserId
     * (user moze byt vo viacerych skupinach, takze moze byt v tej co musi zostat zachovane)
     * @param groupsRemoved
     * @param mustContainUserId
     * @param campainId
     */
    private void removeEmails(List<Integer> groupsRemoved, Set<Integer> mustContainUserId, Long campainId) {

        List<Integer> userIds = UserDetailsController.getUserIdsByUserGroupsIds(userDetailsRepository, groupsRemoved);
        List<Integer> filteredUserIds = new ArrayList<>();

        for (Integer uid : userIds) {
            if (mustContainUserId.contains(uid)) continue;
            filteredUserIds.add(uid);
        }

        //Delete all emails under removed user group
        emailsRepository.deleteCampainEmail(campainId, filteredUserIds);
    }

    //Add emails which belongs to certain user group and campain
    private void addEmails(List<Integer> groupsAdded, CampaingsEntity entity) {
        Identity user = UsersDB.getCurrentUser(getRequest());

        //Now get all emails under campain actualy in DB - we need it to prevent duplicity
        Map<String, Integer> emailsTable = new Hashtable<>();
        if (entity.getId() != null && entity.getId().longValue()>0) {
            for (String email : emailsRepository.getAllCampainEmails(entity.getId())) {
                emailsTable.put(email, emailsTable.size() + 1);
            }
        }

        List<String> recpientEmails = UserDetailsController.getUserEmailsByUserGroupsIds(userDetailsRepository, groupsAdded);

        for(String recipientEmail : recpientEmails) {
            //Check duplicity (if this emial alreadry belongs to campain)
            if(emailsTable.get(recipientEmail) != null) continue;
            else emailsTable.put(recipientEmail, emailsTable.size() + 1);

            EmailsEntity emailToAdd = new EmailsEntity(recipientEmail);
            boolean isValid = EmailsRestController.prepareEmailForInsert(entity, user.getUserId(), emailToAdd);
            if (isValid) {
                emailToAdd.setSubject(entity.getSubject());
                emailToAdd.setUrl(entity.getUrl());

                //Save record in DB
                emailsRepository.save(emailToAdd);
            } else {
                if (getLastImportedRow()!=null) addNotify(new NotifyBean(getProp().getText("datatables.error.title.js"), getProp().getText("datatable.error.importRow", String.valueOf(getLastImportedRow().intValue()+1), "`"+recipientEmail)+"` "+getProp().getText("components.dmail.emailIsNotValid"), NotifyType.ERROR));
                else addNotify(new NotifyBean(getProp().getText("datatables.error.title.js"), getProp().getText("components.dmail.unsubscribe.email.error", "`"+recipientEmail+"`"), NotifyType.ERROR));
            }
        }
    }

    @Override
    public CampaingsEntity processFromEntity(CampaingsEntity entity, ProcessItemAction action) {

        if (entity == null) entity = new CampaingsEntity();

        if(entity.getEditorFields() == null) {
            CampaingsEditorFields cef = new CampaingsEditorFields();
            cef.fromCampaingEntity(entity, getProp(), getRequest());
            entity.setEditorFields(cef);
        }

        return entity;
    }

    @Override
    public CampaingsEntity processToEntity(CampaingsEntity entity, ProcessItemAction action) {
        if(entity != null) {
            //
            CampaingsEditorFields cef = new CampaingsEditorFields();
            cef.toCampaingEntity(entity, getRequest());
        }
        return entity;
    }

    @Override
    public boolean processAction(CampaingsEntity entity, String action) {

        if ("start".equals(action)) { //Send emails
            EmailDB.activateDisableEmails(false, entity.getId().intValue());
        } else if ("stop".equals(action)) { //Stop emails
            EmailDB.activateDisableEmails(true, entity.getId().intValue());
        } else if ("resend".equals(action)) { //Resend emails
            EmailDB.resendEmail(entity.getId().intValue());
        }

        Sender.resetSenderWait();

        return true;
    }

    @Override
    public boolean beforeDelete(CampaingsEntity entity) {

        Integer totalEmails = emailsRepository.getNumberOfCampaingEmails(entity.getId());
        if (totalEmails != null && totalEmails.intValue()>0) {
            //delete all campiang dependencies
            emailsRepository.deleteByCampainId(entity.getId());
            statClicksRepository.deleteByCampainId(entity.getId());
        }

        return true;
    }

}
