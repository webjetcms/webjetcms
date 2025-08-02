package sk.iway.iwcm.editor.rest;

import java.io.Serializable;
import java.util.List;

public class Field implements Serializable {
    private static final long serialVersionUID = 1L;

    private String key;
    private String label;
    private String value;
    private String type;
    private int maxlength;
    private int warninglength = -1;
    private String warningMessage;
    private List<FieldValue> typeValues;
    private boolean multiple;
    private String className;

    //If you dont want fieldA but originalValueA, them set customPrefix = "originalValue"
    private String customPrefix;
    //
    private boolean disabled;

    //potrebujeme ich kvoli WJ8
    public String getKey() {
        return key;
    }
    public void setKey(String key) {
        this.key = key;
    }
    public String getLabel() {
        return label;
    }
    public void setLabel(String label) {
        this.label = label;
    }
    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public int getMaxlength() {
        return maxlength;
    }
    public void setMaxlength(int maxlength) {
        this.maxlength = maxlength;
    }
    public int getWarninglength() {
        return warninglength;
    }
    public void setWarninglength(int warninglength) {
        this.warninglength = warninglength;
    }
    public String getWarningMessage() {
        return warningMessage;
    }
    public void setWarningMessage(String warningMessage) {
        this.warningMessage = warningMessage;
    }
    public List<FieldValue> getTypeValues() {
        return typeValues;
    }
    public void setTypeValues(List<FieldValue> typeValues) {
        this.typeValues = typeValues;
    }
    public boolean isMultiple() {
        return multiple;
    }
    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }
    public String getCustomPrefix() {
        return customPrefix;
    }
    public void setCustomPrefix(String customPrefix) {
        this.customPrefix = customPrefix;
    }
    public boolean isDisabled() {
        return disabled;
    }
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
    public String getClassName() {
        return className;
    }
    public void setClassName(String className) {
        this.className = className;
    }
}
