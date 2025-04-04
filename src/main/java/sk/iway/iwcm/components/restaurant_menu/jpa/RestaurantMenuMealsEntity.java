package sk.iway.iwcm.components.restaurant_menu.jpa;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;

@Entity
@Table(name = "restaurant_menu_meals")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_RESTAURANT_MENU)
public class RestaurantMenuMealsEntity {

    @Id
    @Column(name = "meals_id")
    @GeneratedValue(generator="WJGen_restaurant_menu_meals")
	@TableGenerator(name="WJGen_restaurant_menu_meals", pkColumnValue="restaurant_menu_meals")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @Column(name = "name")
    @NotBlank
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="components.restaurant_menu.name"
    )
    @Size(max = 255)
    private String name;

    @Column(name = "cathegory")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title="components.restaurant_menu.cathegory",
        editor = {
			@DataTableColumnEditor(
				options = {
					@DataTableColumnEditorAttr(key = "components.restaurant_menu.soup", value = "1Polievka"),
					@DataTableColumnEditorAttr(key = "components.restaurant_menu.main_dish", value = "2Hlavné jedlo"),
					@DataTableColumnEditorAttr(key = "components.restaurant_menu.side_dish", value = "3Príloha"),
                    @DataTableColumnEditorAttr(key = "components.restaurant_menu.dessert", value = "4Dezert")
				}
			)
		}
    )
    @Size(max = 128)
    private String cathegory;

    @Column(name = "description")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title="components.restaurant_menu.description"
    )
    private String description;

    @Column(name = "weight")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.restaurant_menu.weight"
    )
    @Size(max = 255)
    private String weight;

    @Column(name = "price")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="components.restaurant_menu.price",
        renderFormat = "dt-format-number--decimal"
    )
    private Double price;

    @Column(name = "alergens")
    @Size(max = 32)
    private String alergens;

    @Column(name = "domain_id")
    @NotNull
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    private Integer domainId;

    @Transient
    @DataTableColumnNested
	private RestaurantMenuMealsEditorFields editorFields = null;

    @JsonIgnore
    @JsonManagedReference(value="meal")
    @OneToMany(mappedBy="meal", fetch=FetchType.LAZY, cascade={CascadeType.ALL}, orphanRemoval=true)
    List<RestaurantMenuEntity> menuEntityes;

    public String getCathegoryName() {
        if(Character.isDigit(cathegory.charAt(0)))
			return cathegory.substring(1);
		else
			return cathegory;
    }
}
