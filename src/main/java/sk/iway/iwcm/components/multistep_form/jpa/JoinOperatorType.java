package sk.iway.iwcm.components.multistep_form.jpa;

import java.util.List;

import sk.iway.iwcm.system.datatable.json.LabelValue;

public enum JoinOperatorType {
    AND,
    OR;

    public static JoinOperatorType fromString(String value) {
        if (value == null) return null;
        for (JoinOperatorType type : JoinOperatorType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown JoinOperatorType: " + value);
    }

    public static List<LabelValue> getLabelValues() {
        List<LabelValue> list = new java.util.ArrayList<>();
        for (JoinOperatorType type : JoinOperatorType.values()) {
            list.add(new LabelValue(type.name(), type.name()));
        }
        return list;
    }
}
