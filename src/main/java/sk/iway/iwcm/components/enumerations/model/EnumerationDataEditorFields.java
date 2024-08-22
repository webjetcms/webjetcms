package sk.iway.iwcm.components.enumerations.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.editor.FieldType;
import sk.iway.iwcm.editor.rest.Field;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@Getter
@Setter
public class EnumerationDataEditorFields {

    private List<Field> fieldsDefinition;

    private static final Integer STRING_FIELDS_COUNT = 12;
    private static final String STRING_PREFIX = "string";

    private static final Integer DECIMAL_FIELDS_COUNT = 4;
    private static final String DECIMAL_PREFIX = "decimal";

    private static final Integer BOOLEAN_FIELDS_COUNT = 4;
    private static final String BOOLEAN_PREFIX = "boolean";

    private static final Integer DATE_FIELDS_COUNT = 4;
    private static final String DATE_PREFIX = "date";

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.enumerations.child_enumeration_type_name",
        hidden = true,
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/rest/enumeration/enumeration-data/autocomplete-child"),
                    @DataTableColumnEditorAttr(key = "data-ac-min-length", value = "1"),
                    @DataTableColumnEditorAttr(key = "data-ac-select", value = "true"),
                    @DataTableColumnEditorAttr(key = "data-ac-render-item-fn", value = "disableDeletedEnum")
                }
            )
        }
    )
    private String childEnumTypeName;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.enumerations.parent_enumeration_data_name",
        hidden = true,
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/rest/enumeration/enumeration-data/autocomplete-parent"),
                    @DataTableColumnEditorAttr(key = "data-ac-min-length", value = "1"),
                    @DataTableColumnEditorAttr(key = "data-ac-params", value = "#DTE_Field_typeId, #DTE_Field_string1"),
                    @DataTableColumnEditorAttr(key = "data-ac-select", value = "true"),
                    @DataTableColumnEditorAttr(key = "data-ac-render-item-fn", value = "disableDeletedEnum")
                }
            )
        }
    )
    private String parentEnumDataName;

    public void fromEnumerationData(EnumerationDataBean entity, EnumerationTypeBean typeEntity, boolean addFields, Prop prop) {
        fieldsDefinition = new ArrayList<>();

        String prefixForHidden = prop.getText("enum_type.deleted_type_mark.js");
        if(entity.getChildEnumerationType() != null) {
            childEnumTypeName = entity.getChildEnumerationType().getTypeName();
            if(entity.getChildEnumerationType().isHidden()) childEnumTypeName = prefixForHidden + childEnumTypeName;
        }

        if(entity.getParentEnumerationData() != null) {
            parentEnumDataName = entity.getParentEnumerationData().getString1();
            if(entity.getParentEnumerationData().isHidden()) parentEnumDataName = prefixForHidden + parentEnumDataName;
        }

        if(addFields == true) {
            prepareAndAddFields(STRING_PREFIX, STRING_FIELDS_COUNT, entity, typeEntity);
            prepareAndAddFields(DECIMAL_PREFIX, DECIMAL_FIELDS_COUNT, entity, typeEntity);
            prepareAndAddFields(BOOLEAN_PREFIX, BOOLEAN_FIELDS_COUNT, entity, typeEntity);
            prepareAndAddFields(DATE_PREFIX, DATE_FIELDS_COUNT, entity, typeEntity);
        }

        entity.setEditorFields(this);
    }

    public void toEnumerationData(EnumerationDataBean entity, EnumerationTypeBean dataTypeEntity, EnumerationTypeRepository etr, EnumerationDataRepository edr, Prop prop) {
        //If creating new enumeration data entity, set EnumerationTypeId
        if(entity.getType() == null) entity.setType(dataTypeEntity);

        /*Handle child enumeration type id*/
        Integer newChildEnumTypeId = Tools.isEmpty(entity.getEditorFields().getChildEnumTypeName()) == true ? -1 : etr.getIdByTypeName(entity.getEditorFields().getChildEnumTypeName());

        //FIX - entity.getChildEnumerationType will allways come like NULL from editor
        Integer oldChildEnumTypeId = edr.getChildEnumTypeIdByEnumDataId(entity.getEnumerationDataId());
        oldChildEnumTypeId = oldChildEnumTypeId == null ? -1 : oldChildEnumTypeId;

        //If not equal, aka childEnumType has changed
        if(!newChildEnumTypeId.equals(oldChildEnumTypeId)) {
            //BE permission check to set child enum type
            if(dataTypeEntity.isAllowChildEnumerationType()==false)
                throw new IllegalArgumentException(prop.getText("enum_data.set_childEnumTyp_notAllowed"));

            //Check that selected type is not deleted (soft deleted, hidden)
            if(newChildEnumTypeId != -1) {
                boolean deleted = DatatableRestControllerV2.jpaToBoolean(etr.getHiddenByEnumTypeId(newChildEnumTypeId));
                if(deleted)
                    throw new IllegalArgumentException(prop.getText("enum_type.allready_deleted_error"));
            }

            if(dataTypeEntity.getEnumerationTypeId() != null && dataTypeEntity.getEnumerationTypeId().equals(newChildEnumTypeId)) {
                //Loop error, child type cant be same as actual type
                throw new IllegalArgumentException(prop.getText("enum_type.on_himself_reference_error"));
            }
        }

        if(newChildEnumTypeId == -1) {
            //Nothing is selected
            entity.setChildEnumerationType(null);
        } else {
            //Select childEnumerationType
            entity.setChildEnumerationType(etr.getByEnumId(newChildEnumTypeId));
        }

        /*Handle parent enumeration data id*/
        Integer newParentEnumDataId = Tools.isEmpty(entity.getEditorFields().getParentEnumDataName()) == true ? -1 : edr.getIdByString1AndTypeId(entity.getEditorFields().getParentEnumDataName(), dataTypeEntity.getEnumerationTypeId());

        //FIX - entity.getParentEnumerationData will always come like NULL from editor
        Integer oldParentEnumDataId = edr.getParentEnumDataIdByEnumDataId(entity.getEnumerationDataId());
        oldParentEnumDataId = oldParentEnumDataId == null ? -1 : oldParentEnumDataId;

        //If not equal, aka childEnumType has changed
        if(!newParentEnumDataId.equals(oldParentEnumDataId)) {
            //BE permission check to set parent enum data
            if(dataTypeEntity.isAllowParentEnumerationData()==false)
                throw new IllegalArgumentException(prop.getText("enum_data.set_parentEnumData_notAllowed"));

            //Check if we want to set deleted option
            if(newParentEnumDataId != -1) {
                boolean deleted = DatatableRestControllerV2.jpaToBoolean(edr.getHiddenByEnumerationDataId(newParentEnumDataId));
                if(deleted)
                    throw new IllegalArgumentException(prop.getText("enum_data.allready_deleted_error"));
            }
        }

        if(newParentEnumDataId == -1) {
            //Nothing is selected
            entity.setParentEnumerationData(null);
        } else {
            //Select parentEnumerationData
            entity.setParentEnumerationData(edr.getEnumId(newParentEnumDataId));
        }
    }

    private void prepareAndAddFields(String prefix, Integer count, EnumerationDataBean dataEntity, EnumerationTypeBean typeEntity) {
        for(Integer i = 1; i <= count; i++) {
            try {
                BeanWrapper bwData = new BeanWrapperImpl(dataEntity);
                BeanWrapper bwType = new BeanWrapperImpl(typeEntity);
                String fieldValue = null;
                String label = (String)bwType.getPropertyValue(prefix + i + "Name");

                Field newField = new Field();
                newField.setKey(i + "");
                newField.setCustomPrefix(prefix);

                if(label == null || label.isEmpty()) {
                    newField.setType(FieldType.NONE.name().toLowerCase());
                } else {
                    if(prefix.equals(STRING_PREFIX)) {
                        fieldValue = (String)bwData.getPropertyValue(prefix + i);
                        newField.setType(FieldType.TEXT.name().toLowerCase());
                        newField.setLabel(label);
                        newField.setMaxlength(1024);
                    }
                    else if(prefix.equals(DECIMAL_PREFIX)) {
                        BigDecimal tmpDecimal = (BigDecimal)bwData.getPropertyValue(prefix + i);
                        fieldValue = tmpDecimal != null ? tmpDecimal.toString() : null;
                        newField.setType(FieldType.NUMBER.name().toLowerCase());
                        newField.setLabel(label);
                    }
                    else if(prefix.equals(BOOLEAN_PREFIX)) {
                        fieldValue = String.valueOf((boolean)bwData.getPropertyValue(prefix + i));
                        newField.setType(FieldType.BOOLEAN.name().toLowerCase());
                        newField.setLabel(label);
                    }
                    else if(prefix.equals(DATE_PREFIX)) {
                        Date tmpDate = (Date)bwData.getPropertyValue(prefix + i);
                        fieldValue = tmpDate != null ? tmpDate.toString() : null;
                        newField.setType(FieldType.DATE.name().toLowerCase());
                        newField.setLabel(label);
                    }
                    newField.setValue(fieldValue);
                }
                fieldsDefinition.add(newField);
            } catch (Exception ex) {
                Logger.error(EnumerationDataEditorFields.class, ex);
            }
        }
    }
}
