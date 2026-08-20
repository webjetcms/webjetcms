package sk.iway.iwcm.rag.service;

import com.fasterxml.jackson.annotation.JsonCreator;

import sk.iway.iwcm.Tools;

/**
 * Supported entity types for RAG indexing.
 * Currently only DOCUMENT is supported.
 */
public enum RagEntityType {
    DOCUMENT;

    /**
     * Parse a string value to RagEntityType (case-insensitive).
     * @param value the string to parse
     * @return the matching RagEntityType, or null if the value is empty or invalid
     */
    @JsonCreator
    public static RagEntityType fromString(String value) {
        if (Tools.isEmpty(value)) return null;

        try {
            return RagEntityType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

