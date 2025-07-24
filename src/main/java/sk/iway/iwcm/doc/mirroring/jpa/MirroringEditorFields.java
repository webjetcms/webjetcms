package sk.iway.iwcm.doc.mirroring.jpa;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter
public class MirroringEditorFields extends BaseEditorFields {

    //Column for stutus icons
    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "webpages.icons.title",
        hiddenEditor = true, hidden = false, visible = true, sortAfter = "id", className = "allow-html", orderable = false
    )
    private String statusIcons;


    public void from(MirroringDTO entity) {
        StringBuilder iconsHtml = new StringBuilder();

        if(Tools.isTrue(entity.getMultipleErr())) {
            iconsHtml.append("<i class=\"ti ti-exclamation-circle\" style=\"color: #ff4b58;width: 1.25em;\"></i>");
            addRowClass("mirror-err");
        }

        if(Tools.isTrue(entity.getNestingWarn())) {
            iconsHtml.append("<i class=\"ti ti-alert-triangle\" style=\"color: #fabd00;width: 1.25em;\"></i>");
            addRowClass("mirror-warn");
        }

        iconsHtml.append( getStatusIconsHtml() );
        statusIcons = iconsHtml.toString();
    }
}
