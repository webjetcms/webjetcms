package sk.iway.iwcm.rag.search;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.filebrowser.EditForm;
import sk.iway.iwcm.rag.service.MarkdownSourceService;
import sk.iway.iwcm.rag.service.RagEntityType;
import sk.iway.iwcm.rag.vectorjpa.EmbeddingChunkEntity;
import sk.iway.iwcm.rag.vectorjpa.EmbeddingChunkRepository;
import sk.iway.iwcm.rag.vectorjpa.EmbeddingChunkStatus;
import sk.iway.iwcm.rag.vectorstore.VectorSearchResult;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Covers the access gate and the authorized Markdown result/answer path. */
class MarkdownSearchServiceTest extends BaseWebjetTest {
    private final SemanticSearchService semanticSearch = spy(new SemanticSearchService(null, null, null, null));
    private final EmbeddingChunkRepository repository = mock(EmbeddingChunkRepository.class);
    private final RagService ragService = mock(RagService.class);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MarkdownSearchService service = new MarkdownSearchService(new MarkdownSourceService(), semanticSearch, repository, ragService);
    private final String originalFolders = Constants.getString("ragMarkdownFolders");
    private final boolean originalAnswerAllowed = Constants.getBoolean("ragAnswerAllowed");
    private final boolean originalRequireLogin = Constants.getBoolean("ragMarkdownSearchRequireLogin");
    private final String originalBlockedPaths = Constants.getString("pathFilterBlockedPaths");

    @BeforeEach
    void configureDocumentation() {
        Constants.setString("ragMarkdownFolders", "/docs/webjetcms");
        Constants.setBoolean("ragAnswerAllowed", true);
        Constants.setBoolean("ragMarkdownSearchRequireLogin", false);
    }

    @AfterEach
    void restoreConfiguration() {
        Constants.setString("ragMarkdownFolders", originalFolders);
        Constants.setBoolean("ragAnswerAllowed", originalAnswerAllowed);
        Constants.setBoolean("ragMarkdownSearchRequireLogin", originalRequireLogin);
        Constants.setString("pathFilterBlockedPaths", originalBlockedPaths);
    }

    /**
     * Verifies that authorized sources retain citations and receive an answer only when ragAnswerAllowed is enabled.
     */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void returnsAuthorizedSourceAndAnswerAccordingToConfiguration(boolean answerAllowed) {
        Constants.setBoolean("ragAnswerAllowed", answerAllowed);
        VectorSearchResult hit = new VectorSearchResult(1L, "markdown", 1L, 0, "Guide snippet", .95);
        EmbeddingChunkEntity source = source(1L, "/docs/webjetcms/sk/guide.md", "sk");
        source.setSourceTitle("Guide");
        doReturn(List.of(hit)).when(semanticSearch).searchChunks("query", 7, "sk", 10,
            RagEntityType.MARKDOWN, Map.of("sourceRoot", "/docs/webjetcms"), request);
        when(repository.findAllById(any())).thenReturn(List.of(source));
        when(ragService.answerQuestion("query", 7, List.of(hit), request)).thenReturn("<p>Guide answer.</p>");

        MarkdownSearchService.SearchResponse response = service.search("query", "sk", null, 7, request);

        assertEquals(List.of(new MarkdownSearchService.SearchResult("Guide", "/docs/webjetcms/#/sk/guide",
            "/docs/webjetcms/sk/guide.md", "Guide snippet", .95)), response.results());
        assertEquals(answerAllowed ? "Guide answer." : null, response.answer());
        assertEquals("Guide", hit.getSourceTitle());
        assertEquals("/docs/webjetcms/#/sk/guide", hit.getSourceUrl());
        verify(ragService, times(answerAllowed ? 1 : 0)).answerQuestion("query", 7, List.of(hit), request);
    }
    /** Searches public and filesystem roots anonymously while excluding stale, blocked, and other-language sources. */
    @Test
    void searchesAccessibleRootsInSelectedLanguage() {
        Constants.setString("ragMarkdownFolders", "/docs/webjetcms,/docs/client,/admin/docs/private,file:/docs");
        Constants.setString("pathFilterBlockedPaths", "/private");
        List<VectorSearchResult> hits = new ArrayList<>();
        List<EmbeddingChunkEntity> sources = new ArrayList<>();
        List<String> paths = List.of("/docs/webjetcms/sk/guide.md", "/docs/client/sk/guide.md",
            "/docs/client/en/guide.md", "/admin/docs/private/sk/guide.md", "/removed/sk/guide.md", "file:/docs/sk/guide.md");
        for (int i = 0; i < paths.size(); i++) {
            long id = i + 1;
            hits.add(new VectorSearchResult(id, "markdown", id, 0, "Guide snippet", .95));
            sources.add(source(id, paths.get(i), i == 2 ? "en" : "sk"));
        }
        Map<String, Object> filters = Map.of("sourceRoots", List.of("/docs/webjetcms", "/docs/client", "file:/docs"));
        doReturn(hits).when(semanticSearch).searchChunks("query", 7, "sk", 10, RagEntityType.MARKDOWN, filters, request);
        when(repository.findAllById(any())).thenReturn(sources);

        MarkdownSearchService.SearchResponse response = service.search("query", "SK", null, 7, request);

        assertEquals(List.of(paths.get(0), paths.get(1), paths.get(5)), response.results().stream().map(MarkdownSearchService.SearchResult::sourcePath).toList());
        verify(semanticSearch).searchChunks("query", 7, "sk", 10, RagEntityType.MARKDOWN, filters, request);
        verify(ragService).answerQuestion("query", 7, List.of(hits.get(0), hits.get(1), hits.get(5)), request);
    }

    /** Keeps all-root searches empty or forbidden without invoking an embedding provider when no root is accessible. */
    @Test
    void doesNotSearchWithoutAccessibleRoots() {
        Constants.setString("ragMarkdownFolders", "");
        assertTrue(service.search("query", "sk", null, 1, request).results().isEmpty());
        Constants.setString("ragMarkdownFolders", "file:/docs/sk/admin,/admin/docs/webjetcms");
        Constants.setString("pathFilterBlockedPaths", "/docs");
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
            () -> service.search("query", "sk", null, 1, request)).getStatusCode());
        verifyNoInteractions(semanticSearch, repository, ragService);
    }

    /** Resolves viewer directories for anonymous searches inside application, filesystem, and narrowed language roots. */
    @ParameterizedTest
    @CsvSource({
        "/docs/webjetcms,/sk/admin/,/docs/webjetcms/sk/admin",
        "file:/docs,/sk/admin/,file:/docs/sk/admin",
        "file:/docs/sk/admin,/sk/admin/,file:/docs/sk/admin",
        "file:/docs/sk/admin/users,/sk/admin/,file:/docs/sk/admin/users",
        "file:/docs/sk/admin,/sk/admin/users/,file:/docs/sk/admin/users",
        "file:/docs/sk/redactor,/sk/admin/,",
        "file:/docs/en/admin,/sk/admin/,"
    })
    void scopesRetrievalToCurrentDirectory(String root, String directory, String expectedPath) {
        Constants.setString("ragMarkdownFolders", root);
        if (expectedPath != null) {
            doReturn(List.of()).when(semanticSearch).searchChunks("query", 7, "sk", 10,
                RagEntityType.MARKDOWN, Map.of("sourceRoot", expectedPath), request);
        }

        MarkdownSearchService.SearchResponse response = service.search("query", "sk", directory, 7, request);

        assertTrue(response.results().isEmpty());
        assertNull(response.answer());
        if (expectedPath == null) verifyNoInteractions(semanticSearch);
        else verify(semanticSearch).searchChunks("query", 7, "sk", 10,
            RagEntityType.MARKDOWN, Map.of("sourceRoot", expectedPath), request);
        verifyNoInteractions(repository, ragService);
    }

    /** Includes descendants in every accessible root but excludes siblings from both results and AI context. */
    @Test
    void directoryFilterRestrictsResultsAndAnswerRecursively() {
        Constants.setString("ragMarkdownFolders", "/docs/webjetcms,/docs/client/sk/admin,/docs/excluded/sk/redactor,/admin/docs/private");
        Constants.setString("pathFilterBlockedPaths", "/private");
        List<String> paths = List.of("/docs/webjetcms/sk/admin/README.md", "/docs/webjetcms/sk/admin/users/groups/guide.md",
            "/docs/client/sk/admin/users/guide.md", "/docs/webjetcms/sk/administrator/guide.md",
            "/docs/webjetcms/sk/redactor/guide.md", "/docs/excluded/sk/redactor/guide.md", "/admin/docs/private/sk/admin/guide.md");
        List<VectorSearchResult> hits = new ArrayList<>();
        List<EmbeddingChunkEntity> sources = new ArrayList<>();
        for (int i = 0; i < paths.size(); i++) {
            long id = i + 1;
            hits.add(new VectorSearchResult(id, "markdown", id, 0, "Guide snippet", .95));
            sources.add(source(id, paths.get(i), "sk"));
        }
        Map<String, Object> filters = Map.of("sourceRoots", List.of("/docs/webjetcms/sk/admin", "/docs/client/sk/admin"));
        doReturn(hits).when(semanticSearch).searchChunks("query", 7, "sk", 10, RagEntityType.MARKDOWN, filters, request);
        when(repository.findAllById(any())).thenReturn(sources);

        MarkdownSearchService.SearchResponse response = service.search("query", "sk", "/sk/admin/", 7, request);

        assertEquals(paths.subList(0, 3), response.results().stream().map(MarkdownSearchService.SearchResult::sourcePath).toList());
        assertEquals("/docs/webjetcms/#/sk/admin/", response.results().get(0).url());
        verify(semanticSearch).searchChunks("query", 7, "sk", 10, RagEntityType.MARKDOWN, filters, request);
        verify(ragService).answerQuestion("query", 7, hits.subList(0, 3), request);
    }

    /** Rejects malformed or other-language directories before invoking an embedding provider. */
    @ParameterizedTest
    @ValueSource(strings = {"", "/", "sk/admin/", "/sk/admin", "/sk/../", "/sk//admin/", "/sk/admin\\users/", "/en/admin/"})
    void rejectsInvalidDirectory(String directory) {
        assertThrows(IllegalArgumentException.class,
            () -> service.search("query", "sk", directory, 7, request));
        verifyNoInteractions(semanticSearch, repository, ragService);
    }

    /** Preserves explicit file permissions for both anonymous searches and ordinary logged-in users. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void inaccessibleFilesAreExcludedFromResultsAndAnswers(boolean requireLogin) {
        Constants.setBoolean("ragMarkdownSearchRequireLogin", requireLogin);
        Constants.setString("ragMarkdownFolders", "/files/documentation");
        Identity user = requireLogin ? new Identity() : null;
        if (user != null) request.getSession().setAttribute(Constants.USER_KEY, user);
        String sourcePath = "/files/documentation/sk/private.md";
        VectorSearchResult hit = new VectorSearchResult(1L, "markdown", 1L, 0, "Private snippet", .95);
        doReturn(List.of(hit)).when(semanticSearch).searchChunks("query", 7, "sk", 10,
            RagEntityType.MARKDOWN, Map.of("sourceRoot", "/files/documentation"), request);
        when(repository.findAllById(any())).thenReturn(List.of(source(1L, sourcePath, "sk")));
        EditForm protection = mock(EditForm.class);
        try (var paths = mockStatic(PathFilter.class)) {
            paths.when(() -> PathFilter.isPasswordProtected(sourcePath, request)).thenReturn(protection);

            MarkdownSearchService.SearchResponse response = service.search("query", "sk", null, 7, request);

            assertTrue(response.results().isEmpty());
            assertNull(response.answer());
            verify(protection).isAccessibleFor(user);
            verifyNoInteractions(ragService);
        }
    }

    /** Creates completed Markdown metadata in the shared domain for a retrieval fixture. */
    private EmbeddingChunkEntity source(long id, String path, String language) {
        EmbeddingChunkEntity source = new EmbeddingChunkEntity();
        source.setId(id);
        source.setEntityId(id);
        source.setEntityType(RagEntityType.MARKDOWN);
        source.setSourcePath(path);
        source.setLanguage(language);
        source.setDomainId(0);
        source.setStatus(EmbeddingChunkStatus.COMPLETED);
        return source;
    }
}
