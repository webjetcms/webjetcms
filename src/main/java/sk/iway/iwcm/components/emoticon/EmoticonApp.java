package sk.iway.iwcm.components.emoticon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.editor.rest.ComponentRequest;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.DatatableTools;
import sk.iway.iwcm.system.datatable.OptionDto;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@WebjetComponent("sk.iway.iwcm.components.emoticon.EmoticonApp")
@WebjetAppStore(nameKey = "components.emoticon.title", descKey = "components.emoticon.desc", itemKey="cmp_emoticon", imagePath = "/components/emoticon/editoricon.png", galleryImages = "/components/emoticon/", componentPath = "", customHtml = "/apps/emoticon/admin/editor-component.html")
@Getter
@Setter
public class EmoticonApp extends WebjetComponentAbstract {
        @DataTableColumn(
        inputType = DataTableColumnType.IMAGE_RADIO,
        title = "&nbsp;",
        tab = "basic",
        className = "image-radio-horizontal image-radio-fullwidth"
    )
    private String emoticon;

    @Override
    public Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request) {
        Map<String, List<OptionDto>> options = new HashMap<>();
        options.put("emoticon", DatatableTools.getImageRadioOptions("/components/emoticon/admin-styles/"));
        return options;
    }
}