package sk.iway.iwcm.rag.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class SemanticSearchResultTest {

    @Test
    void testConstructor() {
        SemanticSearchResult result = new SemanticSearchResult(42L, 0.85);
        assertEquals(42L, result.getDocId());
        assertEquals(0.85, result.getSimilarity());
    }

    @Test
    void testDefaultConstructor() {
        SemanticSearchResult result = new SemanticSearchResult();
        assertNull(result.getDocId());
        assertNull(result.getSimilarity());
    }

    @Test
    void testSetters() {
        SemanticSearchResult result = new SemanticSearchResult();
        result.setDocId(100L);
        result.setSimilarity(0.99);
        assertEquals(100L, result.getDocId());
        assertEquals(0.99, result.getSimilarity());
    }
}
