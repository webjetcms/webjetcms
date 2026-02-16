package sk.iway.iwcm.components.apphtmlbox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.PageParams;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.layout.DocDetailsDto;
import sk.iway.iwcm.common.FileBrowserTools;
import sk.iway.iwcm.common.WriteTagToolsForCore;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.editor.rest.ComponentRequest;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.OptionDto;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@WebjetComponent("sk.iway.iwcm.components.apphtmlbox.HtmlBoxApp")
@WebjetAppStore(
    nameKey = "components.htmlbox.title",
    descKey = "components.htmlbox.desc",
    itemKey = "app-htmlbox",
    imagePath = "/components/htmlbox/editoricon.png",
    componentPath = "/components/htmlbox/showdoc.jsp,/components/htmlbox/editor_component.jsp",
    customHtml = "/apps/app-htmlbox/admin/editor-component.html",
    galleryImages = "/components/htmlbox/"
)
@Getter
@Setter
public class HtmlBoxApp extends WebjetComponentAbstract {

    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.htmlbox.code_type", tab = "basic")
    private String codeType;

    @DataTableColumn(
        inputType = DataTableColumnType.RADIO,
        title = "components.htmlbox.insertType",
        tab = "basic",
        editor = {
            @DataTableColumnEditor(
                options = {
                    @DataTableColumnEditorAttr(
                        key = "components.htmlbox.insertType.static",
                        value = "static"),
                    @DataTableColumnEditorAttr(
                        key = "components.htmlbox.insertType.dynamic",
                        value = "dynamic")
                }
            )
        }
    )
    private String docStyle = "static";

    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.htmlbox.block_type", tab = "basic")
    private String blockType;

    private HashMap<String, String> customPageLinks = new HashMap<>();

    @DataTableColumn(inputType = DataTableColumnType.JSON, title = "components.popup.docid", tab = "basic", className = "dt-tree-page-null")
    private DocDetailsDto docDetails = null;

    private List<OptionDto> getBlocksOptions(HttpServletRequest request) {
        List<OptionDto> blocks = new ArrayList<>();
        // Scan directory and list all folders to selectbox
        IwcmFile dir = new IwcmFile(Tools.getRealPath("/components/" + Constants.getInstallName() + "/htmlbox/objects"));
        if (dir.exists() == false) {
            dir = new IwcmFile(Tools.getRealPath("/components/htmlbox/objects"));
        }

        if (dir.exists() && dir.canRead()) {
            IwcmFile[] files = FileTools.sortFilesByName(dir.listFiles());

            if (files != null) {
                for (IwcmFile f : files) {
                    if (f.isDirectory()) {
                        String dirName = f.getName();

                        if (FileBrowserTools.hasForbiddenSymbol(dirName)) {
                            continue;
                        }

                        String dirNameDecoded = dirName.replace('_', ' ');
                        blocks.add(new OptionDto(Tools.escapeHtml(dirNameDecoded), Tools.escapeHtml(dirName), null));
                    }
                }
            }
        }

        for (OptionDto option : blocks) {
            customPageLinks.put(option.getValue(), WriteTagToolsForCore.getCustomPage("/components/htmlbox/html_temp-ajax.jsp", request) + "?dirName=" + option.getValue());
        }

        return blocks;
    }

    private List<OptionDto> getInsertCodeTypeOptions(HttpServletRequest request) {
        Prop prop = Prop.getInstance(request);

        List<OptionDto> insertCodeTypeOptions = new ArrayList<>();
        insertCodeTypeOptions.add( new OptionDto(prop.getText("components.htmlbox.block"), "block", null) );

        // Doc directly
        insertCodeTypeOptions.add( new OptionDto(prop.getText("user.editablePages.label"), "doc", null) );

        // Main template
        GroupsDB groupsDB = GroupsDB.getInstance();
        GroupDetails templateGroup = groupsDB.getGroup(Constants.getInt("tempGroupId"));
        String mainPrefix = prop.getText("components.htmlbox.main_template") + ": ";
        if (templateGroup != null) insertCodeTypeOptions.add( new OptionDto(mainPrefix + templateGroup.getGroupName(), "templateGroupId_" + templateGroup.getGroupId(), null) );
        else insertCodeTypeOptions.add( new OptionDto(mainPrefix, "templateGroupId_-1", null) );

        // Sub templates if main exist
        if (templateGroup != null) {
            String prefix = prop.getText("components.htmlbox.child_template") + ": ";
            for(GroupDetails templatesGroup : groupsDB.getGroups(templateGroup.getGroupId()) )
                insertCodeTypeOptions.add( new OptionDto(prefix + templatesGroup.getGroupName(), "templateGroupId_" + templatesGroup.getGroupId(), null) );
        }

        return insertCodeTypeOptions;
    }

    @Override
    public Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request) {
        Map<String, List<OptionDto>> options = new HashMap<>();
        options.put("blockType", getBlocksOptions(request));
        options.put("codeType", getInsertCodeTypeOptions(request));
        return options;
    }

    @Override
    public void initAppEditor(ComponentRequest componentRequest, HttpServletRequest request) {
        PageParams pageParams = new PageParams(componentRequest.getParameters());
        int docId = Tools.getIntValue(pageParams.getValue("docid", "-1"), -1);
        if(docId > 0) this.docDetails = new DocDetailsDto( DocDB.getInstance().getDoc(docId) );
    }
}
