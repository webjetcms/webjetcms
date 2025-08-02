package sk.iway.iwcm.system.datatable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.enumerations.EnumerationDataDB;
import sk.iway.iwcm.components.enumerations.model.EnumerationDataBean;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.editor.FieldType;
import sk.iway.iwcm.editor.rest.Field;
import sk.iway.iwcm.editor.rest.FieldValue;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.IdFullPath;

/**
 * Zakladna trieda pre EditorFields Datatabulky, obsahuje spolocne vlastnosti, primarne nastavenie CSS riadku a ikony
 */
@Getter
@Setter
public class BaseEditorFields {

    private String rowClass;
    //zoznam stavovych ikon, je to jsonignore, musi sa pridat specialne v EditorFields objekte ako @DatatableColumn(...) String statusIcons;
    @JsonIgnore
    private List<String> statusIconsList;
    //zoznam volnych poli
    private List<Field> fieldsDefinition;
    //properties key prefix for fields definition, needs to be set for autocomplete field
    private String fieldsDefinitionKeyPrefix;

    //poslanie notifikacie, je potrebne pri getOne alebo pri ulozeni
    private List<NotifyBean> notify;

    /**
     * Prida CSS triedu k ROW tagu (implementovane v index.js v optione rowCallback: DT)
     * @param addClass
     */
    public void addRowClass(String addClass) {
        if (Tools.isEmpty(rowClass)) rowClass = addClass;
        else rowClass += " "+addClass;
    }

    /**
     * Prida novu ikonu k textu (vytvorenu ako span element)
     * @param className - ti ti-eye-off
     */
    public void addStatusIcon(String className) {
        if (statusIconsList == null) statusIconsList = new ArrayList<>();
        if(statusIconsList.contains(className) == false) statusIconsList.add(className);
    }

    @JsonIgnore
    /**
     * Vygeneruje definiciu volnych poli, presunute sem z EditorForm.getFields() pre moznost pouzitia aj v inych DT ako webpages
     * @param bean - java bean, musi obsahovat metody getFieldX
     * @param keyPrefix - prefix textovych klucov, napr. edior, alebo groupedit, nasledne sa hladaju kluce keyPrefix.field_X a keyPrefix.field_X.type
     * @param lastAlphabet - koncove pismeno (urcuje pocet volnych poli), nap. T aleb D
     * @return
     */
    public List<Field> getFields(Object bean, String keyPrefix, char lastAlphabet) {
		//tu musi byt getInstance aby sa prebral jazyk podla prihlaseneho usera
        Prop prop = Prop.getInstance();
		Prop propType = Prop.getInstance(Constants.getString("defaultLanguage"));
        List<Field> fields = new ArrayList<>();
        fieldsDefinitionKeyPrefix = keyPrefix;
        Method method;
        for (char alphabet = 'A'; alphabet <= lastAlphabet; alphabet++) {

            try {
                Field field = new Field();
                method = bean.getClass().getMethod("getField" + alphabet);

                String labelKey = keyPrefix+".field_" + Character.toLowerCase(alphabet);
                String label = prop.getText(labelKey);

                String typeKey = labelKey + ".type";
                String type = propType.getText(typeKey);

                FieldType fieldType = FieldType.asFieldType(type);
                List<FieldValue> fieldValues = new ArrayList<>();

                if (!type.equals(typeKey)) {
                    if (type.contains("|")) {
                        // multiple select
                        if(type.startsWith("multiple:")) {
                            type = type.replace("multiple:", "");
                            field.setMultiple(true);
                        }
                        String values = type.substring(type.indexOf(":") + 1);
                        //ak zacina na znak | chceme mat moznost prvu hodnotu mat prazdnu
                        if (values.startsWith("|")) fieldValues.add(new FieldValue("", ""));
                        for (String value : Tools.getTokens(values, "|")) {
                            fieldValues.add(new FieldValue(value, value));
                        }
                    }

                    if (type.startsWith("docsIn_")) {
                        //JICH - add
                        boolean isNull = false;
                        if (type.endsWith("_null")) {
                            isNull = true;
                            type = type.replace("_null", "");
                        }
                        //JICH - add end
                        String groupId = type.substring(type.indexOf("_") + 1);
                        int groupIdInt = Tools.getIntValue(groupId, 0);
                        if (groupIdInt > 0) {
                            GroupDetails group = GroupsDB.getInstance().getGroup(groupIdInt);
                            List<DocDetails> listOfDocs = DocDB.getInstance().getDocByGroup(groupIdInt);
                            if (listOfDocs != null) {
                                //JICH - add
                                if (isNull) {
                                    fieldValues.add(new FieldValue("", ""));
                                }
                                //JICH - add end
                                for (DocDetails d : listOfDocs) {
                                    if (group != null && group.getDefaultDocId() != d.getDocId()) {
                                        fieldValues.add(new FieldValue(d.getTitle(), d.getDocId()));
                                    }
                                }
                            }
                        }
                    }

                    if (type.startsWith("enumeration_")) {
                        boolean isNull = false;
                        if (type.endsWith("_null")) {
                            isNull = true;
                            type = type.replace("_null", "");
                        }

                        int enumerationId = Tools.getIntValue(type.substring(type.indexOf("_") + 1), 0);
                        if (enumerationId > 0) {
                            List<EnumerationDataBean> enumerationDataList = EnumerationDataDB.getEnumerationDataByType(enumerationId);
                            if (enumerationDataList != null) {
                                if (isNull) {
                                    fieldValues.add(new FieldValue("", ""));
                                }
                                for (EnumerationDataBean enumData : enumerationDataList) {
                                    fieldValues.add(new FieldValue(enumData.getString1(), enumData.getString1()));
                                }
                            }
                        }
                    }

                    // Support nullable of field by setting right className
                    if(type.startsWith("json_group_")) {
                        if (type.endsWith("_null")) {
                            type = type.replace("_null", "");
                            field.setClassName("dt-tree-groupid-null");
                        }
                    }

                    // Support nullable of field by setting right className
                    if(type.startsWith("json_doc_")) {
                        if (type.endsWith("_null")) {
                            type = type.replace("_null", "");
                            field.setClassName("dt-tree-pageid-null");
                        }
                    }

                    //JICH - add
                    if (type.startsWith("custom-dialog")) {
                        //System.out.println(type);
                        String[] typeArray = type.split(",");
                        String dialogScript = "";
                        String displayScript = "";
                        if (typeArray.length > 1) dialogScript = typeArray[1];
                        if (typeArray.length > 2) displayScript = typeArray[2];

                        fieldValues.add(new FieldValue(dialogScript, displayScript));
                    }
                    //JICH - add end
                }

                if (fieldType == null) {
                    fieldType = FieldType.TEXT;
                }

                String value;
                Object returnValue = method.invoke(bean);
                if(returnValue != null) {
                    value = returnValue.toString();
                } else {
                    value = "";
                }

                //	TAN: textovym retazcom je mozne zadat maximalnu dlzku znakov v inpute alebo validaciu s odporucanym maximalnym poctom znakov inputu
                // priklad zadania textoveho kluca: text-120, warningLength-80
                int maxlength = 255;
                int warninglength = 0;
                try
                {
                    if (type.startsWith("text-")) {

                        if (type.contains(",")) {
                            String[] typeArray = type.split(",");
                            String maxlengthstring = typeArray[0];
                            String warninglengthstring = typeArray[1];

                            maxlengthstring = maxlengthstring.substring(maxlengthstring.lastIndexOf("-") + 1);
                            warninglengthstring = warninglengthstring.substring(warninglengthstring.lastIndexOf("-") + 1);

                            maxlength = Integer.parseInt(maxlengthstring.replaceAll("[^0-9]", ""));
                            warninglength = Integer.parseInt(warninglengthstring.replaceAll("[^0-9]", ""));
                        } else {
                            int pomlcka = type.indexOf("-");
                            if (pomlcka > 0) maxlength = Tools.getIntValue(type.substring(pomlcka + 1), 255);
                        }
                    }
                }
                catch (Exception ex)
                {
                    Logger.error(BaseEditorFields.class, ex);
                }

                field.setKey(Character.toLowerCase(alphabet) + "");
                field.setLabel(label);

                if("json_group".equals(type)) {
                    int groupId = Tools.getIntValue(value, -1);
                    if(groupId > 0) {
                        GroupDetails group = GroupsDB.getInstance().getGroup(groupId);
                        if(group != null) {
                            field.setValue( new IdFullPath(groupId, group.getFullPath()).toString() );
                        }
                    }
                } else if("json_doc".equals(type)) {
                    int docId = Tools.getIntValue(value, -1);
                    if(docId > 0) {
                        DocDetails doc = DocDB.getInstance().getDoc(docId);
                        if(doc != null) {
                            field.setValue( new IdFullPath(docId, doc.getFullPath()).toString() );
                        }
                    }
                } else {
                    field.setValue(value);
                }

                if (Tools.isEmpty(field.getType())) field.setType(fieldType.name().toLowerCase());
                field.setMaxlength(maxlength);
                field.setWarninglength(warninglength);
                if (warninglength>0) {
                    field.setWarningMessage( prop.getText(keyPrefix+".field_" + Character.toUpperCase(alphabet)+".warningText", String.valueOf(warninglength)));
                }
                if (fieldType != FieldType.TEXT) {
                    field.setTypeValues(fieldValues);
                }

                fields.add(field);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                Logger.error(BaseEditorFields.class, e);
            }
        }

		return fields;
	}

    /**
     * Vrati HTML kod stavovych ikon, vo vasej triede musite implementovat minimalny kod:
     * getStatusIcons() { return getStatusIconsHtml(); }
     * @return
     */
    @JsonIgnore
    public String getStatusIconsHtml() {
        StringBuilder iconsHtml = new StringBuilder();
        //console.log("currentRow=", currentRow);
        if (statusIconsList!=null && statusIconsList.isEmpty()==false) {
            for (String icon : statusIconsList) {
                iconsHtml.append("<i class=\"").append(icon).append("\"></i> ");
            }
        }
        return iconsHtml.toString();
    }
}
