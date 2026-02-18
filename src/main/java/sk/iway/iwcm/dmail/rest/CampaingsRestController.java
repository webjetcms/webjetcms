package sk.iway.iwcm.dmail.rest;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
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
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
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

            String senderName = SendMail.getDefaultSenderName("dmail", user.getFullName());
            entity.setSenderName(senderName);

            String senderEmail = SendMail.getDefaultSenderEmail("dmail", user.getEmail());
            entity.setSenderEmail(senderEmail);

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
        if(entity.getId() != null && entity.getId().longValue() > 0) {
            //Safety action - remove all unsubscribed emails from campaign (can happen)
            EmailDB.deleteUnsubscribedEmailsFromCampaign(entity.getId().intValue());
        }

        int[] selectedGroups = Tools.getTokensInt(UserDetailsService.getUserGroupIds(entity.getEditorFields().getPermisions(), entity.getEditorFields().getEmails()), ",");
        int[] originalGroups = Tools.getTokensInt(entity.getUserGroupsIds(), ",");

        entity = processToEntity(entity, ProcessItemAction.CREATE);

        if(entity.getId() != null && entity.getId().longValue() > 0 && Arrays.equals(selectedGroups, originalGroups) == false) {
            DmailService.handleEmails(selectedGroups, originalGroups, entity, emailsRepository, userDetailsRepository, getRequest());
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

    @GetMapping(path = "/user-perms", produces = "text/plain;charset=UTF-8")
    public String getUserPerms() {
        JSONObject json = new JSONObject();

        Map<String, String> emailPerms = new HashMap<>();
        for(UserGroupDetails ugd : UserGroupsDB.getInstance().getUserGroupsByTypeId(UserGroupDetails.TYPE_EMAIL)) {
            emailPerms.put(ugd.getUserGroupId() + "", ugd.getUserGroupName());
        }
        json.put("emails", emailPerms);

        Map<String, String> permPerms = new HashMap<>();
        for(UserGroupDetails ugd  : UserGroupsDB.getInstance().getUserGroupsByTypeId(UserGroupDetails.TYPE_PERMS)) {
            permPerms.put(ugd.getUserGroupId() + "", ugd.getUserGroupName());
        }
        json.put("perms", permPerms);

        return json.toString();
    }

    @Override
    public void afterDuplicate(CampaingsEntity entity, Long originalId) {
        //Create recipients for new campaign
        List<EmailsEntity> emails = emailsRepository.findAllByCampainIdAndDomainIdOrderByIdDesc(originalId, CloudToolsForCore.getDomainId());
        for (EmailsEntity email : emails) {
            email.setId(null);
            email.setCampainId(entity.getId());
            email.setCreateDate(new Date());
            email.setCreatedByUserId(getUser().getUserId());

            emailsRepository.save(email);
        }
    }
}
