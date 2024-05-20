package sk.iway.iwcm.components.restaurant_menu.jpa;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class RestaurantMenuEditorFields extends BaseEditorFields {

    public RestaurantMenuEditorFields() {
        //konstruktor
    }

    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="components.cron_task.day_in_week",
        sortAfter = "id",
        hiddenEditor = true,
        orderable = false,
        filter = false
    )
    private String dayOfWeek;

    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title="components.restaurant_menu.cathegory",
        sortAfter = "editorFields.dayOfWeek",
        orderable = false,
        filter = false,
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
    private String mealCathegory;

    //In table just showing meal name
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.restaurant_menu.meal_name",
        sortAfter = "editorFields.mealCathegory",
        renderFormatLinkTemplate = "javascript:openMealDetail({{mealId}});",
        hiddenEditor = true,
        orderable = false,
        filter = false
    )
    private String mealName;

    @DataTableColumn(
        inputType = DataTableColumnType.MULTISELECT,
        title = "components.restaurant_menu.alergens",
        renderFormat = "dt-format-select",
        hiddenEditor = true,
        orderable = false,
        filter = false
    )
    private Integer[] mealAlergens;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.restaurant_menu.description",
        hiddenEditor = true,
        orderable = false,
        filter = false,
        visible = false
    )
    private String mealDescription;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.restaurant_menu.weight",
        hiddenEditor = true,
        orderable = false,
        filter = false,
        visible = false
    )
    private String mealWeight;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.restaurant_menu.price",
        renderFormat = "dt-format-number--decimal",
        hiddenEditor = true,
        orderable = false,
        filter = false,
        visible = false
    )
    private Double mealPrice;

    //In editor it's meal select - values are add throu ajax request in FE
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title="components.restaurant_menu.meal_name",
        sortAfter = "editorFields.mealCathegory",
        hidden = true
    )
    private Integer selectedMealId;

    public void fromRestaurantMenuEntity(RestaurantMenuEntity originalEntity, Prop prop) {
        RestaurantMenuMealsEntity meal = originalEntity.getMeal();

        if(meal == null) return;
        this.mealCathegory = meal.getCathegory();
        this.mealName = meal.getName();
        this.mealDescription = meal.getDescription();
        this.mealWeight = meal.getWeight();
        this.mealPrice = meal.getPrice();
        this.mealAlergens = Arrays.stream( Tools.getTokensInt(meal.getAlergens(), ",") ).boxed().toArray( Integer[]::new );

        //set day of week
        Calendar cal = Calendar.getInstance();
        cal.setTime(originalEntity.getDayDate());
        this.dayOfWeek = prop.getText("dayfull." + cal.get(Calendar.DAY_OF_WEEK));

        originalEntity.setMealId(meal.getId());
        originalEntity.setEditorFields(this);
    }

    public void toRestaurantMenuEntity(RestaurantMenuEntity originalEntity, RestaurantMenuMealsRepository rmmr) {
        if(originalEntity.getEditorFields() == null) return;

        Optional<RestaurantMenuMealsEntity> selectedMeal = rmmr.findFirstByIdAndDomainId(selectedMealId.longValue(), CloudToolsForCore.getDomainId());
        if(selectedMeal.isPresent()) originalEntity.setMeal( selectedMeal.get() );
        else throw new RuntimeException("Something went wrong - uknow meal");
    }
}