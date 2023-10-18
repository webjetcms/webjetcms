package sk.iway.iwcm.doc;

import java.util.Date;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.users.userdetail.UserDetailsService;
import sk.iway.iwcm.editor.rest.Field;
import sk.iway.iwcm.editor.rest.GroupSchedulerDto;
import sk.iway.iwcm.editor.service.WebpagesService;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@Getter
@Setter
public class GroupEditorField extends BaseEditorFields {

    @DataTableColumn(inputType = DataTableColumnType.JSON, title = "group.superior_directory", tab = "basic", visible = false, sortAfter = "editorFields.forceDomainNameChange", className = "dt-tree-group-root", editor = {
            @DataTableColumnEditor(attr = { @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before") }) })
    private GroupDetails parentGroupDetails;

    @DataTableColumn(inputType = DataTableColumnType.JSON, title = "[[#{groupedit.folde_main_site}]]", tab = "basic", visible = false, sortAfter = "editorFields.parentGroupDetails", className = "dt-tree-page")
    private DocDetails defaultDocDetails;

    @DataTableColumn(renderFormat = "dt-format-checkbox", title = "[[#{}]]", visible = false, editor = {
            @DataTableColumnEditor(type = "checkbox", tab = "basic", message = "editor.apply_for_all_sub_folders.tooltip", options = {
                    @DataTableColumnEditorAttr(key = "editor.apply_for_all_sub_folders", value = "true") }) })
    private boolean forceInternalToSubgroups = false;

    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, title = "[[#{editor.access_restrictions_enable}]]", tab = "access", visible = false, editor = {
            @DataTableColumnEditor(attr = {
                    @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "editor.access_restrictions"),
                    @DataTableColumnEditorAttr(key = "unselectedValue", value = "") }) })
    private Integer[] permisions;

    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX,
        title = "editor.access_restrictions_enable_email",
        tab = "access",
        visible = false,
        sortAfter = "editorFields.permisions",
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "menu.email"),
                    @DataTableColumnEditorAttr(key = "unselectedValue", value = "")
                }
            )
        }
    )
    private Integer[] emails;

    @DataTableColumn(renderFormat = "dt-format-checkbox", title = "[[#{}]]", visible = false, sortAfter = "editorFields.emails", editor = {
            @DataTableColumnEditor(type = "checkbox", tab = "access", attr = {
                    @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = ""),
                    @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after") }, options = {
                            @DataTableColumnEditorAttr(key = "editor.apply_for_all_sub_folders", value = "true") }) })
    private boolean passwordProtectedSubFolders;

    @DataTableColumn(inputType = DataTableColumnType.JSON, title = "[[#{groupedit.log_on_page}]]", tab = "access", visible = false, sortAfter = "editorFields.passwordProtectedSubFolders", className = "dt-tree-page")
    private DocDetails logonPage;

    @DataTableColumn(renderFormat = "dt-format-checkbox", title = "[[#{}]]", visible = false, sortAfter = "editorFields.logonPage", editor = {
            @DataTableColumnEditor(type = "checkbox", tab = "access", message = "[[#{editor.use_default_settings.tooltip}]]", options = {
                    @DataTableColumnEditorAttr(key = "editor.use_default_settings", value = "true") }) })
    private boolean useDefaultLogonPage;

    @DataTableColumn(renderFormat = "dt-format-checkbox", title = "[[#{}]]", visible = false, sortAfter = "menuType", editor = {
            @DataTableColumnEditor(type = "checkbox", tab = "menu", options = {
                    @DataTableColumnEditorAttr(key = "editor.apply_for_all_sub_folders", value = "true") }) })
    private boolean forceMenuTypeSubfolders = false;

    @DataTableColumn(renderFormat = "dt-format-checkbox", title = "[[#{}]]", visible = false, sortAfter = "loggedMenuType", editor = {
            @DataTableColumnEditor(type = "checkbox", tab = "menu", options = {
                    @DataTableColumnEditorAttr(key = "editor.apply_for_all_sub_folders", value = "true") }) })
    private boolean forceLoggedMenuTypeSubfolders = false;

    @DataTableColumn(renderFormat = "dt-format-checkbox", title = "[[#{}]]", visible = false, sortAfter = "newPageDocIdTemplate", editor = {
            @DataTableColumnEditor(type = "checkbox", tab = "template", options = {
                    @DataTableColumnEditorAttr(key = "editor.apply_for_all_sub_folders", value = "true") }) })
    private boolean forceNewPageDocIdTemplateSubFolders;

    @DataTableColumn(renderFormat = "dt-format-checkbox", title = "[[#{}]]", visible = false, sortAfter = "showInNavbar", editor = {
            @DataTableColumnEditor(type = "checkbox", tab = "menu", options = {
                    @DataTableColumnEditorAttr(key = "editor.apply_for_all_sub_folders", value = "true") }) })
    private boolean forceNavbarSubfolders;

    @DataTableColumn(renderFormat = "dt-format-checkbox", title = "[[#{}]]", visible = false, sortAfter = "showInSitemap", editor = {
            @DataTableColumnEditor(type = "checkbox", tab = "menu", attr = {
                    @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after") }, options = {
                            @DataTableColumnEditorAttr(key = "editor.apply_for_all_sub_folders", value = "true") }) })
    private boolean forceSitemapSubfolders;

    @DataTableColumn(renderFormat = "dt-format-checkbox", title = "[[#{}]]", visible = false, sortAfter = "loggedShowInNavbar", editor = {
            @DataTableColumnEditor(type = "checkbox", tab = "menu", options = {
                    @DataTableColumnEditorAttr(key = "editor.apply_for_all_sub_folders", value = "true") }) })
    private boolean forceLoggedNavbarSubfolders;

    @DataTableColumn(renderFormat = "dt-format-checkbox", title = "[[#{}]]", visible = false, sortAfter = "loggedShowInSitemap", editor = {
            @DataTableColumnEditor(type = "checkbox", tab = "menu", options = {
                    @DataTableColumnEditorAttr(key = "editor.apply_for_all_sub_folders", value = "true") }) })
    private boolean forceLoggedSitemapSubfolders;

    @DataTableColumn(renderFormat = "dt-format-checkbox", title = "[[#{groupedit.scheduler.plan}]]", visible = false, editor = {
            @DataTableColumnEditor(type = "checkbox", tab = "publishing", attr = {
                    @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "editor.publish_headline") }) })
    private boolean publishPlan;

    @DataTableColumn(renderFormat = "dt-format-date-time", title = "[[#{groupedit.scheduler.title}]]", visible = false, editor = {
            @DataTableColumnEditor(type = "datetime", label = "[[#{groupedit.scheduler.title}]]", tab = "publishing") })
    private Date publishDate;

    @DataTableColumn(renderFormat = "dt-format-checkbox", title = "[[#{}]]", visible = false, sortAfter = "urlDirName", editor = {
            @DataTableColumnEditor(type = "checkbox", tab = "basic", options = {
                    @DataTableColumnEditorAttr(key = "editor.apply_for_all_sub_folders_and_sub_pages", value = "true") }) })
    private boolean forceUrlDirNameChange;

    @DataTableColumn(renderFormat = "dt-format-checkbox", title = "[[#{}]]", visible = false, sortAfter = "domainName", editor = {
            @DataTableColumnEditor(type = "checkbox", tab = "basic", options = {
                    @DataTableColumnEditorAttr(key = "groupedit.changeDomainRedirects", value = "true") }) })
    private boolean forceDomainNameChange;

    @DataTableColumn(renderFormat = "dt-format-checkbox", title = "[[#{}]]", visible = false, sortAfter = "sortPriority", editor = {
            @DataTableColumnEditor(type = "checkbox", tab = "menu", options = {
                    @DataTableColumnEditorAttr(key = "groupedit.priority.regenerate_page_order", value = "true") }) })
    private boolean forcePriorityRecalculation;

    //Special anotation, get inner table into publishing tab, table type groupSchedulerDto. Beware, sererSide must by true because we use repo in this case
    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, title = "&nbsp;",
        tab = "publishing",
        editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/group-scheduler?groupId={groupId}&selectType=plannedChanges"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.editor.rest.GroupSchedulerDto"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-serverSide", value = "true"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-order", value = "3,desc"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-hideButtons", value = "create,edit,duplicate,import,celledit")
            }
        )
    })
    private List<GroupSchedulerDto> groupSchedulerPlannedChanges;

    //Special anotation, get inner table into history tab, table type groupSchedulerDto. Beware, sererSide must by true because we use repo in this case
    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, title = "&nbsp;",
        tab = "history",
        editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/group-scheduler?groupId={groupId}&selectType=changeHistory"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.editor.rest.GroupSchedulerDto"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-serverSide", value = "true"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-order", value = "2,desc"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-hideButtons", value = "create,edit,duplicate,remove,import,celledit"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-removeColumns", value = "whenToPublish,datePublished")
            }
        )
    })
    private List<GroupSchedulerDto> groupSchedulerChangeHistory;

    @DataTableColumn(
        renderFormat = "dt-format-checkbox",
        title = "[[#{}]]",
        className = "DTE_Field_Has_Checkbox",
        visible = false,
        sortAfter = "tempId",
        editor = {
                @DataTableColumnEditor(
                        type = "checkbox",
                        tab = "template",
                        options = {
                                        @DataTableColumnEditorAttr(key = "editor.apply_for_all_sub_folders_and_sub_pages", value = "true")
                        }
                )
        }
    )
    private boolean forceTemplateToSubgroupsAndPages;

    @DataTableColumn(
        renderFormat = "dt-format-checkbox",
        title = "[[#{}]]",
        visible = false,
        sortAfter = "lng",
        editor = {
                        @DataTableColumnEditor(
                                        type = "checkbox",
                                        tab = "template",
                                        attr = {
                                                        @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after")
                                        },
                                        options = {
                                                        @DataTableColumnEditorAttr(key = "editor.apply_for_all_sub_folders", value = "true")
                                        }
                        )
        }
    )
    private boolean forceLngToSubFolders;

    //zoznam volnych poli
    public List<Field> fieldsDefinition;

    /**
     * POZOR: tato metoda vytovir KLON group objektu a vrati ho, zaroven mu setne
     * tento EditorFields
     *
     * je to z toho dovodu, ze GroupDetails je cachovane a potom sa nam menili cachovane zaznamy
     *
     * @param group
     * @return
     */
    public GroupDetails fromGroupDetails(GroupDetails groupOriginal) {

        GroupDetails group = null;
        try {
            group = (GroupDetails) groupOriginal.clone();

            setLogonPage(WebpagesService.getBasicDoc(group.getLogonPageDocId()));

            setParentGroupDetails(WebpagesService.getGroup(group.getParentGroupId()));

            setDefaultDocDetails(WebpagesService.getBasicDoc(group.getDefaultDocId()));

            int pp[] = Tools.getTokensInt(group.getPasswordProtected(), ",");
            List<Integer[]> splitPermsEmails = UserDetailsService.splitGroupsToPermsAndEmails(pp);
            permisions = splitPermsEmails.get(0);
            emails = splitPermsEmails.get(1);

            //definicia volnych poli
            if (group.getTempId() > 0)
            {
                //nastavenie prefixu klucov podla skupiny sablon
                TemplateDetails temp = TemplatesDB.getInstance().getTemplate(group.getTempId());
                if (temp != null && temp.getTemplatesGroupId()!=null && temp.getTemplatesGroupId().longValue() > 0) {
                    TemplatesGroupBean tgb = TemplatesGroupDB.getInstance().getById(temp.getTemplatesGroupId());
                    if (tgb != null && Tools.isNotEmpty(tgb.getKeyPrefix())) {
                        RequestBean.addTextKeyPrefix(tgb.getKeyPrefix(), false);
                    }
                }

                RequestBean.addTextKeyPrefix("temp-"+group.getTempId(), false);
            }
            fieldsDefinition = getFields(group, "groupedit", 'D');

            group.setEditorFields(this);
        } catch (CloneNotSupportedException e) {
            Logger.error(GroupEditorField.class, e);
        }
        return group;
    }

    public void toGroupDetails(GroupDetails group) {
        //Validation is secure by validateEditor in GroupRestController
        group.setPasswordProtected(UserDetailsService.getUserGroupIds(permisions, emails));

        group.setParentGroupId(group.getEditorFields().parentGroupDetails.getGroupId());

        group.setDefaultDocId(group.getEditorFields().defaultDocDetails.getDocId());

        if(group.getEditorFields().isUseDefaultLogonPage() == true) {
            group.setLogonPageDocId(0);
        } else {
            group.setLogonPageDocId(group.getEditorFields().getLogonPage().getDocId());
        }
    }
}
