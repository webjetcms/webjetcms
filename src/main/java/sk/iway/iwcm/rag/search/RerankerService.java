package sk.iway.iwcm.rag.search;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import sk.iway.iwcm.rag.service.RagSettingsService;
import sk.iway.iwcm.rag.vectorstore.VectorSearchResult;

/** Refines retrieval order using local word, phrase, and heading matches without model calls. */
@Service
public class RerankerService {
    private static final Set<String> STOP_WORDS = Set.of(
        "a", "an", "and", "are", "as", "at", "be", "by", "do", "for", "from", "how", "i", "in", "is", "it",
        "of", "on", "or", "the", "to", "what", "where", "which", "with", "you", "your",
        "aj", "ako", "ale", "bez", "co", "jak", "je", "jsou", "k", "na", "o", "od", "pre", "pri", "pro",
        "s", "sa", "se", "si", "som", "su", "v", "ve", "vo", "z", "ze", "zo");

    /**
     * Combines retrieval score with configured lexical relevance, defaulting to an 85/15 ratio.
     * Retains all authorized candidates, updates their rerank scores in place, and reads the weight on every call.
     * Original similarity remains available for filtering. Equal scores retain retrieval order.
     *
     * @param query original user question
     * @param chunks authorized retrieval candidates
     * @return locally ranked chunks without changing their text or original similarity
     */
    public List<VectorSearchResult> rerank(String query, List<VectorSearchResult> chunks) {
        List<String> queryWords = words(query).stream().filter(word -> STOP_WORDS.contains(word) == false).toList();
        if (queryWords.isEmpty() || chunks.isEmpty()) return chunks;

        double lexicalWeight = RagSettingsService.getRerankLexicalWeight();
        List<Passage> passages = chunks.stream().filter(chunk -> chunk.getSimilarity() != null).map(this::passage).toList();
        Map<String, Double> weights = termWeights(new LinkedHashSet<>(queryWords), passages);
        Set<List<String>> pairs = new LinkedHashSet<>();
        for (int i = 1; i < queryWords.size(); i++) {
            if (queryWords.get(i - 1).equals(queryWords.get(i)) == false) {
                pairs.add(List.of(queryWords.get(i - 1), queryWords.get(i)));
            }
        }

        List<VectorSearchResult> ranked = new ArrayList<>(chunks);
        for (Passage passage : passages) {
            double headingCoverage = passage.headings().stream()
                .mapToDouble(heading -> coverage(weights, new HashSet<>(heading))).max().orElse(0d);
            double phraseScore = Math.max(proximity(pairs, passage.body()), proximity(pairs, passage.title()));
            double lexical = .4 * headingCoverage + .35 * phraseScore + .25 * coverage(weights, passage.terms());
            passage.chunk().setRerankScore((1d - lexicalWeight) * passage.chunk().getSimilarity() + lexicalWeight * lexical);
        }
        ranked.sort(Comparator.comparingDouble(VectorSearchResult::getRankingScore).reversed());
        return ranked;
    }

    /**
     * Tokenizes once for scoring and source frequency, keeping passage positions intact for proximity.
     *
     * @param chunk candidate supplying body text, source title, and stored Markdown heading context
     * @return normalized tokens, heading tokens, and distinct terms associated with the candidate
     */
    private Passage passage(VectorSearchResult chunk) {
        String text = chunk.getChunkText() == null ? "" : chunk.getChunkText();
        String title = chunk.getSourceTitle() == null ? "" : chunk.getSourceTitle();
        List<String> bodyWords = words(text);
        List<String> titleWords = words(title);
        Set<String> terms = new HashSet<>(bodyWords);
        terms.addAll(titleWords);
        List<List<String>> headings = new ArrayList<>();
        headings.add(titleWords);
        if ("markdown".equalsIgnoreCase(chunk.getEntityType())) {
            // Trust the stored hierarchy; split code fragments can resemble Markdown headings.
            String firstLine = text.lines().findFirst().orElse("");
            if (!title.isBlank() && firstLine.startsWith(title + " > ")) {
                for (String heading : firstLine.split(" > ")) headings.add(words(heading));
            }
        }
        return new Passage(chunk, bodyWords, titleWords, headings, terms);
    }

    /**
     * Uses capped rarity in the retrieved sources, counting repeated chunks of each source only once.
     *
     * @param terms distinct query terms to weight
     * @param passages tokenized retrieval candidates used to count source frequency
     * @return weights from one to 2.5 in query-term iteration order
     */
    private Map<String, Double> termWeights(Set<String> terms, List<Passage> passages) {
        Map<String, Set<String>> sources = new HashMap<>();
        for (Passage passage : passages) {
            VectorSearchResult chunk = passage.chunk();
            String key = chunk.getEntityType() + ":" + chunk.getEntityId();
            sources.computeIfAbsent(key, ignored -> new HashSet<>()).addAll(passage.terms());
        }
        Map<String, Double> weights = new LinkedHashMap<>();
        for (String term : terms) {
            long frequency = sources.values().stream().filter(source -> source.contains(term)).count();
            weights.put(term, Math.min(2.5, 1d + Math.log((sources.size() + 1d) / (frequency + 1d))));
        }
        return weights;
    }

    private double coverage(Map<String, Double> weights, Set<String> text) {
        double total = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        return weights.entrySet().stream().filter(term -> text.contains(term.getKey()))
            .mapToDouble(Map.Entry::getValue).sum() / total;
    }

    /**
     * Averages the best forward gap for each distinct query pair; repeated occurrences add no bonus.
     *
     * @param pairs distinct ordered pairs of neighboring query terms
     * @param text passage tokens in their original order
     * @return mean inverse gap for matches up to four positions apart, or zero when no pair matches
     */
    private double proximity(Set<List<String>> pairs, List<String> text) {
        if (pairs.isEmpty()) return 0d;
        double score = 0d;
        for (List<String> pair : pairs) {
            int left = -1;
            double best = 0d;
            for (int i = 0; i < text.size(); i++) {
                if (text.get(i).equals(pair.get(1)) && left >= 0 && i - left <= 4) {
                    best = Math.max(best, 1d / (i - left));
                }
                if (text.get(i).equals(pair.get(0))) left = i;
            }
            score += best;
        }
        return score / pairs.size();
    }

    private List<String> words(String text) {
        String normalized = normalize(text);
        return normalized.isBlank() ? List.of() : Arrays.asList(normalized.split(" +"));
    }

    private String normalize(String text) {
        if (text == null) return "";
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
            .toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }

    /**
     * Holds normalized passage tokens used by local relevance scoring.
     *
     * @param chunk candidate whose rerank score is updated
     * @param body body tokens in source order
     * @param title source-title tokens in source order
     * @param headings tokenized title and stored heading hierarchy
     * @param terms distinct body and title terms used for coverage and source-frequency weights
     */
    private record Passage(VectorSearchResult chunk, List<String> body, List<String> title,
            List<List<String>> headings, Set<String> terms) { }
}
