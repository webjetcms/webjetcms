package sk.iway.iwcm.dmail.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.users.userdetail.UserDetailsController;
import sk.iway.iwcm.components.users.userdetail.UserDetailsRepository;
import sk.iway.iwcm.components.users.userdetail.UserDetailsService;
import sk.iway.iwcm.dmail.DmailUtil;
import sk.iway.iwcm.dmail.jpa.CampaingsEntity;
import sk.iway.iwcm.dmail.jpa.EmailsEntity;
import sk.iway.iwcm.dmail.jpa.EmailsRepository;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

public class DmailService {

    // Private constructor to hide the implicit public one
    private DmailService() {}

    /**
     * Returns ID of campaign. If campaign is not saved yet, returns -ID of current user used as temporary ID for nested tables
     * @param campaing
     * @param user
     * @return
     */
    public static final Long getCampaignId(CampaingsEntity campaing, UserDetails user) {
        if (campaing != null && campaing.getId() != null && campaing.getId().longValue()>0) return campaing.getId();
        if (user == null) return 0L;
        return Long.valueOf(-user.getUserId());
    }

    //Add emails which belongs to certain user group and campain
    public static void addEmails(List<Integer> groupsAdded, CampaingsEntity campaing, EmailsRepository emailsRepository, UserDetailsRepository userDetailsRepository, HttpServletRequest request) {
        Identity user = UsersDB.getCurrentUser(request);

        //Now get all emails under campain actualy in DB - we need it to prevent duplicity
        Set<String> usedEmails = new HashSet<>();
        for (String email : emailsRepository.getAllCampainEmails( DmailService.getCampaignId(campaing, user), CloudToolsForCore.getDomainId()) ) {
            usedEmails.add(email.toLowerCase());
        }
        //add all unsubscribed emails
        usedEmails.addAll(DmailUtil.getUnsubscribedEmails());

        //Get all emails under selected user groups
        List<Integer> recpientIds = UserDetailsController.getUserIdsByUserGroupsIds(userDetailsRepository, groupsAdded);

        int count = 0;

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
                boolean prepareSuccess = prepareEmailForInsert(campaing, user == null ? 0 : user.getUserId(), emailToAdd, recipient);
                if(prepareSuccess == false) continue; //Email is not valid

                //Save record in DB
                emailsRepository.save(emailToAdd);
                count++;
            } else {
                return;
            }
        }

        RequestBean.addAuditValue("addedEmailCount", ""+count);
    }

    //Add and/or remove emails which belongs to campain by ceratin user group
    public static void handleEmails(int[] selectedGroups, int[] originalGroups, CampaingsEntity campaing, EmailsRepository emailsRepository, UserDetailsRepository userDetailsRepository, HttpServletRequest request) {
        /* We need compare saved groups ids and now selected group ids
            so we can add/delete emails mapped on current domain, where reciver belongs to one of that groups*/
        List<Integer> groupsRemoved = new ArrayList<>();
        List<Integer> groupsAdded = new ArrayList<>();
        List<Integer> selectedGroupsList = Arrays.stream(selectedGroups).boxed().toList();

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

        if(groupsRemoved.isEmpty()==false) removeEmails(groupsRemoved, mustContainUserId, campaing, emailsRepository, userDetailsRepository);
        if(groupsAdded.isEmpty()==false) addEmails(groupsAdded, campaing, emailsRepository, userDetailsRepository, request);
    }

    /**
     * Odstrani pouzivatelov v skupine groupsRemoved, zachova ale pouzivatelov v mustContainUserId
     * (user moze byt vo viacerych skupinach, takze moze byt v tej co musi zostat zachovane)
     * @param groupsRemoved
     * @param mustContainUserId
     * @param campain
     */
    private static void removeEmails(List<Integer> groupsRemoved, Set<Integer> mustContainUserId, CampaingsEntity campain, EmailsRepository emailsRepository, UserDetailsRepository userDetailsRepository) {

        List<Integer> userIds = UserDetailsController.getUserIdsByUserGroupsIds(userDetailsRepository, groupsRemoved);
        List<Integer> filteredUserIds = new ArrayList<>();

        for (Integer uid : userIds) {
            if (mustContainUserId.contains(uid)) continue;
            filteredUserIds.add(uid);
        }

        //Delete all emails under removed user group
        int deleted = emailsRepository.deleteCampainEmail(campain.getId(), filteredUserIds, CloudToolsForCore.getDomainId());
        RequestBean.addAuditValue("removedEmailCount", ""+deleted);
    }

    /**
     * Pripravi entity na vlozenie do DB, nastavi udaje podla campaign a podla emailu dohlada userId v databaze pouzivatelov
     * @param campaign
     * @param loggedUserId
     * @param email
     */
    public static boolean prepareEmailForInsert(CampaingsEntity campaign, int loggedUserId, EmailsEntity email) {
        //Get email recipient(user) using email
        UserDetails recipient = UsersDB.getUserByEmail(email.getRecipientEmail(), 600);
        return prepareEmailForInsert(campaign, loggedUserId, email, recipient);
    }

    /**
     * Pripravi entity na vlozenie do DB, nastavi udaje podla campaign a podla emailu dohlada userId v databaze pouzivatelov
     * @param campaign
     * @param loggedUserId
     * @param email
     * @param recipient
     * @return
     */
    public static boolean prepareEmailForInsert(CampaingsEntity campaign, int loggedUserId, EmailsEntity email, UserDetails recipient) {

        //trimni email adresu
        if (email.getRecipientEmail()!=null) email.setRecipientEmail(email.getRecipientEmail().trim());

        //sprav lower case
        if (email.getRecipientEmail()!=null) email.setRecipientEmail(email.getRecipientEmail().toLowerCase());

        if(recipient == null) {
            email.setRecipientUserId(-1);
            if(Tools.isEmpty(email.getRecipientName())) email.setRecipientName("- -");
        }
        else {
            //Check if user is valid
            if(UserDetailsService.isUserDisabled(recipient) == true) return false;

            email.setRecipientUserId(recipient.getUserId());
            if(Tools.isEmpty(email.getRecipientName())) email.setRecipientName(recipient.getFullName());
        }

        //Set create values
        if (campaign == null || campaign.getId()==null || campaign.getId().longValue()<1) {
            email.setCampainId((long)-loggedUserId);
            //oracle does not allow null values in varchar fields - set default values
            email.setUrl("-");
            email.setSubject("-");
            email.setSenderName("-");
            email.setSenderEmail("-");
        } else {
            email.setCampainId(campaign.getId());
            email.setUrl(campaign.getUrl());
            email.setSubject(campaign.getSubject());
            email.setSenderName(campaign.getSenderName());
            email.setSenderEmail(campaign.getSenderEmail());
        }

        email.setCreatedByUserId(loggedUserId);
        email.setCreateDate(new Date());
        email.setRetry(0);

        //Set domainId
        email.setDomainId( CloudToolsForCore.getDomainId() );

        if (email.getId()==null || email.getId().longValue()<1) {
            email.setDisabled(true);
        }

        return true;
    }
}
