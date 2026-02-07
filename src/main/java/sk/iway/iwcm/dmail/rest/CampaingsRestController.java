package sk.iway.iwcm.dmail.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.users.userdetail.UserDetailsController;
import sk.iway.iwcm.components.users.userdetail.UserDetailsRepository;
import sk.iway.iwcm.components.users.userdetail.UserDetailsService;
import sk.iway.iwcm.dmail.DmailUtil;
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
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.datatable.NotifyBean.NotifyType;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.users.UserDetails;
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

        page = new DatatablePageImpl<>( campaingsRepository.findAllByDomainId(CloudToolsForCore.getDomainId(), pageable) );

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
            //we need to set -1 as ID because of nested tables handling like recipients - they are saved using -1 as campain ID and after save we update it
            entity.setId(id);
            entity.setCountOfSentMails(0);

            //Delete previous temporaly saved emails
            emailsRepository.deleteByCampainIdAndDomainId((long)-user.getUserId(), CloudToolsForCore.getDomainId());
        } else {
            entity = campaingsRepository.getById(id);
        }

        processFromEntity(entity, ProcessItemAction.GETALL);

        return entity;
    }

    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, CampaingsEntity> target, Identity currentUser, Errors errors, Long id, CampaingsEntity campaingsEntity) {

        if(target.isDelete() || Tools.isNotEmpty(target.getImportMode())) return;

        if(campaingsEntity.getEditorFields().getPageToSend() == null || campaingsEntity.getEditorFields().getPageToSend().getDocId() == -1) {
            errors.rejectValue("errorField.editorFields.pageToSend", "403", getProp().getText("components.dmail.campaigns.doc.error.not_null"));
        }
    }

    @Override
	public void beforeSave(CampaingsEntity entity) {
        Identity user = UsersDB.getCurrentUser(getRequest());

        if(entity.getId() != null && entity.getId().longValue() > 0) {
            //Safety action - remove all unsubscribed emails from campaign (can happen)
            EmailDB.deleteUnsubscribedEmailsFromCampaign(entity.getId().intValue());
        }

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
	}

    @Override
    public void afterSave(CampaingsEntity entity, CampaingsEntity saved) {
        Long oldCampaignId = entity.getId();
        int userId = getUser().getUserId();
        if (oldCampaignId == null || oldCampaignId.intValue() == -1) oldCampaignId = Long.valueOf(-userId);

        //update vykoname vzdy, co ked sa zmenil predmet, alebo odosielatel
        emailsRepository.updateCampaingEmails(Integer.valueOf(userId), saved.getUrl(), saved.getSubject(), saved.getSenderName(), saved.getSenderEmail(), saved.getReplyTo(), saved.getCcEmail(), saved.getBccEmail(), saved.getSendAt(), saved.getAttachments(), saved.getId(), oldCampaignId, CloudToolsForCore.getDomainId());

        //Set count of recipients
        saved.setCountOfRecipients(emailsRepository.getNumberOfCampaingEmails(saved.getId(), CloudToolsForCore.getDomainId()));
        saved.setCountOfSentMails(emailsRepository.getNumberOfSentEmails(saved.getId(), CloudToolsForCore.getDomainId()));
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
        emailsRepository.deleteCampainEmail(campainId, filteredUserIds, CloudToolsForCore.getDomainId());
    }

    //Add emails which belongs to certain user group and campain
    private void addEmails(List<Integer> groupsAdded, CampaingsEntity entity) {
        Identity user = UsersDB.getCurrentUser(getRequest());

        //Now get all emails under campain actualy in DB - we need it to prevent duplicity
        Set<String> usedEmails = new HashSet<>();
        if (entity.getId() != null && entity.getId().longValue()>0) {
            for(String email : emailsRepository.getAllCampainEmails( getCampaignId(entity, getUser()), CloudToolsForCore.getDomainId()) ) {
                usedEmails.add(email.toLowerCase());
            }
        }
        //add all unsubscribed emails
        usedEmails.addAll(DmailUtil.getUnsubscribedEmails());

        //Get all emails under selected user groups
        List<Integer> recpientIds = UserDetailsController.getUserIdsByUserGroupsIds(userDetailsRepository, groupsAdded);
        for(Integer recipientId : recpientIds) {

            UserDetails recipient = UsersDB.getUser(recipientId);
            if(recipient == null) continue;

            // Duplicity check
            if(usedEmails.contains( recipient.getEmail().toLowerCase() )) continue;
            usedEmails.add(recipient.getEmail().toLowerCase());

            //Check validity then continue
            if (Tools.isEmail(recipient.getEmail()) == true) {
                //Prepare and save email
                EmailsEntity emailToAdd = new EmailsEntity( recipient.getEmail() );
                boolean prepareSuccess = EmailsRestController.prepareEmailForInsert(entity, user.getUserId(), emailToAdd, recipient);
                if(prepareSuccess == false) continue; //Email is not valid

                //Save record in DB
                emailsRepository.save(emailToAdd);
            } else {
                if (getLastImportedRow()!=null) addNotify(new NotifyBean(getProp().getText("datatables.error.title.js"), getProp().getText("datatable.error.importRow", String.valueOf(getLastImportedRow().intValue()+1), "`" + recipient.getEmail()) + "` "+getProp().getText("components.dmail.emailIsNotValid"), NotifyType.ERROR));
                else addNotify(new NotifyBean(getProp().getText("datatables.error.title.js"), getProp().getText("components.dmail.unsubscribe.email.error", "`" + recipient.getEmail() + "`"), NotifyType.ERROR));
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

        //Safety action - remove all unsubscribed emails from campaign (can happen)
        EmailDB.deleteUnsubscribedEmailsFromCampaign(entity.getId().intValue());

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

        Integer totalEmails = emailsRepository.getNumberOfCampaingEmails(entity.getId(), CloudToolsForCore.getDomainId());
        if (totalEmails != null && totalEmails.intValue() > 0) {
            //delete all campiang dependencies
            emailsRepository.deleteByCampainIdAndDomainId(entity.getId(), CloudToolsForCore.getDomainId());
            statClicksRepository.deleteByCampainId(entity.getId());
        }

        return true;
    }

    /**
     * Returns ID of campaign. If campaign is not saved yet, returns -ID of current user used as temporary ID for nested tables
     * @param entity
     * @param user
     * @return
     */
    public static Long getCampaignId(CampaingsEntity entity, UserDetails user) {
        if (entity != null && entity.getId() != null && entity.getId().longValue()>0) return entity.getId();
        if (user == null) return 0L;
        return Long.valueOf(-user.getUserId());
    }

}
