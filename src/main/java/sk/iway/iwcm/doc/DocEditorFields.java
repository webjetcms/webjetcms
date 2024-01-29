package sk.iway.iwcm.doc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.struts.util.ResponseUtils;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.users.userdetail.UserDetailsService;
import sk.iway.iwcm.doc.attributes.jpa.DocAtrDefEntity;
import sk.iway.iwcm.editor.DocNoteBean;
import sk.iway.iwcm.editor.DocNoteDB;
import sk.iway.iwcm.editor.rest.DocHistoryDto;
import sk.iway.iwcm.editor.rest.Field;
import sk.iway.iwcm.editor.service.EditorService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.spirit.model.Media;
/**
 * Doplnkove data fieldy potrebne pre editor
 */
@Getter
@Setter
public class DocEditorFields extends BaseEditorFields {

    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "webpages.icons.title",
        hiddenEditor = true, hidden = false, visible = true, sortAfter = "id", className = "allow-html", orderable = false
    )
    private String statusIcons;

    @DataTableColumn(inputType = DataTableColumnType.JSON, title="editor.superior_directory",
        tab = "basic", visible = false, filter=false, sortAfter = "externalLink", className = "dt-tree-group",
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "editor.position_in_structure")
                }
            )
        }
    )
    private GroupDetails groupDetails;

    @DataTableColumn(inputType = DataTableColumnType.JSON, title="editor.webpage_copy_in",
        tab = "basic", visible = false, filter=false, sortAfter = "editorFields.groupDetails", className = "dt-tree-group-array",
        editor = { @DataTableColumnEditor( attr = {
            @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after"),
            @DataTableColumnEditorAttr(key = "data-dt-json-addbutton", value = "editor.json.addWebpageCopy")
        } )} )
    private List<GroupDetails> groupCopyDetails;

    @DataTableColumn(inputType = DataTableColumnType.TEXTAREA, className = "wrap", title="editor.redactor_note",
        tab = "basic", visible = false, filter=false, sortAfter = "cacheable"
    )
    private String redactorNote = "";

    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX,
        title = "editor.access_restrictions_enable",
        tab = "access",
        visible = false,
        sortAfter = "loggedShowInSitemap",
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "editor.access_restrictions"),
                    @DataTableColumnEditorAttr(key = "unselectedValue", value = "")
                }
            )
        }
    )
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

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "&nbsp;",
        tab = "perex", hidden = true, sortAfter = "publishStartDate",
        editor = { @DataTableColumnEditor(
            options = {
                @DataTableColumnEditorAttr(key = "editor.public", value = "true")
            },
            message = "editor.public.tooltip"
        )
    })
    private boolean publishAfterStart = false;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "&nbsp;",
        tab = "perex", hidden = true, sortAfter = "publishEndDate",
        editor = { @DataTableColumnEditor(
            options = {
                @DataTableColumnEditorAttr(key = "editor.disableAfterEnd", value = "true")
            },
            message = "editor.disableAfterEnd.tooltip"
        )
    })
    private boolean disableAfterEnd = false;


    //special anotation, create a Media table inside media tab of docEditor (load Media's are connected to concrete doc by docId)
    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, title = "&nbsp;",
        tab = "media",
        hidden = true,
        editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/media?docId={docId}&groupId={groupId}"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.spirit.model.Media"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-tabs", value = "[{ 'id': 'basic', 'title': '[[#{datatable.tab.basic}]]', 'selected': true },{ 'id': 'fields', 'title': '[[#{editor.tab.fields}]]' }]")
            }
        )
    })
    private List<Media> media;

    //special anotation, create a Media table inside media tab of docEditor (load Media's are connected to concrete doc by docId)
    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, title = "&nbsp;",
        tab = "history",
        hidden = true,
        editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/web-pages/history?docId={docId}&groupId={groupId}"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.editor.rest.DocHistoryDto"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-serverSide", value = "false"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-order", value = "")
            }
        )
    })
    private List<DocHistoryDto> history;

    //zoznam volnych poli
    public List<Field> fieldsDefinition;

    //zoznam CSS stylov
    List<Map<String, String>> styleComboList;

    //true - verejne publikovat
    //false - ulozit pracovnu verziu do historie
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN, title="",tab="basic",hidden = true)
    boolean requestPublish=true;

    //nastavuje rezim editora podla skupiny sablon
    private String editingMode = "";
    private String editingModeLink = "";
    private String baseCssLink = "";

    //atributy stranky
    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "user.admin_edit.atrGroup",
        tab = "attributes",
        hidden = true,
        editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after")
            }
        )}
    )
    private String attrGroup;

    @DataTableColumn(inputType = DataTableColumnType.ATTRS, title = "&nbsp;",
        tab = "attributes",
        hidden = true
    )
    private List<DocAtrDefEntity> attrs;

    /**
     * Nastavi hodnoty atributov z DocDetails objektu
     * @param doc
     */
    public void fromDocDetails(DocBasic doc, boolean loadSubQueries) {
        DocDB docDB = DocDB.getInstance();
        GroupsDB groupsDB = GroupsDB.getInstance();
        GroupDetails group = groupsDB.getGroup(doc.getGroupId());
        if (group != null) {
            groupDetails = group;
        }

        //hlavna stranka adresara
        if (groupDetails != null && doc.getDocId()>0 && groupDetails.getDefaultDocId()==doc.getDocId()) {
            addRowClass("is-default-page");
        }

        //vypnute zobrazovanie
        if (doc.isAvailable()==false) addRowClass("is-not-public");
        setDisableAfterEnd(doc.isDisableAfterEnd());
        if (doc.isPublishAfterStart()) setPublishAfterStart(doc.isPublishAfterStart());

        //ikony
        if (group!=null && group.getDefaultDocId()==doc.getDocId()) addStatusIcon("fas fa-star");
        if (doc.isShowInMenu()) addStatusIcon("fas fa-map-marker-alt");
        else addStatusIcon("far fa-map-marker-alt-slash");
        if (Tools.isNotEmpty(doc.getExternalLink())) addStatusIcon("fas fa-external-link-alt");
        if (doc.isSearchable()==false) addStatusIcon("fas fa-eye-slash");
        if (Tools.isNotEmpty(doc.getPasswordProtected())) addStatusIcon("fas fa-lock");
        if (doc.getVirtualPath()!=null && doc.getVirtualPath().contains(Constants.getString("ABTestingName"))) addStatusIcon("fas fa-restroom");

        StringBuilder iconsHtml = new StringBuilder();
        //pridaj odkaz na zobrazenie stranky
        Prop prop = Prop.getInstance();
        String link = "/showdoc.do?docid="+doc.getDocId();
        if (doc instanceof DocHistory) {
            //v history je otocene docid a historyid
            link = "/showdoc.do?docid="+doc.getId()+"&historyId="+doc.getDocId();
        }
        iconsHtml.append("<a href=\""+link+"\" target=\"_blank\" class=\"preview-page-link\" title=\""+ResponseUtils.filter(prop.getText("history.showPage"))+"\"><i class=\"far fa-eye\"></i></a> ");
        iconsHtml.append(getStatusIconsHtml());
        statusIcons = iconsHtml.toString();

        int[] pp = Tools.getTokensInt(doc.getPasswordProtected(), ",");
        List<Integer[]> splitPermsEmails = UserDetailsService.splitGroupsToPermsAndEmails(pp);
        permisions = splitPermsEmails.get(0);
        emails = splitPermsEmails.get(1);

        if (loadSubQueries) {
            List<Integer> slaves = MultigroupMappingDB.getSlaveDocIds(doc.getDocId());
            if (slaves != null && slaves.size()>0) {
                groupCopyDetails = new ArrayList<>();
                for (Integer docId : slaves) {
                    DocDetails slave = docDB.getBasicDocDetails(docId.intValue(), false);
                    if (slave != null) {
                        group = groupsDB.getGroup(slave.getGroupId());
                        if (group != null) {
                            groupCopyDetails.add(group);
                        }
                    }
                }
            }

            DocNoteBean note = DocNoteDB.getInstance().getDocNote(doc.getDocId(), -1);
            if (note != null && Tools.isNotEmpty(note.getNote())) redactorNote = note.getNote();

            //definicia volnych poli
            if (doc.getTempId() > 0)
            {
                //nastavenie prefixu klucov podla skupiny sablon
                TemplateDetails temp = TemplatesDB.getInstance().getTemplate(doc.getTempId());
                if (temp != null && temp.getTemplatesGroupId()!=null && temp.getTemplatesGroupId().longValue() > 0) {
                    TemplatesGroupBean tgb = TemplatesGroupDB.getInstance().getById(temp.getTemplatesGroupId());
                    if (tgb != null && Tools.isNotEmpty(tgb.getKeyPrefix())) {
                        RequestBean.addTextKeyPrefix(tgb.getKeyPrefix(), false);
                    }
                    if (tgb != null && group != null) {
                        //nastav typ editora
                        setEditingMode(doc, temp, tgb, group, docDB);
                    }
                }

                RequestBean.addTextKeyPrefix("temp-"+doc.getTempId(), false);
            }
            fieldsDefinition = getFields(doc, "editor", 'T');

            if (doc instanceof DocDetails) {
                styleComboList = EditorService.getCssListJson(doc);
            }
        }

        //TODO: do DB modelu pridat
        // - objectADocId-objectDDocId co su IDecka objektov v sablone
        // - notLoggedNavbar
        // - notLoggedSitemap
        // - loggedShowInMenu
        // - loggedNavbar
        // - loggedSitemap
        // - passwordProtected

        //nastav editor URL
        if (Boolean.TRUE.equals(doc.getUrlInheritGroup())) {
            if (Tools.isEmpty(doc.getEditorVirtualPath())) {
                int i = doc.getVirtualPath().lastIndexOf("/");
                if (i!=-1) {
                    String editorPath = doc.getVirtualPath().substring(i);
                    if ("/".equals(editorPath)==false && editorPath.length()>1) {
                        editorPath = editorPath.substring(1);
                    }
                    doc.setEditorVirtualPath(editorPath);
                }
            }
        } else if(Boolean.TRUE.equals(doc.getGenerateUrlFromTitle())) {
            doc.setEditorVirtualPath("");
        } else {
            doc.setEditorVirtualPath("");
        }
    }

    /**
     * Nastavi rezim editacia (normal/pagebuilder) a linku pre iframe pagebuildera
     * @param doc
     * @param docDB
     * @param temp
     * @param tgb
     * @param group
     */
    private void setEditingMode(DocBasic doc, TemplateDetails temp, TemplatesGroupBean tgb, GroupDetails group, DocDB docDB) {
        editingMode = tgb.getInlineEditingMode();
        if (Tools.isNotEmpty(temp.getInlineEditingMode())) {
            editingMode = temp.getInlineEditingMode();
            if ("default".equals(editingMode)) editingMode = "";
        }

        baseCssLink = "/templates/"+tgb.getDirectory();

        //ak sablona alebo forward obsahuje pboff, tak PageBuilder nebude dostupny
        if (temp.getTempName().contains("PBoff") || temp.getForward().contains("-pboff") || temp.getForward().contains("_pboff")) editingMode = "";

        //ak sablona alebo forward obsahuje PBon, tak PageBuilder bude aktivovany
        if (temp.getTempName().contains("PBon") || temp.getForward().contains("-pbon") || temp.getForward().contains("_pbon")) editingMode = "pageBuilder";

        if (Tools.isNotEmpty(editingMode)) {
            editingModeLink = doc.getVirtualPath();

            if (doc.getDocId()<1 || Tools.isEmpty(editingModeLink)) {
                //asi je to podstranka, otvor fejkovanu hlavnu stranku adresara
                DocDetails groupDoc = docDB.getBasicDocDetails(group.getDefaultDocId(), false);
                if (groupDoc != null) {
                    editingModeLink = Tools.addParameterToUrlNoAmp(groupDoc.getVirtualPath(), "inlineEditingNewPage", "true");
                }
            }

            if (Tools.isNotEmpty(editingModeLink)) {
                editingModeLink = Tools.addParameterToUrlNoAmp(editingModeLink, "inlineEditorAdmin", "true");
                if (doc instanceof DocDetails) {
                    DocDetails docDocDetails = (DocDetails)doc;
                    if (docDocDetails.getHistoryId()>0) {
                        editingModeLink = Tools.addParameterToUrlNoAmp(editingModeLink, "historyid", String.valueOf(docDocDetails.getHistoryId()));
                    }
                }
            } else {
                //nepodarilo sa najst stranku pre editaciu
                editingMode = "";
            }
        }
    }

    /**
     * Nastavi hodnoty atributov nazad do DocDetails objektu
     * @param doc
     */
    public void toDocDetails(DocDetails doc) {
        doc.setPasswordProtected(UserDetailsService.getUserGroupIds(permisions, emails));

        doc.setGroupId(groupDetails.getGroupId());

        //Nastav editorVirtualPath do virtualPath, aby sa vygenerovala cela cesta
        if(Boolean.TRUE.equals(doc.getUrlInheritGroup())) {
            doc.setVirtualPath(doc.getEditorVirtualPath());
        }

        //Ak generateUrlFromTitle je true, vygeneruj hodnotu podla titulku nastavenim virtualPath na prazdnu hodnotu
        if(Boolean.TRUE.equals(doc.getGenerateUrlFromTitle())) {
            doc.setVirtualPath("");
            doc.setEditorVirtualPath("");
        }

        doc.setPublishAfterStart(publishAfterStart);
        doc.setDisableAfterEnd(disableAfterEnd);
    }
}