package sk.iway.iwcm.rag.indexing;

import sk.iway.iwcm.rag.service.RagEntityType;

/**
 * Interface for extracting plain text from an entity for embedding.
 */
public interface ContentExtractor<T> {

    /**
     * Extract text content from the given entity, suitable for embedding.
     * @param entity the source entity
     * @return extracted plain text (HTML stripped, includes rendered)
     */
    String extractText(T entity);

    /**
     * Returns the entity type identifier (e.g., "document").
     */
    RagEntityType getEntityType();
}
