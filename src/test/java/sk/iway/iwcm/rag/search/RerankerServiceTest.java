package sk.iway.iwcm.rag.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.rag.indexing.MarkdownChunker;
import sk.iway.iwcm.rag.indexing.MarkdownContentExtractor;
import sk.iway.iwcm.rag.indexing.SlidingWindowChunker;
import sk.iway.iwcm.rag.vectorstore.VectorSearchResult;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Covers trusted heading bonuses, split code examples, and retrieval similarity preservation. */
class RerankerServiceTest extends BaseWebjetTest {
    private final MarkdownContentExtractor extractor = new MarkdownContentExtractor();
    private final RerankerService reranker = new RerankerService();
    private String originalWeight;

    @BeforeEach
    void configureWeight() {
        originalWeight = Constants.getString("ragRerankLexicalWeight");
        Constants.setString("ragRerankLexicalWeight", "0.15");
    }

    @AfterEach
    void restoreWeight() {
        Constants.setString("ragRerankLexicalWeight", originalWeight);
    }

    /** Heading-like lines inside a split fenced example receive only body and phrase credit. */
    @ParameterizedTest
    @ValueSource(strings = {"# password reset", "password reset\n---"})
    void splitCodeExamplesDoNotReceiveHeadingBonus(String codeLine) {
        String markdown = "# Guide\n\n```text\n" + "print(1)\n".repeat(130)
            + codeLine + "\n" + "print(2)\n".repeat(250) + "```";
        String text = extractor.extractText(markdown);
        MarkdownChunker chunker = new MarkdownChunker(extractor, new SlidingWindowChunker());
        List<String> inputs = extractor.addHeadingContext(text, "Guide", chunker.chunkWithOffsets(text, 1000, 0));
        String fragment = inputs.stream().filter(input -> input.contains(codeLine)).findFirst().orElseThrow();
        assertFalse(fragment.contains("```"), "The regression must exercise a fragment without fence delimiters");
        VectorSearchResult result = chunk(1L, "markdown", "Guide", fragment, .8);

        reranker.rerank("password reset", List.of(result));

        assertEquals(.77, result.getRankingScore(), 1e-9);
        assertEquals(.8, result.getSimilarity());
        assertEquals(fragment, result.getChunkText());
    }

    /** Trusted source titles and stored hierarchy headings retain their bonus over body-only matches. */
    @Test
    void sourceTitlesAndHierarchyRetainHeadingBonus() {
        VectorSearchResult body = chunk(1L, "markdown", "Guide", "password reset", .8);
        VectorSearchResult title = chunk(2L, "document", "Password reset", "Instructions", .8);
        VectorSearchResult hierarchy = chunk(3L, "markdown", "Guide", "Guide > Password reset\n\nInstructions", .8);

        List<VectorSearchResult> ranked = reranker.rerank("password reset", List.of(body, title, hierarchy));

        assertEquals(List.of(title, hierarchy, body), ranked);
        assertEquals(.83, title.getRankingScore(), 1e-9);
        assertEquals(.83, hierarchy.getRankingScore(), 1e-9);
        assertEquals(.77, body.getRankingScore(), 1e-9);
    }

    /** Filtering still uses retrieval similarity when a heading match changes the result order. */
    @Test
    void rerankingPreservesSimilarityThresholdAndMinimumResults() {
        VectorSearchResult body = chunk(1L, "markdown", "Guide", "password reset", .8);
        VectorSearchResult title = chunk(2L, "markdown", "Password reset", "Instructions", .78);
        SemanticSearchService search = new SemanticSearchService(null, null, null, null, reranker);

        List<SemanticSearchResult> ranked = search.aggregateBySourceBestScore(
            reranker.rerank("password reset", List.of(body, title)));

        assertEquals(List.of(2L, 1L), ranked.stream().map(SemanticSearchResult::getDocId).toList());
        assertEquals(.78, title.getSimilarity());
        assertEquals(.8, body.getSimilarity());
        assertEquals(List.of(1L), search.filterResultsBySimilarity(ranked, .79, 0).stream()
            .map(SemanticSearchResult::getDocId).toList());
        assertEquals(ranked, search.filterResultsBySimilarity(ranked, .79, 1));
    }

    private VectorSearchResult chunk(Long id, String type, String title, String text, double similarity) {
        VectorSearchResult chunk = new VectorSearchResult(id, type, id, 0, text, similarity);
        chunk.setSourceTitle(title);
        return chunk;
    }
}
