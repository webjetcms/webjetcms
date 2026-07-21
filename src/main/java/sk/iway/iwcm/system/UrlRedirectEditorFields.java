package sk.iway.iwcm.system;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter
public class UrlRedirectEditorFields extends BaseEditorFields {

    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title = "webpages.icons.title",
        hiddenEditor = true,
        hidden = false,
        visible = true,
        className = "allow-html",
        orderable = false,
        sortAfter = "urlRedirectId"
    )
    private String statusIcons;

    public void fromUrlRedirect(UrlRedirectBean entity) {
        if (Boolean.TRUE.equals(entity.getManualRedirect())) {
            addStatusIcon("ti ti-user-filled");
        } else {
            addStatusIcon("ti ti-keyframes");
        }

        statusIcons = getStatusIconsHtml();
    }
}
