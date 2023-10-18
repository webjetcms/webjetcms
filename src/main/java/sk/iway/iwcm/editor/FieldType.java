package sk.iway.iwcm.editor;

import sk.iway.iwcm.Tools;

public enum FieldType {
    TEXT,
    SELECT,
    IMAGE,
    LINK,
    AUTOCOMPLETE,
    //JICH - add
    HIDDEN,
    //JICH - add end
    //LPA
    NONE,
    DIR,
    TEXTAREA,
    //just text label
    LABEL,
    DATE,
    NUMBER,
    BOOLEAN
    ;

    public static FieldType asFieldType(String str) {

        if (Tools.isEmpty(str)) {
            return null;
        }

        if (str.contains(":")) {
            str = str.substring(0, str.indexOf(":"));
        }

        if (str.startsWith("docsIn") || str.contains("|") || str.startsWith("enumeration") || str.startsWith("multiple")) {
            return FieldType.SELECT;
        }

        //JICH - add
        if (str.startsWith("custom-dialog")) {
            return FieldType.HIDDEN;
        }
        //JICH - add end

        for (FieldType me : FieldType.values()) {
            if (me.name().equalsIgnoreCase(str))
                return me;
        }

        if ("none".equals(str)) return FieldType.NONE;
        if ("textarea".equals(str)) return FieldType.TEXTAREA;
        if ("label".equals(str)) return FieldType.LABEL;
        if ("date".equals(str)) return FieldType.DATE;
        if ("number".equals(str)) return FieldType.NUMBER;
        if ("boolean".equals(str)) return FieldType.BOOLEAN;

        return FieldType.TEXT;
    }
}
