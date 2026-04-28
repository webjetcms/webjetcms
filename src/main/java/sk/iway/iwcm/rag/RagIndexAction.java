package sk.iway.iwcm.rag;

import sk.iway.iwcm.Tools;

/**
 * Enumeration of actions for RAG index queue entries.
 */
public enum RagIndexAction {
    INDEX,
    DELETE;

    public static RagIndexAction fromString(String value) {
        if (Tools.isEmpty(value)) return null;

        try {
            return RagIndexAction.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
