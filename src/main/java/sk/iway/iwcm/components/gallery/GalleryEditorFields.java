package sk.iway.iwcm.components.gallery;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@Getter
@Setter
public class GalleryEditorFields {

    @DataTableColumn(
        inputType = DataTableColumnType.CHECKBOX,
        title = "[[#{editor.perex.group}]]",
        tab = "metadata",
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "unselectedValue", value = "")
                }
            )
        }
    )
	private String[] perexGroupsIds;

    public void fromGalleryEntity(GalleryEntity entityOriginal) {
        // Get perex group ids as String, parse String and ids set into perexGroupsIds array
        perexGroupsIds = Tools.getTokens(entityOriginal.getPerexGroup(), ",");
    }

    public void toGalleryEntity(GalleryEntity entityOriginal) {
        // From array perexGroupIds build String perexGroupString (perex group ids separated using comma)
        // Next set his builded string as new exportDatBeanOriginal.perexGroup value
        StringBuilder perexGroupStringBuilder = new StringBuilder();
        for (String perexGroupId : entityOriginal.getEditorFields().getPerexGroupsIds()) {
            if (perexGroupStringBuilder.length() > 0) {
                perexGroupStringBuilder.append(",");
            }
            perexGroupStringBuilder.append(perexGroupId);
        }
        entityOriginal.setPerexGroup(perexGroupStringBuilder.toString());
    }
}