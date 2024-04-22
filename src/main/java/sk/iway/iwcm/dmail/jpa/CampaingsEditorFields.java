package sk.iway.iwcm.dmail.jpa;

import java.io.Serializable;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;

import org.springframework.beans.BeanWrapperImpl;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.users.userdetail.UserDetailsService;
import sk.iway.iwcm.dmail.EmailDB;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.editor.service.WebpagesService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.users.UserGroupsDB;

@Getter
@Setter
public class CampaingsEditorFields implements Serializable {

    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, title = "user.permissions.label", tab = "groupsTab", hidden = true, editor = {
        @DataTableColumnEditor(attr = {
            @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "user.admin.editUserGroups"),
            @DataTableColumnEditorAttr(key = "unselectedValue", value = "") }) })
    private Integer[] permisions;

    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, title = "groupedit.type_email", tab = "groupsTab", hidden = true, editor = {
        @DataTableColumnEditor(attr = {
            @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "menu.email"),
            @DataTableColumnEditorAttr(key = "unselectedValue", value = "") }) })
    private Integer[] emails;

    //Replacement for CampaingsEntity.userGroupsIds, showen in table as text but hidden in editor
    @DataTableColumn(inputType = DataTableColumnType.TEXT,
        title = "components.email.regenerate.all.groups.title",
        hiddenEditor = true,
        filter = false,
        sortAfter = "senderEmail"
    )
    private String userGroupsNames = "";

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.dmail.camp.send_emails",
        hiddenEditor = true,
        filter = false,
        sortAfter = "lastSentDate")
    private String sentMails;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.dmail.campaings.status",
        hiddenEditor = true,
        filter = false,
        sortAfter = "sentMails")
    private String status;

    //Get DocDetails then convert to emailsEntity.url (String)
    @DataTableColumn(
        inputType = DataTableColumnType.JSON,
        title="components.dmail.admin_email.emailUrl",
        className = "dt-tree-page",
        tab = "main",
        sortAfter = "subject",
        filter = false
    )
    @NotEmpty
    private DocDetails pageToSend = null;

    @DataTableColumn(
        inputType = DataTableColumnType.ELFINDER,
        title="email.attachment",
        tab = "advanced",
        hidden = true,
        sortAfter = "sendAt",
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before"),
                }
            )
        }
    )
    private String file1;

    @DataTableColumn(
        inputType = DataTableColumnType.ELFINDER,
        title="&nbsp;",
        tab = "advanced",
        sortAfter = "editorFields.file1",
        hidden = true
    )
    private String file2;

    @DataTableColumn(
        inputType = DataTableColumnType.ELFINDER,
        title="&nbsp;",
        tab = "advanced",
        sortAfter = "editorFields.file2",
        hidden = true
    )
    private String file3;


    public void fromCampaingEntity(CampaingsEntity campOriginal, Prop prop, HttpServletRequest request) {

        //Set this Editor fields
        campOriginal.setEditorFields(this);

        //Concate text for sentMails column (just info column)
        sentMails = campOriginal.getCountOfSentMails() + " / " + campOriginal.getCountOfRecipients();

        //nastav pageToSend
        pageToSend = WebpagesService.getBasicDocFromUrl(campOriginal.getUrl());

        //zoznam nazvov skupin pouzivatelov
        userGroupsNames = UserGroupsDB.getInstance().convertIdsToNames(campOriginal.getUserGroupsIds());

        //Take userGroupIds string, split on Individual ids, convert to Integer, push inside recieversGroupsArray
        int[] userGroupsIds =  Tools.getTokensInt(campOriginal.getUserGroupsIds(), ",");
        List<Integer[]> splitPermsEmails = UserDetailsService.splitGroupsToPermsAndEmails(userGroupsIds);
        permisions = splitPermsEmails.get(0);
        emails = splitPermsEmails.get(1);

        if(campOriginal.getId() != -1) {
            //Set email status
            String currentStatus = EmailDB.getStaus(campOriginal.getId().intValue());

            if("disabled".equals(currentStatus)) {
                status = prop.getText("components.dmail.campaings.status.disabled");
            } else if ("enabled".equals(currentStatus)) {
                status = prop.getText("components.dmail.campaings.status.enabled");
            } else if ("all_sent".equals(currentStatus)) {
                status = prop.getText("components.dmail.campaings.status.all_sent");
            } else if ("unknown".equals(currentStatus)) {
                status = prop.getText("components.dmail.campaings.status.unknown");
            }
        }

        //process attachments, format is /files/path/filename.pdf;filename.pdf;/files/path/file2.pdf;file2.pdf;
        if (Tools.isNotEmpty(campOriginal.getAttachments())) {
            String[] tokens = Tools.getTokens(campOriginal.getAttachments(), ";");
            int fileCounter = 1;
            BeanWrapperImpl bw = new BeanWrapperImpl(this);
            for (String token : tokens) {
                if (token.startsWith("/")==false) continue;

                try {
                    bw.setPropertyValue("file"+fileCounter, token);
                } catch (Exception ex) {
                    Logger.error(CampaingsEditorFields.class, ex);
                }


                fileCounter++;
            }
        }
    }

    public void toCampaingEntity(CampaingsEntity campOriginal, HttpServletRequest request) {

        //Check if url was changed
        DocDB  docDB = DocDB.getInstance();
        if(campOriginal.getEditorFields().getPageToSend()!=null) {
            String docLink = docDB.getDocLink(campOriginal.getEditorFields().getPageToSend().getDocId(), null, true, request);
            if(!docLink.equals(campOriginal.getUrl())) campOriginal.setUrl(docLink);
        } else campOriginal.setUrl("");

        //Check if user groups was changed
        campOriginal.setUserGroupsIds(UserDetailsService.getUserGroupIds(campOriginal.getEditorFields().getPermisions(), campOriginal.getEditorFields().getEmails()));

        //Cannot be null
        if(campOriginal.getCountOfSentMails() == null) campOriginal.setCountOfSentMails(0);

        //process attachments, format is /files/path/filename.pdf;filename.pdf;/files/path/file2.pdf;file2.pdf;
        StringBuilder attachments = new StringBuilder();
        BeanWrapperImpl bw = new BeanWrapperImpl(campOriginal.getEditorFields());
        for (int fileCounter=1; fileCounter<=3; fileCounter++) {
            try {
                String path = (String)bw.getPropertyValue("file"+fileCounter);
                if (path==null || Tools.isEmpty(path)) continue;

                int separator = path.lastIndexOf("/");
                if (separator>1) {
                    String name = path.substring(separator+1);
                    attachments.append(path).append(";").append(name).append(";");
                }

            } catch (Exception ex) {
                Logger.error(CampaingsEditorFields.class, ex);
            }
        }
        if (attachments.length()<1) campOriginal.setAttachments(null);
        else campOriginal.setAttachments(attachments.toString());
    }

}
