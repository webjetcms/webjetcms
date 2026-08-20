package sk.iway.iwcm.rag;

import sk.iway.iwcm.Tools;

/**
 * Enumeration of actions for RAG index queue entries.
 */
public enum RagIndexAction {
    INDEX,
    DELETE;

    /**
     * Parse a string value to RagIndexAction (case-insensitive).
     * @param value the string to parse
     * @return the matching RagIndexAction, or null if the value is empty or invalid
     */
    public static RagIndexAction fromString(String value) {
        if (Tools.isEmpty(value)) return null;

        try {
            return RagIndexAction.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
