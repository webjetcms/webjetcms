package sk.iway.iwcm.dmail.rest;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
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
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

@RestController
@RequestMapping("/admin/rest/dmail/emails")
@PreAuthorize("@WebjetSecurityService.hasPermission('menuEmail')")
@Datatable
public class EmailsRestController extends DatatableRestControllerV2<EmailsEntity, Long> {

    private final EmailsRepository emailsRepository;
    private final CampaingsRepository campaingsRepository;
    private final StatClicksRepository statClicksRepository;

    private static final String NEW_LINE = "<br><br>";

    @Autowired
    public EmailsRestController(EmailsRepository emailsRepository, CampaingsRepository campaingsRepository, StatClicksRepository statClicksRepository) {
        super(emailsRepository);
        this.emailsRepository = emailsRepository;
        this.campaingsRepository = campaingsRepository;
        this.statClicksRepository = statClicksRepository;
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
            }
        } else {
            page = new DatatablePageImpl<>(emailsRepository.findAll(pageable));
        }

        processFromEntity(page, ProcessItemAction.GETALL);

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
        for (String email : emailsRepository.getAllCampainEmails( CampaingsRestController.getCampaignId(campaign, getUser()), CloudToolsForCore.getDomainId()) ) {
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

            boolean prepareSuccess = prepareEmailForInsert(campaign, getUser().getUserId(), emailToInsert);
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

    /**
     * Pripravi entity na vlozenie do DB, nastavi udaje podla campaign a podla emailu dohlada userId v databaze pouzivatelov
     * @param campaign
     * @param loggedUserId
     * @param entity
     */
    public static boolean prepareEmailForInsert(CampaingsEntity campaign, int loggedUserId, EmailsEntity entity) {
        //Get email recipient(user) using email
        UserDetails recipient = UsersDB.getUserByEmail(entity.getRecipientEmail(), 600);
        return prepareEmailForInsert(campaign, loggedUserId, entity, recipient);
    }

    public static boolean prepareEmailForInsert(CampaingsEntity campaign, int loggedUserId, EmailsEntity entity, UserDetails recipient) {

        //trimni email adresu
        if (entity.getRecipientEmail()!=null) entity.setRecipientEmail(entity.getRecipientEmail().trim());

        //sprav lower case
        if (entity.getRecipientEmail()!=null) entity.setRecipientEmail(entity.getRecipientEmail().toLowerCase());

        if(recipient == null) {
            entity.setRecipientUserId(-1);
            if(Tools.isEmpty(entity.getRecipientName())) entity.setRecipientName("- -");
        }
        else {
            //Check if user is valid
            if(UserDetailsService.isUserDisabled(recipient) == true) return false;

            entity.setRecipientUserId(recipient.getUserId());
            if(Tools.isEmpty(entity.getRecipientName())) entity.setRecipientName(recipient.getFullName());
        }

        //Set create values
        if (campaign == null || campaign.getId()==null || campaign.getId().longValue()<1) {
            entity.setCampainId((long)-loggedUserId);
            //oracle cant store null in string fields, so we set "-"
            entity.setUrl("-");
            entity.setSubject("-");
            entity.setSenderName("-");
            entity.setSenderEmail("-");
            entity.setCreatedByUserId(loggedUserId);
        } else {
            entity.setCampainId(campaign.getId());
            entity.setUrl(campaign.getUrl());
            entity.setSubject(campaign.getSubject());
            entity.setSenderName(campaign.getSenderName());
            entity.setSenderEmail(campaign.getSenderEmail());
            entity.setCreatedByUserId(loggedUserId);
        }

        entity.setCreateDate(new Date());
        entity.setRetry(0);

        //Set domainId
        entity.setDomainId( CloudToolsForCore.getDomainId() );

        if (entity.getId()==null || entity.getId().longValue()<1) {
            entity.setDisabled(true);
        }

        return true;
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
        }

        Sender.resetSenderWait();

        return true;
    }

}
