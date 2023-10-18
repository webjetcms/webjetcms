package sk.iway.iwcm.components.enumerations.model;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter
public class EnumerationTypeEditorFields implements Serializable {

    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title="components.enumerations.child_enumeration_type_name",
        sortAfter = "typeName",
        filter = false,
        tab="basic"
    )
    private Integer childEnumTypeId;

    public void fromEnumerationType(EnumerationTypeBean enumerationTypeOriginal) {
        EnumerationTypeBean bean = enumerationTypeOriginal.getChildEnumerationTypeBean();

        //Set id of child enumeration type bean
        if(bean != null) this.childEnumTypeId = bean.getEnumerationTypeId();

        enumerationTypeOriginal.setEditorFields(this);
    }

    public void toEnumerationType(EnumerationTypeBean enumerationTypeOriginal, EnumerationTypeRepository etr, Prop prop) {
        //Both child and parent options CANNOT be true
        if(enumerationTypeOriginal.isAllowChildEnumerationType() && enumerationTypeOriginal.isAllowParentEnumerationData())
            throw new IllegalArgumentException(prop.getText("enum_type.allow_error"));

        //Check loop dependecie (A can have B as child, but B cant have A as child same time)
        //If leep dependecie is ok, then set enumeration child bean (if null or -1 its empty select)
        Integer childId = enumerationTypeOriginal.getEditorFields().getChildEnumTypeId() == null ? -1 : enumerationTypeOriginal.getEditorFields().getChildEnumTypeId();
        Integer oldChildId = etr.getChildEnumTypeId(enumerationTypeOriginal.getEnumerationTypeId());
        oldChildId = oldChildId == null ? -1 : oldChildId;

        //Check only if childId is changed
        //!! Very important in case child is same but allready deleted
        if(oldChildId.equals(childId)==false) {
            EnumerationTypeBean childEnumType = etr.getByEnumId(childId);
            if(childEnumType != null) {
                EnumerationTypeBean childOfTheChild = childEnumType.getChildEnumerationTypeBean();
                if(childOfTheChild != null && (childOfTheChild.getId().equals(enumerationTypeOriginal.getId()))) {
                    String errorText = prop.getText("enum_type.loop_child_reference_1") + " " + childEnumType.getTypeName() + " " + prop.getText("enum_type.loop_child_reference_2");
                    throw new IllegalArgumentException(errorText);
                } else if(childEnumType.isHidden()) {
                    //Check that selected type is not soft deleted (hidden)
                    throw new IllegalArgumentException(prop.getText("enum_type.allready_deleted_error"));
                } else enumerationTypeOriginal.setChildEnumerationTypeBean(childEnumType);
            }
        }

        if(childId != -1)
            enumerationTypeOriginal.setChildEnumerationTypeBean(etr.getByEnumId(childId));
        else enumerationTypeOriginal.setChildEnumerationTypeBean(null);
    }
}
