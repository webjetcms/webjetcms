package sk.iway.iwcm.rag.service;

import sk.iway.iwcm.Tools;

public enum RagEntityType {
    DOCUMENT;

    public static RagEntityType fromString(String value) {
        if (Tools.isEmpty(value)) return null;

        try {
            return RagEntityType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

