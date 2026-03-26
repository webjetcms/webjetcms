package sk.iway.iwcm.components.file_archiv;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@Entity
@Getter
@Setter
public class BasicManagerBean {

    public BasicManagerBean() {}

    public BasicManagerBean(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public enum ManagerType {
        CATEGORY, PRODUCT, NONE;

        public static ManagerType fromString(String type) {
            if(Tools.isEmpty(type)) return NONE;

            type = type.toLowerCase();
            if("category".equals(type)) {
                return CATEGORY;
            } else if("product".equals(type)) {
                return PRODUCT;
            }

            return NONE;
        }
    }

    @Id
	@DataTableColumn(inputType = DataTableColumnType.ID)
	private Long id;

	@DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="",
        editor = {
            @DataTableColumnEditor(
                attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }
            )
        }
    )
    @NotBlank
	private String name;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="",
        hidden = true,
        className = "hide-on-create"
    )
    @NotBlank
	private String newName;

    public static List<BasicManagerBean> getEntitiesFromStrings(List<String> values) {
        Long id = 1L;
        List<BasicManagerBean> entities = new ArrayList<>();

        for (String value : values) {
            if(Tools.isNotEmpty(value))
                entities.add(new BasicManagerBean(id++, value));
        }

        return entities;
    }
}