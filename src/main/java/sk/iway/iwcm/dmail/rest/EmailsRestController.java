package sk.iway.iwcm.dmail.rest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.users.userdetail.UserDetailsRepository;
import sk.iway.iwcm.components.users.userdetail.UserDetailsService;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.dmail.DmailUtil;
import sk.iway.iwcm.dmail.Sender;
import sk.iway.iwcm.dmail.jpa.CampaingsEntity;
import sk.iway.iwcm.dmail.jpa.CampaingsRepository;
import sk.iway.iwcm.dmail.jpa.EmailsEntity;
import sk.iway.iwcm.dmail.jpa.EmailsRepository;
import sk.iway.iwcm.dmail.jpa.StatClicksRepository;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.datatable.NotifyBean.NotifyType;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.users.UserGroupsDB;

@RestController
@RequestMapping("/admin/rest/dmail/emails")
@PreAuthorize("@WebjetSecurityService.hasPermission('menuEmail')")
@Datatable
public class EmailsRestController extends DatatableRestControllerV2<EmailsEntity, Long> {

    private final EmailsRepository emailsRepository;
    private final CampaingsRepository campaingsRepository;
    private final StatClicksRepository statClicksRepository;
    private final UserDetailsRepository userDetailsRepository;

    private static final String NEW_LINE = "<br><br>";

    @Autowired
    public EmailsRestController(EmailsRepository emailsRepository, CampaingsRepository campaingsRepository, StatClicksRepository statClicksRepository, UserDetailsRepository userDetailsRepository) {
        super(emailsRepository);
        this.emailsRepository = emailsRepository;
        this.campaingsRepository = campaingsRepository;
        this.statClicksRepository = statClicksRepository;
        this.userDetailsRepository = userDetailsRepository;
    }

    @Override
    public Page<EmailsEntity> getAllItems(Pageable pageable) {

        DatatablePageImpl<EmailsEntity> page = null;

        String selectType = getRequest().getParameter("selectType");
        Long campainId = Long.valueOf(Tools.getIntValue(getRequest().getParameter("campainId"), -1));
        if (campainId<1L) campainId = (long)-getUser().getUserId();

        if(selectType != null && !selectType.isEmpty()) {
            if(selectType.equals("recipients")) {
                page = new DatatablePageImpl<>(emailsRepository.findAllByCampainIdAndDomainId(campainId, CloudToolsForCore.getDomainId() , pageable));
            } else if(selectType.equals("opens")) {
                page = new DatatablePageImpl<>(emailsRepository.findAllByCampainIdAndDomainIdAndSeenDateIsNotNull(campainId, CloudToolsForCore.getDomainId(), pageable));
            } else {
                //Empty page
                page = new DatatablePageImpl<>(new ArrayList<>());
            }
        } else {
            page = new DatatablePageImpl<>(emailsRepository.findAll(pageable));
        }

        processFromEntity(page, ProcessItemAction.GETALL);

        page.addOptions("groupIds", UserGroupsDB.getInstance().getUserGroups(), "userGroupName", "userGroupId", false);

        //reset last fetched ID to check all emails to send again
        Sender.resetSenderWait();

        return page;
    }

    @Override
    public EmailsEntity editItem(EmailsEntity entity, long id) {
        /*Extract email - ONLY IF importing
         * Example: from "Janko Tester <tester@test.sk>" do JUST "tester@test.sk"
        */
        if(isImporting() == true) {
            entity.setRecipientEmail( DmailUtil.parseEmailFromNameEmailFormat(entity.getRecipientEmail()) );
        }

        //Now remove unwanted characters
        entity.setRecipientEmail( entity.getRecipientEmail().replaceAll("[<>]", "") );

        //In editor can be put more than one email adress BUT if it's IMPORT action it's allways 1 email
        String[] emailsToInsert;
        if(isImporting() == true) {
            emailsToInsert = new String[]{entity.getRecipientEmail()};
        } else {
            emailsToInsert = Tools.getTokens(entity.getRecipientEmail(), ",;\n\s", true);
        }

        //This is replacement for raw/hand insert of emails (not thru CSV/Excel)
        Set<String> emailsTable = new HashSet<>();
        Set<String> unsubscribedEmails = DmailUtil.getUnsubscribedEmails();

        Long campainId = Long.valueOf(Tools.getIntValue(getRequest().getParameter("campainId"), -1));
        CampaingsEntity campaign = (campainId > 0L) ? campaingsRepository.getById(campainId) : null;

        //Load allready pushed emails in DB
        for (String email : emailsRepository.getAllCampainEmails( DmailService.getCampaignId(campaign, getUser()), CloudToolsForCore.getDomainId()) ) {
            emailsTable.add(email.toLowerCase());
        }

        //Skip wrong one so import can continue
        //Prepare message for user
        boolean skipWrongEmails = isImporting() == true ? isSkipWrongData() : entity.isSkipWrongEmails();
        StringBuilder warningMsg = new StringBuilder("");

        //Check if emailsToInsert are valid
        EmailsEntity saved = null;
        for(String recipientEmail : emailsToInsert) {

            //Get rid of white-spaces (for safety reason)
            recipientEmail = recipientEmail.replaceAll("\\s+","");

            //Validate email
            if(Tools.isEmail(recipientEmail) == false) {
                if(skipWrongEmails == false) {
                    throwError( getProp().getText("email_validation.error.invalid", recipientEmail) );
                } else {
                    //Skip
                    warningMsg.append( getProp().getText("email_validation.error.invalid", recipientEmail) ).append(NEW_LINE);
                    continue;
                }
            }

            //Protection against unsubscribed email addresses
            if(unsubscribedEmails.contains(recipientEmail.toLowerCase())) {
                warningMsg.append( getProp().getText("emails.error.unsubscribed", recipientEmail) ).append(NEW_LINE);
                continue;
            }

            //Email duplicity protection
            if(emailsTable.contains(recipientEmail.toLowerCase())) {
                if (emailsToInsert.length==1 && (entity.getId()!=null && entity.getId()>0)) {
                    //ak je v zozname len jeden email a uz ma v DB idecko, tak pokracuj, ulozime jeho zmeny
                } else {
                    warningMsg.append( getProp().getText("emails.error.duplicit_or_exist", recipientEmail) ).append(NEW_LINE);
                    continue;
                }
            }

            EmailsEntity emailToInsert;
            if (emailsToInsert.length==1) emailToInsert = entity;
            else emailToInsert = new EmailsEntity(recipientEmail, entity.getRecipientName());

            boolean prepareSuccess = DmailService.prepareEmailForInsert(campaign, getUser().getUserId(), emailToInsert);
            if(prepareSuccess == false) continue;

            //Save entity into DB
            if (saved == null) saved = emailsRepository.save(emailToInsert);
            else emailsRepository.save(emailToInsert);

            //Update emails table (because there can be duplicity in excel)
            emailsTable.add(recipientEmail.toLowerCase());
        }

        setForceReload(true);

        //Show warning message
        if(warningMsg.length() > 0) {
            addNotify(new NotifyBean(getProp().getText("emails.error.title"), warningMsg.toString(), NotifyType.WARNING));
        }

        return saved;
    }

    @Override
    public EmailsEntity insertItem(EmailsEntity entity) {
        //Insert item fn is override and dont save any entity
        //Save step is allready made in beforeSave fn
        return editItem(entity, -1);
    }

    @Override
    public boolean beforeDelete(EmailsEntity entity) {
        statClicksRepository.deleteByEmailId( entity.getId() );
        return true;
    }

    @Override
    public EmailsEntity processFromEntity(EmailsEntity entity, ProcessItemAction action) {

        if (entity == null) return null;

        Prop prop = getProp();
        String keyPrefix = "components.dmail.campaings.email.status.";

        if (entity.getRetry()!=null && entity.getSentDate()!=null && entity.getRetry().intValue()<0) entity.setStatus(prop.getText(keyPrefix+"error"));
        else if(entity.getCreatedByUserId()!=null && entity.getCreatedByUserId() < 0) entity.setStatus(prop.getText(keyPrefix+"temporal"));
        else if (Boolean.TRUE.equals(entity.getDisabled())) entity.setStatus(prop.getText(keyPrefix+"disabled"));
        else if (entity.getSentDate()!=null) entity.setStatus(prop.getText(keyPrefix+"sent"));
        else entity.setStatus(prop.getText(keyPrefix+"saved"));

        return entity;
    }

    @Override
    public boolean processAction(EmailsEntity entity, String action) {
        if ("resend".equals(action)) {
            //Resend emails
            try {
                new SimpleQuery().execute("UPDATE emails SET sent_date = ?, retry = ?, disabled = ? WHERE email_id = ?",null, 0, false, entity.getId());
            } catch (Exception e) {
                Logger.error(EmailsRestController.class, e);
                return false;
            }

            Sender.resetSenderWait();
        }
        else if("addRecipients".equals(action)) {
            String customData = getRequest().getParameter("customData");

            try {
                JSONObject jsonObject = new JSONObject(customData);
                Long campaingId = Tools.getLongValue( jsonObject.getString("campaingId"), -1L);
                String emailsString = Tools.getStringValue( jsonObject.getString("emails"), "");
                String permissionString = Tools.getStringValue( jsonObject.getString("permissions"), "");

                CampaingsEntity campain;
                if(campaingId > 0) {
                    campain = campaingsRepository.getById(campaingId);
                } else {
                    //Problem we dont have campain yet (create fake)
                    campain = new CampaingsEntity();
                    campain.setId( Long.valueOf(-getUser().getUserId()) );

                    // We CANT tell which groups were selected before, because campain is not saved yet
                    // For savety reason, we must remove ALL emails that have recipient_user_id
                    emailsRepository.deleteByCampainIdAndDomainIdWhereRecipientUserIsSet(-getUser().getUserId() , CloudToolsForCore.getDomainId());
                }

                //This prefixes are used in FE to distinguish between emails and permissions (FE cut them out but just to be sure)
                if(Tools.isNotEmpty(emailsString)) emailsString = emailsString.replace("email_", "");
                if(Tools.isNotEmpty(permissionString)) permissionString = permissionString.replace("perm_", "");

                Integer[] emailIds = Tools.getTokensInteger(emailsString, ",");
                Integer[] permissionIds = Tools.getTokensInteger(permissionString, ",");

                String selectedGroupsString = UserDetailsService.getUserGroupIds(emailIds, permissionIds);
                int[] selectedGroups = Tools.getTokensInt(selectedGroupsString, ",");
                int[] originalGroups = Tools.getTokensInt(campain.getUserGroupsIds(), ",");

				Adminlog.add(Adminlog.TYPE_DMAIL, String.format("Recipients groups for Campaign: %d changed \n from %s \n to %s", campaingId, campain.getUserGroupsIds(), selectedGroupsString), getUser().getUserId(), -1);

                DmailService.handleEmails(selectedGroups, originalGroups, campain, emailsRepository, userDetailsRepository, getRequest());

                if(campaingId > 0) {
                    //Update campain user groups (if campain is allready created)
                    campaingsRepository.updateUserGroups(selectedGroupsString, campaingId, CloudToolsForCore.getDomainId());
                }

            } catch (Exception ex) {
                sk.iway.iwcm.Logger.error(ex);
            }
        }

        return true;
    }

}
