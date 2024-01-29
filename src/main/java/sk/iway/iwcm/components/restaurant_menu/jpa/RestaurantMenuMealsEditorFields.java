package sk.iway.iwcm.components.restaurant_menu.jpa;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class RestaurantMenuMealsEditorFields {
    
    public RestaurantMenuMealsEditorFields(){
        //konstruktor
    }

    @DataTableColumn(
        inputType = DataTableColumnType.MULTISELECT, 
        title = "components.restaurant_menu.alergens",
        renderFormat = "dt-format-select",
        orderable = false,
        editor = {
            @DataTableColumnEditor(attr = {
                @DataTableColumnEditorAttr(key = "unselectedValue", value = "") 
            }) 
        }
    )
    private Integer[] alergensArr;

    public void fromRestaurantMenuMealsEntity(RestaurantMenuMealsEntity originalEntity) {
        alergensArr = Arrays.stream( Tools.getTokensInt(originalEntity.getAlergens(), ",") ).boxed().toArray( Integer[]::new );

        originalEntity.setEditorFields(this);
    }

    public void toRestaurantMenuMealsEntity(RestaurantMenuMealsEntity originalEntity) {
        if(originalEntity.getEditorFields() == null) return;

        String alergensStr = "";
        if(!(alergensArr.length == 1 && alergensArr[0] == null)) { //Hadnle situation when arr has only 1 elemnt NULL (nothing is selected)
            for(int alergenNumber : alergensArr) {
                if(Tools.isAnyEmpty(alergensStr)) alergensStr = alergenNumber + "";
                else {
                    alergensStr += "," + alergenNumber;
                }
            }
        }
        originalEntity.setAlergens(alergensStr);
    }
}
