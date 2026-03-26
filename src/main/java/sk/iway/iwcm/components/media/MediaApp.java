package sk.iway.iwcm.components.media;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.editor.rest.ComponentRequest;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.OptionDto;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.spirit.MediaDB;
import sk.iway.spirit.model.MediaGroupBean;

@WebjetComponent("sk.iway.iwcm.components.media.MediaApp")
@WebjetAppStore(
    nameKey = "components.media.title",
    descKey = "components.media.desc",
    itemKey = "menuWebpages",
    imagePath = "/components/media/editoricon.png",
    galleryImages = "/components/media/",
    componentPath = "/components/media/media.jsp")
@Getter
@Setter
public class MediaApp extends WebjetComponentAbstract {

    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, renderFormat = "dt-format-select", title = "editor.media.group", tab = "basic",
    editor = {
        @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "unselectedValue", value = "-1")
            }
        )
    })
    private Integer[] groups;

    @DataTableColumn(inputType = DataTableColumnType.JSON, title = "components.popup.docid", tab = "basic", className = "dt-tree-page-null")
    private DocDetails docid;

    @Override
    public Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request) {
        Map<String, List<OptionDto>> options = new HashMap<>();

        int groupId = Tools.getIntValue(request.getParameter("groupId"), 0);
        List<MediaGroupBean> groups;
        if (groupId < 1) groups = MediaDB.getGroups();
        else groups = MediaDB.getGroups(groupId);

        options.put("groups", addOptions(groups, "mediaGroupName", "mediaGroupId", false));

        return options;
    }
}
