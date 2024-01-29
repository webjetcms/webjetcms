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
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.system.datatable.NotifyBean.NotifyType;
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
                page = new DatatablePageImpl<>(emailsRepository.findAllByCampainId(campainId, pageable));
            } else if(selectType.equals("opens")) {
                page = new DatatablePageImpl<>(emailsRepository.findAllByCampainIdAndSeenDateIsNotNull(campainId, pageable));
            }
        } else {
            page = new DatatablePageImpl<>(emailsRepository.findAll(pageable));
        }

        processFromEntity(page, ProcessItemAction.GETALL);

        return page;
    }

    @Override
    public EmailsEntity editItem(EmailsEntity entity, long id) {
        //In editor can be put more than one email adress
        String[] emailsToInsert = Tools.getTokens(entity.getRecipientEmail(), ", ;\n", true);

        //This is replacement for raw/hand insert of emails (not thru CSV/Excel)
        Set<String> emailsTable = new HashSet<>();
        Set<String> unsubscribedEmails = DmailUtil.getUnsubscribedEmails();

        Long campainId = Long.valueOf(Tools.getIntValue(getRequest().getParameter("campainId"), -1));
        CampaingsEntity campaign = null;
        if (campainId>0L) campaign = campaingsRepository.getById(campainId);

        //Load allready pushed emails in DB
        for (String email : emailsRepository.getAllCampainEmails(CampaingsRestController.getCampaignId(campaign, getUser()))) {
            emailsTable.add(email.toLowerCase());
        }

        EmailsEntity saved = null;

        //Check if emailsToInsert are valid
        for(String recipientEmail : emailsToInsert) {

            //Get rid of white-spaces (for safety reason)
            recipientEmail = recipientEmail.replaceAll("\\s+","");

            //Protection against unsubscribed email addresses
            if(unsubscribedEmails.contains(recipientEmail.toLowerCase())) continue;

            //Email duplicity protection
            if(emailsTable.contains(recipientEmail.toLowerCase())) {
                if (emailsToInsert.length==1 && (entity.getId()!=null && entity.getId()>0)) {
                    //ak je v zozname len jeden email a uz ma v DB idecko, tak pokracuj, ulozime jeho zmeny
                } else {
                    continue;
                }
            }

            EmailsEntity emailToInsert;
            if (emailsToInsert.length==1) emailToInsert = entity;
            else emailToInsert = new EmailsEntity(recipientEmail, entity.getRecipientName());

            boolean isValid = prepareEmailForInsert(campaign, getUser().getUserId(), emailToInsert);
            if (isValid) {

                //Save entity into DB
                if (saved == null) saved = emailsRepository.save(emailToInsert);
                else emailsRepository.save(emailToInsert);

                //Update emails table (because there can be duplicity in excel)
                emailsTable.add(recipientEmail);
            } else {
                addNotify(new NotifyBean(getProp().getText("datatables.error.title.js"), getProp().getText("datatable.error.importRow", String.valueOf(getLastImportedRow().intValue()+1), "`"+recipientEmail)+"` "+getProp().getText("components.dmail.emailIsNotValid"), NotifyType.ERROR));
            }
        }

        setForceReload(true);

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

        //trimni email adresu
        if (entity.getRecipientEmail()!=null) entity.setRecipientEmail(entity.getRecipientEmail().trim());

        //sprav lower case
        if (entity.getRecipientEmail()!=null) entity.setRecipientEmail(entity.getRecipientEmail().toLowerCase());

        if(Tools.isEmail(entity.getRecipientEmail())==false) {
            return false;
        }

        //Get email recipient(user) using email
        UserDetails recipient = UsersDB.getUserByEmail(entity.getRecipientEmail(), 600);
        if(recipient == null) {
            entity.setRecipientUserId(-1);
            if(Tools.isEmpty(entity.getRecipientName())) entity.setRecipientName("- -");
        }
        else {
            entity.setRecipientUserId(recipient.getUserId());
            if(Tools.isEmpty(entity.getRecipientName())) entity.setRecipientName(recipient.getFullName());
        }

        //Set create values
        if (campaign == null) {
            entity.setCampainId((long)-loggedUserId);
            entity.setUrl("");
            entity.setSubject("");
            entity.setSenderName("");
            entity.setSenderEmail("");
            entity.setCreatedByUserId(loggedUserId);
        } else {
            entity.setCampainId(campaign.getId());
            entity.setUrl(campaign.getUrl());
            entity.setSubject(campaign.getSubject());
            entity.setSenderName(campaign.getSenderName());
            entity.setSenderEmail(campaign.getSenderEmail());
            entity.setCreatedByUserId(loggedUserId);

            if (campaign.getId()==null || campaign.getId().longValue()<1) {
                entity.setCreatedByUserId(-loggedUserId);
            }
        }

        entity.setCreateDate(new Date());
        entity.setRetry(0);

        if (entity.getId()==null || entity.getId().longValue()<1) {
            entity.setDisabled(true);
        }

        return true;
    }

    @Override
    public boolean beforeDelete(EmailsEntity entity) {
        statClicksRepository.deleteByEmailId(entity.getId());
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
