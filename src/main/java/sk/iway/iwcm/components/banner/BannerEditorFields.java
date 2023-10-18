package sk.iway.iwcm.components.banner;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter
public class BannerEditorFields extends BaseEditorFields {

    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title="[[#{components.banner.viewable}]]",
		visible = true,
		tab = "main",
        sortAfter = "name",
        hiddenEditor = true
    )
    public boolean viewable = true;

}
