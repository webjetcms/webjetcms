package sk.iway.iwcm.components.multistep_form.jpa;

public enum ConditionType {
    VISIBILITY,
    REQUIREMENT;

    public static ConditionType fromString(String value) {
        if (value == null) return null;
        for (ConditionType type : ConditionType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }
}
