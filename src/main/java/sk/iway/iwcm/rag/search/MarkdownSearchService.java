package sk.iway.iwcm.rag.search;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.SpamProtection;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.filebrowser.EditForm;
import sk.iway.iwcm.rag.service.MarkdownIndexService;
import sk.iway.iwcm.rag.service.MarkdownSourceService;
import sk.iway.iwcm.rag.service.RagEntityType;
import sk.iway.iwcm.rag.service.RagSettingsService;
import sk.iway.iwcm.rag.vectorjpa.EmbeddingChunkEntity;
import sk.iway.iwcm.rag.vectorjpa.EmbeddingChunkRepository;
import sk.iway.iwcm.rag.vectorjpa.EmbeddingChunkStatus;
import sk.iway.iwcm.rag.vectorstore.VectorSearchResult;
import sk.iway.iwcm.system.multidomain.DomainRequestBeanScope;
import sk.iway.iwcm.users.UsersDB;

/** Searches configured Markdown documentation using the shared semantic, hybrid and RAG pipelines. */
@Service
public class MarkdownSearchService {

    private static final int MAX_RESULTS = 10;
    private static final int MAX_QUERY_LENGTH = 2000;
    private static final int MAX_SNIPPET_LENGTH = 350;
    private static final int CACHE_SECONDS = 30 * 60;
    private static final String CACHE_PREFIX = "MarkdownSearchService.";

    private final MarkdownSourceService sourceService;
    private final SemanticSearchService semanticSearchService;
    private final EmbeddingChunkRepository chunkRepository;
    private final RagService ragService;

    public MarkdownSearchService(MarkdownSourceService sourceService, SemanticSearchService semanticSearchService,
            EmbeddingChunkRepository chunkRepository, RagService ragService) {
        this.sourceService = sourceService;
        this.semanticSearchService = semanticSearchService;
        this.chunkRepository = chunkRepository;
        this.ragService = ragService;
    }

    /**
     * Searches accessible globally configured roots in the selected language in the shared Markdown domain zero.
     * Search availability is checked before validation, source access and cache lookup.
     * Access checks run before embeddings and again for individual sources before answer generation.
     * Embeddings and answers use the requesting domain's assistants, credentials and usage accounting.
     * Answers are generated automatically when ragAnswerAllowed is enabled and authorized context is available.
     * When ragMarkdownSearchRequireLogin is enabled, any authenticated user can search; administrator access is not required.
     * The shared search spam protection limits requests before any AI provider call.
     * Retrieval and answers are cached for 30 minutes using a lowercase, accent-free question key.
     * Cached chunks are authorized again and the current answer setting is checked on every request.
     * Missing cached chunk IDs trigger fresh retrieval after reindexing or deletion.
     *
     * @param query question or search terms
     * @param language required documentation language
     * @param directory optional viewer directory including its language and leading/trailing slashes; descendants are always included
     * @param domainId current request domain used for assistants and usage, not Markdown storage
     * @param request current HTTP request used for access checks and AI providers
     * @return unique source results with optional plain-text and safe HTML answers
     */
    public SearchResponse search(String query, String language, String directory, Integer domainId,
            HttpServletRequest request) {
        if (semanticSearchService.isAvailable() == false) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Semantic search is not available");
        }
        if (domainId == null || domainId < 1) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Documentation domain is not available");
        }
        List<String> configuredRoots = sourceService.getRoots();
        String normalizedQuery = query == null ? "" : query.trim();
        String normalizedLanguage = sourceService.requireLanguage(language);
        if (normalizedQuery.isEmpty() || normalizedQuery.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException("Search query must contain between 1 and 2000 characters");
        }

        Identity user = UsersDB.getCurrentUser(request);
        if (Constants.getBoolean("ragMarkdownSearchRequireLogin") && user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to search documentation");
        }
        List<String> roots = configuredRoots.stream().filter(root -> isBlockedPath(root) == false).toList();
        if (roots.isEmpty()) {
            if (configuredRoots.isEmpty()) return new SearchResponse(List.of(), null, null);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Documentation is not accessible");
        }

        List<String> searchPaths = getSearchPaths(roots, directory, normalizedLanguage);
        if (searchPaths.isEmpty()) return new SearchResponse(List.of(), null, null);
        if (SpamProtection.canPost("search", "", request) == false) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Search limit reached. Please try again later.");
        }
        Cache cache = Cache.getInstance();
        String cacheQuery = Normalizer.normalize(normalizedQuery, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
        String cacheKey = CACHE_PREFIX + DigestUtils.sha256Hex(new JSONArray(List.of(domainId, normalizedLanguage,
            searchPaths, request.getContextPath(), cacheQuery)).toString());
        CachedSearch cached = cache.getObject(cacheKey, CachedSearch.class);
        Map<Long, EmbeddingChunkEntity> metadata = cached == null ? Map.of() : loadSourceMetadata(cached.chunks());
        if (cached != null && metadata.keySet().containsAll(cached.chunks().stream().map(VectorSearchResult::getChunkId).toList()) == false) {
            cache.removeObject(cacheKey);
            cached = null;
        }
        List<VectorSearchResult> chunks;
        if (cached == null) {
            Map<String, Object> filters = searchPaths.size() == 1 ? Map.of("sourceRoot", searchPaths.get(0)) : Map.of("sourceRoots", searchPaths);
            chunks = semanticSearchService.searchChunks(normalizedQuery, domainId,
                normalizedLanguage, MAX_RESULTS, RagEntityType.MARKDOWN, filters, request);
            // Empty retrieval can also signal a temporary provider or vector store failure.
            if (chunks.isEmpty()) return new SearchResponse(List.of(), null, null);
            cached = new CachedSearch(copyChunks(chunks), Tools.getNow() + CACHE_SECONDS * 1000L, null, null);
            cache.setObjectByExpiry(cacheKey, cached, cached.expiresAt(), false);
            metadata = loadSourceMetadata(chunks);
        } else {
            chunks = copyChunks(cached.chunks());
        }
        List<VectorSearchResult> authorized = authorizeSources(chunks, metadata, roots, searchPaths, normalizedLanguage, user, request);
        authorized = semanticSearchService.rerankChunks(normalizedQuery, authorized);
        List<SearchResult> results;
        try (DomainRequestBeanScope ignored = DomainRequestBeanScope.open(null)) {
            results = aggregateResults(authorized);
        }

        String generatedAnswer = null;
        if (authorized.isEmpty() == false && RagSettingsService.isAnswerAllowed(null)) {
            List<Long> answerChunkIds = authorized.stream().map(VectorSearchResult::getChunkId).toList();
            if (answerChunkIds.equals(cached.answerChunkIds())) {
                generatedAnswer = cached.answer();
            } else {
                generatedAnswer = ragService.answerQuestion(normalizedQuery, domainId, authorized, request);
                if (Tools.isNotEmpty(generatedAnswer)) {
                    cached = new CachedSearch(cached.chunks(), cached.expiresAt(), answerChunkIds, generatedAnswer);
                    cache.setObjectByExpiry(cacheKey, cached, cached.expiresAt(), false);
                }
            }
        }
        return new SearchResponse(results, toPlainText(generatedAnswer), toSafeAnswerHtml(generatedAnswer));
    }

    /** Copies retrieval fields so authorization and reranking cannot mutate shared cache entries. */
    private List<VectorSearchResult> copyChunks(List<VectorSearchResult> chunks) {
        return chunks.stream().map(chunk -> new VectorSearchResult(chunk.getChunkId(), chunk.getEntityType(),
            chunk.getEntityId(), chunk.getChunkIndex(), chunk.getChunkText(), chunk.getSimilarity())).toList();
    }

    /** Keeps the original expiry when adding an answer generated from the currently authorized chunks. */
    private record CachedSearch(List<VectorSearchResult> chunks, long expiresAt, List<Long> answerChunkIds, String answer) { }

    /**
     * Intersects a viewer directory with accessible index roots before vector or full-text retrieval.
     * Roots already inside a language subtree retain their original scope when it is narrower than the requested directory.
     *
     * @param roots accessible configured roots
     * @param directory viewer directory, or null to search all accessible roots
     * @param language normalized search language
     * @return source path prefixes confined to configured roots, without trailing slashes
     * @throws IllegalArgumentException if the directory is invalid or does not start with the search language
     */
    private List<String> getSearchPaths(List<String> roots, String directory, String language) {
        if (directory == null) return roots;
        if (directory.startsWith("/") == false || directory.endsWith("/") == false || directory.length() < 3) {
            throw new IllegalArgumentException("A documentation directory must include leading and trailing slashes");
        }
        String relativeDirectory = sourceService.requireDirectory(directory.substring(1, directory.length() - 1));
        if (relativeDirectory.equals(language) == false && relativeDirectory.startsWith(language + "/") == false) {
            throw new IllegalArgumentException("The documentation directory must start with the search language");
        }
        List<String> languages = sourceService.getLanguages();
        List<String> paths = new ArrayList<>();
        for (String root : roots) {
            String viewerRoot = root;
            for (String ancestor = root; ancestor.contains("/"); ancestor = ancestor.substring(0, ancestor.lastIndexOf('/'))) {
                int slash = ancestor.lastIndexOf('/');
                if (languages.contains(ancestor.substring(slash + 1).toLowerCase(Locale.ROOT))) {
                    viewerRoot = ancestor.substring(0, slash);
                    break;
                }
            }
            String selected = viewerRoot + "/" + relativeDirectory;
            if (selected.equals(root) || selected.startsWith(root + "/")) paths.add(selected);
            else if (root.startsWith(selected + "/")) paths.add(root);
        }
        return paths.stream().distinct().toList();
    }

    /** Loads current chunk metadata for cache validation and source authorization. */
    private Map<Long, EmbeddingChunkEntity> loadSourceMetadata(List<VectorSearchResult> chunks) {
        Map<Long, EmbeddingChunkEntity> metadata = new HashMap<>();
        chunkRepository.findAllById(chunks.stream().map(VectorSearchResult::getChunkId).filter(Objects::nonNull).toList())
            .forEach(chunk -> metadata.put(chunk.getId(), chunk));
        return metadata;
    }

    /**
     * Filters retrieved chunks against stored metadata and source permissions, then adds citation metadata.
     *
     * @param chunks retrieved chunks to validate and enrich
     * @param metadata current stored metadata for the retrieved chunk IDs
     * @param roots accessible configured documentation roots
     * @param searchPaths permitted source path prefixes after applying the directory filter
     * @param language required source language
     * @param user current user, or {@code null} for an anonymous request
     * @param request request used for path protection and context-relative URLs
     * @return authorized chunks with titles, logical paths, and available viewer URLs
     */
    private List<VectorSearchResult> authorizeSources(List<VectorSearchResult> chunks, Map<Long, EmbeddingChunkEntity> metadata,
            List<String> roots, List<String> searchPaths, String language, Identity user, HttpServletRequest request) {
        if (chunks.isEmpty()) return List.of();

        List<VectorSearchResult> authorized = new ArrayList<>();
        for (VectorSearchResult chunk : chunks) {
            EmbeddingChunkEntity source = metadata.get(chunk.getChunkId());
            if (source == null || source.getEntityType() != RagEntityType.MARKDOWN ||
                    source.getStatus() != EmbeddingChunkStatus.COMPLETED ||
                    Objects.equals(source.getEntityId(), chunk.getEntityId()) == false ||
                    Objects.equals(MarkdownIndexService.SHARED_DOMAIN_ID, source.getDomainId()) == false ||
                    Objects.equals(language, source.getLanguage()) == false) continue;

            if (searchPaths.stream().noneMatch(path -> source.getSourcePath() != null && source.getSourcePath().startsWith(path + "/"))) continue;
            String root = roots.stream().filter(candidate -> sourceService.getRelativePath(candidate, source.getSourcePath()) != null)
                .findFirst().orElse(null);
            if (root == null) continue;
            String relativePath = source.getSourcePath().substring(root.length() + 1);
            String sourcePath = source.getSourcePath();
            if (isBlockedPath(sourcePath)) continue;
            if (sourceService.isFileSystemRoot(root) == false) {
                EditForm protection = PathFilter.isPasswordProtected(sourcePath, request);
                if (protection != null && protection.isAccessibleFor(user) == false) continue;
            }

            chunk.setSourceTitle(Tools.isEmpty(source.getSourceTitle()) ? relativePath : source.getSourceTitle());
            chunk.setSourcePath(sourcePath);
            String sourceUrl = sourceService.getUrl(root, relativePath);
            chunk.setSourceUrl(sourceUrl == null ? null : request.getContextPath() + sourceUrl);
            authorized.add(chunk);
        }
        return authorized;
    }

    /**
     * Keeps each source's best chunk, applies shared similarity settings, and builds bounded snippets.
     *
     * @param chunks authorized and enriched chunks
     * @return up to ten source results ordered by descending score
     */
    private List<SearchResult> aggregateResults(List<VectorSearchResult> chunks) {
        Map<Long, VectorSearchResult> bestChunks = new LinkedHashMap<>();
        chunks.stream().filter(chunk -> chunk.getSimilarity() != null || chunk.getRerankScore() != null)
            .sorted(Comparator.comparingDouble(VectorSearchResult::getRankingScore).reversed())
            .forEach(chunk -> bestChunks.putIfAbsent(chunk.getEntityId(), chunk));

        List<SemanticSearchResult> scores = semanticSearchService.aggregateBySourceBestScore(chunks);
        return semanticSearchService.filterResultsBySimilarity(scores,
            RagSettingsService.getSemanticMinimumSimilarity(null), RagSettingsService.getSemanticMinimumResults(null))
            .stream().limit(MAX_RESULTS).map(result -> {
                VectorSearchResult chunk = bestChunks.get(result.getDocId());
                String snippet = Tools.getStringValue(chunk.getChunkText(), "").replace("\r\n", "\n").replace('\r', '\n').strip();
                if (snippet.length() > MAX_SNIPPET_LENGTH) snippet = snippet.substring(0, MAX_SNIPPET_LENGTH) + "…";
                return new SearchResult(chunk.getSourceTitle(), chunk.getSourceUrl(), chunk.getSourcePath(), snippet, chunk.getRankingScore());
            }).toList();
    }

    private boolean isBlockedPath(String path) {
        for (String blocked : Constants.getArray("pathFilterBlockedPaths")) {
            if (Tools.isNotEmpty(blocked) && path.contains(blocked)) return true;
        }
        return false;
    }

    /**
     * Converts sanitized answer HTML into text while retaining paragraph, list and table boundaries.
     *
     * @param answer generated HTML answer, possibly null or empty
     * @return plain text, or {@code null} when no text remains
     */
    static String toPlainText(String answer) {
        if (Tools.isEmpty(answer)) return null;
        Document document = Jsoup.parse(toSafeAnswerHtml(answer));
        document.select("br").before("\n").remove();
        document.select("p,div,li,h1,h2,h3,h4,h5,h6,blockquote,pre").after("\n\n");
        document.select("th,td").after("\t");
        document.select("tr").after("\n");
        String text = document.body().wholeText().replaceAll("\\n{3,}", "\n\n").trim();
        return text.isEmpty() ? null : text;
    }

    /**
     * Keeps answer formatting without links, images, attributes or executable content.
     *
     * @param answer generated HTML answer, possibly null or empty
     * @return safe HTML, or {@code null} when no answer is available
     */
    static String toSafeAnswerHtml(String answer) {
        if (Tools.isEmpty(answer)) return null;
        return Jsoup.clean(answer, "", new Safelist().addTags("p", "div", "br", "strong", "b", "em", "i",
            "ul", "ol", "li", "code", "pre", "blockquote", "h1", "h2", "h3", "h4", "h5", "h6", "details", "summary",
            "table", "caption", "thead", "tbody", "tfoot", "tr", "th", "td"),
            new Document.OutputSettings().prettyPrint(false));
    }

    /**
     * Represents one documentation source returned to a search client.
     *
     * @param title source heading or relative path fallback
     * @param url viewer URL, or {@code null} for filesystem sources
     * @param sourcePath full logical path including the configured root
     * @param snippet bounded Markdown excerpt from the best matching chunk, retaining line breaks and indentation
     * @param score best chunk rerank score when available, otherwise its retrieval score
     */
    public record SearchResult(String title, String url, String sourcePath, String snippet, Double score) {}

    /**
     * Carries ranked documentation sources and an optional generated answer.
     *
     * @param results unique authorized source results
     * @param answer plain-text answer, or {@code null} when none is available
     * @param answerHtml answer with safe formatting only, or {@code null} when none is available
     */
    public record SearchResponse(List<SearchResult> results, String answer, String answerHtml) {}
}
