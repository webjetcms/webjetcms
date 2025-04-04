package sk.iway.iwcm.doc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.DocDetailsFullPathDeserializer;
import sk.iway.iwcm.system.datatable.TemplateGroupNameDeserializer;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;

/**
 * Drzi zaznam z tabulky templates
 *
 * @author $Author: jeeff $
 * @version $Revision: 1.2 $
 * @Title Interway Content Management
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2002
 * @created $Date: 2003/11/23 13:22:41 $
 */
public class TemplateDetails {

    @DataTableColumn(
        data = "tempId",
        inputType = DataTableColumnType.ID
    )
    private int tempId;

    @NotBlank
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title = "components.news.template_title",
        tab = "basic"
    )
    private String tempName;

    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title = "admin.temp_group_list.editor.title",
        tab = "basic"
    )
    @JsonDeserialize(using = TemplateGroupNameDeserializer.class)
    private Long templatesGroupId;

    private String templatesGroupName;

    @NotBlank
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title = "admin.temps_list.jazyk",
        tab = "basic"
    )
    private String lng = "";

    /**
     * V akych skupinach je mozne sablonu pouzit
     */
    @JsonIgnore
    private String availableGroups;

    @DataTableColumn(inputType = DataTableColumnType.JSON, title="admin.temp.edit.showForDir",
        tab = "accessTab", className = "dt-tree-group-array")
    private List<GroupDetails> availableGrooupsList;

    @NotBlank
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "temp_edit.forward",
        tab = "basic",
        editor = {
			@DataTableColumnEditor(
				attr = {
					@DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/rest/templates/temps-list/autocomplete"),
					@DataTableColumnEditorAttr(key = "data-ac-min-length", value = "1"),
                    @DataTableColumnEditorAttr(key = "data-ac-params", value = "#DTE_Field_templateInstallName,#DTE_Field_templatesGroupId"),
					@DataTableColumnEditorAttr(key = "data-ac-select", value = "true")
				}
			)
		}
    )
    private String forward;

    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title = "admin.temp_group_list.inline_editing_mode",
        tab = "basic"
    )
    private String inlineEditingMode;

    @DataTableColumn(
            inputType = DataTableColumnType.SELECT,
            tab = "templatesTab",
            title = "temp_edit.header",
            editor = {
                @DataTableColumnEditor(attr = {
                        @DataTableColumnEditorAttr(key = "data-dt-edit-url", value = "/admin/v9/webpages/web-pages-list/?groupid=SYSTEM&docid={id}"),
                        @DataTableColumnEditorAttr(key = "data-dt-edit-perms", value = "menuTemplates")
                })
            }
    )
    @JsonDeserialize(using = DocDetailsFullPathDeserializer.class)
    private int headerDocId;

    @JsonIgnore
    private String headerDocData = null;

    @DataTableColumn(
            inputType = DataTableColumnType.SELECT,
            tab = "templatesTab",
            title = "temp_edit.footer",
            editor = {
                @DataTableColumnEditor(attr = {
                        @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after"),
                        @DataTableColumnEditorAttr(key = "data-dt-edit-url", value = "/admin/v9/webpages/web-pages-list/?groupid=SYSTEM&docid={id}"),
                        @DataTableColumnEditorAttr(key = "data-dt-edit-perms", value = "menuTemplates")
                })
            }
    )
    @JsonDeserialize(using = DocDetailsFullPathDeserializer.class)
    private int footerDocId;

    @JsonIgnore
    private String footerDocData = null;

    @DataTableColumn(
            inputType = DataTableColumnType.SELECT,
            tab = "templatesTab",
            title = "templates.temps-list.main-navigation",
            editor = {
                @DataTableColumnEditor(attr = {
                        @DataTableColumnEditorAttr(key = "data-dt-edit-url", value = "/admin/v9/webpages/web-pages-list/?groupid=SYSTEM&docid={id}"),
                        @DataTableColumnEditorAttr(key = "data-dt-edit-perms", value = "menuTemplates")
                })
            }
    )
    @JsonDeserialize(using = DocDetailsFullPathDeserializer.class)
    private int menuDocId;

    @JsonIgnore
    private String menuDocData = null;

    @DataTableColumn(
            inputType = DataTableColumnType.SELECT,
            tab = "templatesTab",
            title = "templates.temps-list.side-navigation",
            editor = {
                @DataTableColumnEditor(attr = {
                        @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after"),
                        @DataTableColumnEditorAttr(key = "data-dt-edit-url", value = "/admin/v9/webpages/web-pages-list/?groupid=SYSTEM&docid={id}"),
                        @DataTableColumnEditorAttr(key = "data-dt-edit-perms", value = "menuTemplates")
                })
            }
    )
    @JsonDeserialize(using = DocDetailsFullPathDeserializer.class)
    private int rightMenuDocId;

    @JsonIgnore
    private String rightMenuDocData = null;

    @DataTableColumn(
            inputType = DataTableColumnType.SELECT,
            tab = "templatesTab",
            title = "templates.temps-list.object-a",
            editor = {
                @DataTableColumnEditor(attr = {
                        @DataTableColumnEditorAttr(key = "data-dt-edit-url", value = "/admin/v9/webpages/web-pages-list/?groupid=SYSTEM&docid={id}"),
                        @DataTableColumnEditorAttr(key = "data-dt-edit-perms", value = "menuTemplates")
                })
            }
    )
    @JsonDeserialize(using = DocDetailsFullPathDeserializer.class)
    private int objectADocId;

    @DataTableColumn(
            inputType = DataTableColumnType.SELECT,
            tab = "templatesTab",
            title = "templates.temps-list.object-b",
            editor = {
                @DataTableColumnEditor(attr = {
                        @DataTableColumnEditorAttr(key = "data-dt-edit-url", value = "/admin/v9/webpages/web-pages-list/?groupid=SYSTEM&docid={id}"),
                        @DataTableColumnEditorAttr(key = "data-dt-edit-perms", value = "menuTemplates")
                })
            }
    )
    @JsonDeserialize(using = DocDetailsFullPathDeserializer.class)
    private int objectBDocId;

    @DataTableColumn(
            inputType = DataTableColumnType.SELECT,
            tab = "templatesTab",
            title = "templates.temps-list.object-c",
            editor = {
                @DataTableColumnEditor(attr = {
                        @DataTableColumnEditorAttr(key = "data-dt-edit-url", value = "/admin/v9/webpages/web-pages-list/?groupid=SYSTEM&docid={id}"),
                        @DataTableColumnEditorAttr(key = "data-dt-edit-perms", value = "menuTemplates")
                })
            }
    )
    @JsonDeserialize(using = DocDetailsFullPathDeserializer.class)
    private int objectCDocId;

    @DataTableColumn(
            inputType = DataTableColumnType.SELECT,
            tab = "templatesTab",
            title = "templates.temps-list.object-d",
            editor = {
                @DataTableColumnEditor(attr = {
                        @DataTableColumnEditorAttr(key = "data-dt-edit-url", value = "/admin/v9/webpages/web-pages-list/?groupid=SYSTEM&docid={id}"),
                        @DataTableColumnEditorAttr(key = "data-dt-edit-perms", value = "menuTemplates")
                })
            }
    )
    @JsonDeserialize(using = DocDetailsFullPathDeserializer.class)
    private int objectDDocId;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, title = "&nbsp;",
        tab = "folders",
        editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/groups?tempId={tempId}"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.doc.GroupDetails"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-forceVisibleColumns", value = "groupId,groupName,fullPath"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-updateColumnsFunction", value = "updateColumnsGroupDetails"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-hideButtons", value = "create,edit,duplicate,remove,import,celledit"),
                @DataTableColumnEditorAttr(key = "data-dt-field-full-headline", value = "templates.groups_title"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-serverSide", value = "false")
            }
        )
    })
    private List<GroupDetails> groupDetailsList;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, title = "&nbsp;",
        tab = "sites",
        editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/web-pages?tempId={tempId}"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.doc.DocDetails"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-forceVisibleColumns", value = "id,title,fullPath"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-updateColumnsFunction", value = "updateColumnsDocDetails"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-hideButtons", value = "create,edit,duplicate,remove,import,celledit"),
                @DataTableColumnEditorAttr(key = "data-dt-field-full-headline", value = "templates.doc_title"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-serverSide", value = "true")
            }
        )
    })
    private List<DocDetails> docDetailsList;

    @JsonIgnore
    private String objectADocData = null;

    @JsonIgnore
    private String objectBDocData = null;

    @JsonIgnore
    private String objectCDocData = null;

    @JsonIgnore
    private String objectDDocData = null;

    /**
     * Moznost definovania InstallName per sablona (prepise aktualnu hodnotu z Constants)
     */
    @DataTableColumn(inputType=DataTableColumnType.TEXT, title="temp_edit.install_name", tab = "basic")
    private String templateInstallName;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title = "temp_edit.base_css_style",
        tab = "style"
    )
    private String baseCssPath;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title = "templates.temps-list.css",
        tab = "style",
        editor = @DataTableColumnEditor(
            attr = @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after")
        )
    )
    private String css;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title = "components.news.template_html",
        tab = "style"
    )
    private String afterBodyData = "";

    /**
     * Moznost vypnutia spam ochrany per sablona
     */
    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title = "templates.temps-list.spam",
        tab = "basic"
    )
    private boolean disableSpamProtection = false;

    @DataTableColumn(
        inputType = DataTableColumnType.DISABLED,
        title = "admin.temps_list.pocet_pouziti",
        renderFormat = "dt-format-number", //v DT to chceme ako cislo
        sortAfter = "templatesGroupId",
        editor = {
            @DataTableColumnEditor(
                    type = "text",
                    attr = @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before")
            )
        },
        className = "hide-on-create"
    )
    private int pocetPouziti;

    @DataTableColumnNested
	@Transient
	private TemplateDetailEditorFields editorFields = null;

    public int getPocetPouziti() {
        return pocetPouziti;
    }

    public void setPocetPouziti(int pocetPouziti) {
        this.pocetPouziti = pocetPouziti;
    }

    /**
     * Gets the tempId attribute of the TemplateDetails object
     *
     * @return The tempId value
     */
    public int getTempId() {
        return tempId;
    }

    /**
     * Sets the tempId attribute of the TemplateDetails object
     *
     * @param newTempId The new tempId value
     */
    public void setTempId(int newTempId) {
        tempId = newTempId;
    }

    /**
     * Sets the tempName attribute of the TemplateDetails object
     *
     * @param newTempName The new tempName value
     */
    public void setTempName(String newTempName) {
        tempName = newTempName;
    }

    /**
     * Gets the tempName attribute of the TemplateDetails object
     *
     * @return The tempName value
     */
    public String getTempName() {
        return tempName;
    }

    /**
     * Sets the forward attribute of the TemplateDetails object
     *
     * @param newForward The new forward value
     */
    public void setForward(String newForward) {
        forward = newForward;
    }

    /**
     * Gets the forward attribute of the TemplateDetails object
     *
     * @return The forward value
     */
    public String getForward() {
        return forward;
    }

    /**
     * Sets the headerDocId attribute of the TemplateDetails object
     *
     * @param newHeaderDocId The new headerDocId value
     */
    public void setHeaderDocId(int newHeaderDocId) {
        headerDocId = newHeaderDocId;
    }

    /**
     * Gets the headerDocId attribute of the TemplateDetails object
     *
     * @return The headerDocId value
     */
    public int getHeaderDocId() {
        return headerDocId;
    }

    /**
     * Sets the headerDocData attribute of the TemplateDetails object
     *
     * @param newHeaderDocData The new headerDocData value
     */
    public void setHeaderDocData(String newHeaderDocData) {
        headerDocData = newHeaderDocData;
    }

    /**
     * Gets the headerDocData attribute of the TemplateDetails object
     *
     * @return The headerDocData value
     */
    public String getHeaderDocData() {
        if (headerDocData==null) headerDocData = TemplatesDB.getDocData(getHeaderDocId());
        return headerDocData;
    }

    /**
     * Sets the footerDocId attribute of the TemplateDetails object
     *
     * @param newFooterDocId The new footerDocId value
     */
    public void setFooterDocId(int newFooterDocId) {
        footerDocId = newFooterDocId;
    }

    /**
     * Gets the footerDocId attribute of the TemplateDetails object
     *
     * @return The footerDocId value
     */
    public int getFooterDocId() {
        return footerDocId;
    }

    /**
     * Sets the footerDocData attribute of the TemplateDetails object
     *
     * @param newFooterDocData The new footerDocData value
     */
    public void setFooterDocData(String newFooterDocData) {
        footerDocData = newFooterDocData;
    }

    /**
     * Gets the footerDocData attribute of the TemplateDetails object
     *
     * @return The footerDocData value
     */
    public String getFooterDocData() {
        if (footerDocData==null) footerDocData = TemplatesDB.getDocData(getFooterDocId());
        return footerDocData;
    }

    /**
     * Sets the afterBodyData attribute of the TemplateDetails object
     *
     * @param newAfterBodyData The new afterBodyData value
     */
    public void setAfterBodyData(String newAfterBodyData) {
        afterBodyData = newAfterBodyData;
    }

    /**
     * Gets the afterBodyData attribute of the TemplateDetails object
     *
     * @return The afterBodyData value
     */
    public String getAfterBodyData() {
        return afterBodyData;
    }

    /**
     * Sets the css attribute of the TemplateDetails object
     *
     * @param newCss The new css value
     */
    public void setCss(String newCss) {
        css = newCss;
    }

    /**
     * Gets the css attribute of the TemplateDetails object
     *
     * @return The css value
     */
    public String getCss() {
        return css;
    }

    public void setMenuDocId(int menuDocId) {
        this.menuDocId = menuDocId;
    }

    public int getMenuDocId() {
        return menuDocId;
    }

    public void setMenuDocData(String menuDocData) {
        this.menuDocData = menuDocData;
    }

    public String getMenuDocData() {
        if (menuDocData==null) menuDocData = TemplatesDB.getDocData(getMenuDocId());
        return menuDocData;
    }

    public void setLng(String lng) {
        this.lng = lng;
    }

    public String getLng() {
        return lng;
    }

    public String getRightMenuDocData() {
        if (rightMenuDocData==null) rightMenuDocData = TemplatesDB.getDocData(getRightMenuDocId());
        return rightMenuDocData;
    }

    public void setRightMenuDocData(String rightMenuDocData) {
        this.rightMenuDocData = rightMenuDocData;
    }

    public int getRightMenuDocId() {
        return rightMenuDocId;
    }

    public void setRightMenuDocId(int rightMenuDocId) {
        this.rightMenuDocId = rightMenuDocId;
    }

    public String getBaseCssPath() {
        if (Tools.isEmpty(baseCssPath)) {
            baseCssPath = "/css/page.css";
        }
        return baseCssPath;
    }

    public void setBaseCssPath(String baseCssPath) {
        this.baseCssPath = baseCssPath;
    }

    public int getObjectADocId() {
        return objectADocId;
    }

    public void setObjectADocId(int objectADocId) {
        this.objectADocId = objectADocId;
    }

    public int getObjectBDocId() {
        return objectBDocId;
    }

    public void setObjectBDocId(int objectBDocId) {
        this.objectBDocId = objectBDocId;
    }

    public int getObjectCDocId() {
        return objectCDocId;
    }

    public void setObjectCDocId(int objectCDocId) {
        this.objectCDocId = objectCDocId;
    }

    public int getObjectDDocId() {
        return objectDDocId;
    }

    public void setObjectDDocId(int objectDDocId) {
        this.objectDDocId = objectDDocId;
    }

    public String getObjectADocData() {
        if (objectADocData==null) objectADocData = TemplatesDB.getDocData(getObjectADocId());
        return objectADocData;
    }

    public void setObjectADocData(String objectADocData) {
        this.objectADocData = objectADocData;
    }

    public String getObjectBDocData() {
        if (objectBDocData==null) objectBDocData = TemplatesDB.getDocData(getObjectBDocId());
        return objectBDocData;
    }

    public void setObjectBDocData(String objectBDocData) {
        this.objectBDocData = objectBDocData;
    }

    public String getObjectCDocData() {
        if (objectCDocData==null) objectCDocData = TemplatesDB.getDocData(getObjectCDocId());
        return objectCDocData;
    }

    public void setObjectCDocData(String objectCDocData) {
        this.objectCDocData = objectCDocData;
    }

    public String getObjectDDocData() {
        if (objectDDocData==null) objectDDocData = TemplatesDB.getDocData(getObjectDDocId());
        return objectDDocData;
    }

    public void setObjectDDocData(String objectDDocData) {
        this.objectDDocData = objectDDocData;
    }

    public String getAvailableGroups() {
        return availableGroups;
    }

    @JsonIgnore
    public int[] getAvailableGroupsInt() {
        return Tools.getTokensInt(availableGroups, ",");
    }

    public void setAvailableGroups(String availableGroups) {
        this.availableGroups = availableGroups;
    }

    public String getTemplateInstallName() {
        return templateInstallName;
    }

    public void setTemplateInstallName(String templateInstallName) {
        this.templateInstallName = templateInstallName;
    }

    public boolean isDisableSpamProtection() {
        return disableSpamProtection;
    }

    public void setDisableSpamProtection(boolean disableSpamProtection) {
        this.disableSpamProtection = disableSpamProtection;
    }

    public Long getTemplatesGroupId() {
        return templatesGroupId;
    }

    public void setTemplatesGroupId(Long templatesGroupId) {
        this.templatesGroupId = templatesGroupId;
    }

    //kvoli spatnej kompatibilite import/export medzi WebJETmi
    public void setTemplatesGroupId(int templatesGroupId) {
        this.templatesGroupId = Long.valueOf(templatesGroupId);
    }

    public String getTemplatesGroupName() {
        return templatesGroupName;
    }

    public void setTemplatesGroupName(String templatesGroupName) {
        this.templatesGroupName = templatesGroupName;
    }

    public boolean retrieveForwardExist() {
        try {
            File forwardFile = new File(sk.iway.iwcm.Tools.getRealPath("/templates/" + Constants.getInstallName() + "/" + this.forward));
            return forwardFile.exists();
        } catch (Exception e) {
            sk.iway.iwcm.Logger.error(e);
        }
        return false;
    }

    public List<GroupDetails> getAvailableGrooupsList() {
        List<GroupDetails> list = new ArrayList<>();
        GroupsDB groupsDB = GroupsDB.getInstance();
        for (int groupId : getAvailableGroupsInt()) {
            GroupDetails group = groupsDB.getGroup(groupId);
            if (group != null) list.add(group);
        }
        return list;
    }

    public void setAvailableGrooupsList(List<GroupDetails> availableGrooupsList) {
        StringBuilder groupIds = new StringBuilder("");
        for (GroupDetails group : availableGrooupsList) {
            if (groupIds.length()>0) groupIds.append(",");
            groupIds.append(String.valueOf(group.getGroupId()));
        }
        availableGroups = groupIds.toString();
    }

    public TemplateDetailEditorFields getEditorFields() {
        return editorFields;
    }

    public void setEditorFields(TemplateDetailEditorFields editorFields) {
        this.editorFields = editorFields;
    }

    public String getInlineEditingMode() {
        return inlineEditingMode;
    }

    public void setInlineEditingMode(String inlineEditingMode) {
        this.inlineEditingMode = inlineEditingMode;
    }
}
