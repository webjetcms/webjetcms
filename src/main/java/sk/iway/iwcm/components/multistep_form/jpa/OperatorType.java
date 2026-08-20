package sk.iway.iwcm.components.multistep_form.jpa;

import java.util.ArrayList;
import java.util.List;

import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.LabelValue;

public enum OperatorType {
    EQUALS("equals"),
    NOT_EQUALS("not_equals"),
    CONTAINS("contains"),
    NOT_CONTAINS("not_contains"),
    STARTS_WITH("starts_with"),
    ENDS_WITH("ends_with"),
    EMPTY("empty"),
    NOT_EMPTY("not_empty");

    private final String value;
    private static final String KEY_PREFIX = "components.form_items_condition.";

    OperatorType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public boolean needsValue() {
        return this != EMPTY && this != NOT_EMPTY;
    }

    public static OperatorType fromString(String value) {
        if (value == null) return EQUALS;
        for (OperatorType type : OperatorType.values()) {
            if (type.value.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return EQUALS;
    }

    public static List<LabelValue> getLabelValues(Prop prop) {
        List<LabelValue> list = new ArrayList<>();
        for (OperatorType type : OperatorType.values()) {
            list.add(new LabelValue(prop.getText(KEY_PREFIX + type.getValue()), type.name()));
        }
        return list;
    }
}
