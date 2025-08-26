package sk.iway.iwcm.components.gallery;

import javax.validation.constraints.NotBlank;

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

    @DataTableColumn(inputType = DataTableColumnType.JSON, title = "admin.temp_group_list.directory", tab = "metadata",  visible=false, filter=false, orderable=false, className = "dt-tree-dir-simple", sortAfter = "imageName",
		editor = {
			@DataTableColumnEditor(
				attr = {
					@DataTableColumnEditorAttr(key = "data-dt-field-root", value = "/images/gallery"),
					@DataTableColumnEditorAttr(key = "data-dt-field-hideRootParents", value = "true"),
                    @DataTableColumnEditorAttr(key = "disabled", value = "disabled")
				}
			)
		}
	)
    @NotBlank
    private String imagePath;

    @DataTableColumn(
        inputType = DataTableColumnType.CHECKBOX,
        title = "[[#{editor.perex.group}]]",
        tab = "metadata",
        sortAfter = "sortPriority",
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "unselectedValue", value = ""),
                    @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before"),
                }
            )
        }
    )
	private String[] perexGroupsIds;

    public void fromGalleryEntity(GalleryEntity entityOriginal) {
        // Get perex group ids as String, parse String and ids set into perexGroupsIds array
        perexGroupsIds = Tools.getTokens(entityOriginal.getPerexGroup(), ",");

        //
        imagePath = entityOriginal.getImagePath();
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
        entityOriginal.setImagePath(imagePath);
    }
}