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
    BOOLEAN,
    UUID,
    QUILL,
    BOOLEAN_TEXT,
    COLOR,
    JSON_GROUP,
    JSON_DOC
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
        if ("boolean_text".equals(str)) return FieldType.BOOLEAN_TEXT;
        if ("uuid".equals(str)) return FieldType.UUID;
        if ("quill".equals(str)) return FieldType.QUILL;
        if ("color".equals(str)) return FieldType.COLOR;

        if (str.startsWith("json_group")) return FieldType.JSON_GROUP;
        if (str.startsWith("json_doc")) return FieldType.JSON_DOC;

        return FieldType.TEXT;
    }
}
