package sk.iway.iwcm.components.contentblock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.editor.rest.ComponentRequest;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.OptionDto;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@WebjetComponent("sk.iway.iwcm.components.contentblock.ContentBlockApp")
@WebjetAppStore(
    nameKey = "components.content-block.title",
    descKey = "components.content-block.desc",
    itemKey = "cmp_content-block",
    imagePath = "/components/content-block/editoricon.png",
    galleryImages = "/components/content-block/",
    componentPath = "/components/content-block/content-block.jsp")
@Getter
@Setter
public class ContentBlockApp extends WebjetComponentAbstract {
    
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        tab = "basic",
        title="components.inquiry.design"
    )
    private String type;
    
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        tab = "basic",
        title="components.htmlbox.titles", editor = {
        @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "placeholder", value = "components.htmlbox.titles")
            }
        )
    })
    private String title;

    @DataTableColumn(
        inputType = DataTableColumnType.ELFINDER,
        tab = "basic",
        title = "editor.perex.image",
        className = "image1"
    )
    private String image1;

    @DataTableColumn(
        inputType = DataTableColumnType.ELFINDER,
        tab = "basic",
        title="editor.perex.image",
        className = "image2"
    )
    private String image2;

    @DataTableColumn(
        inputType = DataTableColumnType.COLOR,
        tab = "basic",
        title="editor.div_properties.background_color")
    private String color;

    @DataTableColumn(inputType = DataTableColumnType.TEXT,
     tab = "basic", title="editor.div_properties.css_class", editor = {
        @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "placeholder", value = "editor.div_properties.css_class")
            }
        )
    })
    private String classes;

    @Override
    public Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request) {
        Map<String, List<OptionDto>> options = new HashMap<>();

        List<OptionDto> typeOptions = new ArrayList<>();
        Prop prop = Prop.getInstance(request);
        for(int i = 1; i <= Constants.getInt("contentBlockTypeCount"); i++) {
            typeOptions.add(
                new OptionDto(
                    prop.getText("components.inquiry.design") + " " + i,
                    i + "",
                    null
                )
            );
        }

        options.put("type", typeOptions);

        return options;
    }
}
