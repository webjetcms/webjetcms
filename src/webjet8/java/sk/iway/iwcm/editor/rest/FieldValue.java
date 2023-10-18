package sk.iway.iwcm.editor.rest;

import java.io.Serializable;

public class FieldValue implements Serializable {

    private static final long serialVersionUID = 1L;

    private String label;
    private Object value;
    private String type;

    public FieldValue() {

    }

    public FieldValue(String label, Object value) {
        this.label = label;
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}