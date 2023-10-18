package sk.iway.iwcm.components.translation_keys.jpa;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.editor.FieldType;
import sk.iway.iwcm.editor.rest.Field;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.ConstantsV9;
import sk.iway.iwcm.system.datatable.ProcessItemAction;

@Getter
@Setter
public class TranslationKeyEditorFields {

    //List of Free FieldsNconf
    private List<Field> fieldsDefinition;

    //String keyPrefix = "language.";
    private static final String KEY_PREFIX = "language.";

    //Represent how much custom-field's contains TranslationKeyEntity class
    private static final char LAST_ALPHABET = 'J';

    @JsonIgnore
    public void from(Map<String, String> languageFieldCombination, ProcessItemAction action) {
        Prop prop = Prop.getInstance();
        fieldsDefinition = new ArrayList<>();

        //Trere are no set language-fieldAlphabet combination's
        if(languageFieldCombination == null) return;

        //Set language value fields
        for(Map.Entry<String,String> entry : languageFieldCombination.entrySet()) {
            String lng = entry.getKey();
            String fieldAlphabet = entry.getValue();
            try {
                Field valueField = new Field(); //Represent value of translation key in specific language
                valueField.setKey(fieldAlphabet.toLowerCase());
                String label = prop.getText(KEY_PREFIX + lng);
                valueField.setLabel(label);
                valueField.setType(FieldType.TEXTAREA.name().toLowerCase());
                valueField.setMaxlength(65000);

                fieldsDefinition.add(valueField);
            } catch (Exception ex) {
                Logger.error(TranslationKeyEditorFields.class, ex);
            }
        }

        //Set language original value fields
        String originalKeyValue = prop.getText("translation_key.original_value");
        for(Map.Entry<String,String> entry : languageFieldCombination.entrySet()) {
            String lng = entry.getKey();
            String fieldAlphabet = entry.getValue();
            try {
                Field originalValueField = new Field(); //Represent ORIGINAL value of translation key in specific language (load from file replaced by value in DB)
                originalValueField.setKey(fieldAlphabet.toLowerCase());
                String label = prop.getText(KEY_PREFIX + lng) + ", " + originalKeyValue;
                originalValueField.setLabel(label);
                originalValueField.setType(FieldType.LABEL.name().toLowerCase());
                originalValueField.setDisabled(true);
                originalValueField.setCustomPrefix("originalValue");
                fieldsDefinition.add(originalValueField);
            } catch (Exception ex) {
                Logger.error(TranslationKeyEditorFields.class, ex);
            }
        }

        //Hide redundant fields
        String[] lngArr = ConstantsV9.getArray("languages");
        char lastUsedAlphabet = (char)(((int)'A') + lngArr.length);
        for(char alphabet = lastUsedAlphabet; alphabet <= LAST_ALPHABET; alphabet++) {
            try {
                Field valueField = new Field();
                valueField.setKey((alphabet + "").toLowerCase());
                valueField.setType(FieldType.NONE.name().toLowerCase());
                fieldsDefinition.add(valueField);

                Field originalValueField = new Field();
                originalValueField.setKey((alphabet + "").toLowerCase());
                originalValueField.setType(FieldType.NONE.name().toLowerCase());
                originalValueField.setCustomPrefix("originalValue");
                fieldsDefinition.add(originalValueField);
            } catch (Exception e) {
                Logger.error(TranslationKeyEditorFields.class, e);
            }
        }
    }
}
