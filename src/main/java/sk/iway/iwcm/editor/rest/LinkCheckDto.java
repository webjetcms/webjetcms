package sk.iway.iwcm.editor.rest;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter
public class LinkCheckDto {

    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;


    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title= "stat.graph.page",
        renderFormatLinkTemplate = "javascript:editWebPage({{id}});"
    )
    private String page;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title= "linkcheck.error"
    )
    private String error;


    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title= "linkcheck.link",
        renderFormatLinkTemplate = "javascript:openLink('{{link}}');"
    )
    private String link;
}
