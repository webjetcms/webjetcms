package sk.iway.iwcm.components.redirects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.Getter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.redirects.RedirectClearingAction.ActionType;
import sk.iway.iwcm.system.RedirectsRepository;
import sk.iway.iwcm.system.UrlRedirectBean;

/**
 * Analyzes exact redirects and executes the resulting immutable clearing plan.
 * Analysis selects preferred targets, breaks cycles, compresses chains, and
 * removes duplicates without recursive traversal or per-row database queries.
 * Global redirects take precedence over domain-specific redirects in every phase.
 */
@Service
public class RedirectClearingService {

    private static final int EXECUTION_BATCH_SIZE = 500;

    private static final Comparator<Candidate> AGE_COMPARATOR = Comparator
        .comparing(Candidate::insertTime, Comparator.nullsFirst(Comparator.naturalOrder()))
        .thenComparing(Candidate::id, Comparator.nullsFirst(Comparator.naturalOrder()));

    private final RedirectsRepository redirectsRepository;

    /**
     * Creates the redirect clearing service.
     *
     * @param redirectsRepository repository used to load and modify redirects
     */
    public RedirectClearingService(RedirectsRepository redirectsRepository) {
        this.redirectsRepository = redirectsRepository;
    }

    /**
     * Loads exact redirects accessible from the selected domain and prepares a
     * clearing plan without modifying the database.
     *
     * @param currentDomain selected domain; {@code null} and an empty value mean global scope
     * @return immutable clearing plan
     */
    public RedirectClearingPlan analyze(String currentDomain) {
        String normalizedDomain = normalizeDomain(currentDomain);
        List<UrlRedirectBean> redirects = redirectsRepository.findAllForRedirectClearing(normalizedDomain);
        return analyze(normalizedDomain, redirects);
    }

    /**
     * Analyzes a supplied redirect collection. This package-level entry point is
     * used by tests and applies the same rules as {@link #analyze(String)}.
     *
     * @param currentDomain selected domain
     * @param redirects redirects to analyze
     * @return immutable clearing plan
     */
    RedirectClearingPlan analyze(String currentDomain, List<UrlRedirectBean> redirects) {
        String normalizedCurrentDomain = normalizeDomain(currentDomain);
        List<Candidate> candidates = new ArrayList<>();
        int ignoredRecords = 0;

        for (UrlRedirectBean redirect : redirects) {
            if (!isAnalyzable(redirect)) {
                ignoredRecords++;
                continue;
            }
            candidates.add(new Candidate(redirect));
        }

        Map<LogicalKey, List<Candidate>> versions = candidates.stream()
            .collect(Collectors.groupingBy(
                Candidate::logicalKey,
                LinkedHashMap::new,
                Collectors.toList()
            ));

        Map<GraphKey, Map<String, Edge>> graphs = new LinkedHashMap<>();
        for (Map.Entry<LogicalKey, List<Candidate>> entry : versions.entrySet()) {
            List<Candidate> versionRecords = entry.getValue();
            Candidate preferredNewest = preferredNewest(versionRecords);
            String winningTarget = preferredNewest.originalNewUrl;

            List<Candidate> winningRecords = new ArrayList<>();
            for (Candidate candidate : versionRecords) {
                if (Objects.equals(winningTarget, candidate.originalNewUrl)) {
                    winningRecords.add(candidate);
                } else {
                    candidate.action = ActionType.DELETE_OLD;
                }
            }

            LogicalKey key = entry.getKey();
            Map<String, Edge> graph = graphs.computeIfAbsent(key.graphKey(), ignored -> new LinkedHashMap<>());
            graph.put(key.oldUrl(), new Edge(key.oldUrl(), winningTarget, winningRecords));
        }

        for (Map<String, Edge> graph : graphs.values()) {
            breakCycles(graph);
            optimizeChains(graph);
        }

        deduplicate(candidates);

        List<RedirectClearingAction> actions = new ArrayList<>();
        for (Candidate candidate : candidates) {
            if (candidate.action == null && !Objects.equals(candidate.originalNewUrl, candidate.resultNewUrl)) {
                candidate.action = ActionType.UPDATE_OPTIMIZE;
            }
            if (candidate.action != null) actions.add(candidate.toAction());
        }

        return new RedirectClearingPlan(normalizedCurrentDomain, actions, redirects.size(), ignoredRecords);
    }

    /**
     * Executes exactly the actions stored in a previously analyzed plan. Updates
     * and deletes are grouped into batches of at most 500 identifiers. Records no
     * longer accessible from the selected domain are skipped by repository queries.
     *
     * @param plan immutable plan to execute
     * @param currentDomain currently selected domain
     * @return counts of updated, deleted, and skipped records
     * @throws IllegalStateException when the plan belongs to another domain
     */
    public ExecutionResult execute(RedirectClearingPlan plan, String currentDomain) {
        String normalizedCurrentDomain = normalizeDomain(currentDomain);
        if (!Objects.equals(plan.getAnalyzedDomain(), normalizedCurrentDomain)) {
            throw new IllegalStateException("Redirect clearing plan belongs to another domain");
        }

        int updated = 0;
        int deleted = 0;
        int skipped = 0;
        List<RedirectClearingAction> actions = plan.getActions();

        for (int offset = 0; offset < actions.size(); offset += EXECUTION_BATCH_SIZE) {
            List<RedirectClearingAction> batch = actions.subList(offset, Math.min(offset + EXECUTION_BATCH_SIZE, actions.size()));

            List<Long> deleteIds = new ArrayList<>();
            Map<String, List<Long>> updatesByTarget = new LinkedHashMap<>();
            for (RedirectClearingAction action : batch) {
                Long id = action.getId();
                if (id == null) continue;
                if (action.isDelete()) {
                    deleteIds.add(id);
                } else {
                    updatesByTarget.computeIfAbsent(action.getProposedNewUrl(), ignored -> new ArrayList<>()).add(id);
                }
            }

            int batchUpdated = 0;
            for (Map.Entry<String, List<Long>> entry : updatesByTarget.entrySet()) {
                batchUpdated += redirectsRepository.updateNewUrlForRedirectClearing(entry.getValue(), entry.getKey(), normalizedCurrentDomain);
            }
            int batchDeleted = deleteIds.isEmpty() ? 0 : redirectsRepository.deleteForRedirectClearing(deleteIds, normalizedCurrentDomain);

            updated += batchUpdated;
            deleted += batchDeleted;
            skipped += batch.size() - batchUpdated - batchDeleted;
        }

        return new ExecutionResult(updated, deleted, skipped);
    }

    /**
     * @param redirect database redirect to inspect
     * @return whether the record is a non-empty exact redirect supported by clearing
     */
    private static boolean isAnalyzable(UrlRedirectBean redirect) {
        return Tools.isNotEmpty(redirect.getOldUrl()) &&
            Tools.isNotEmpty(redirect.getNewUrl()) &&
            !redirect.getOldUrl().startsWith("regexp:");
    }

    /**
     * Normalizes both representations of a global domain to an empty string.
     *
     * @param domain domain value to normalize
     * @return empty string for {@code null} or empty input, otherwise the original value
     */
    static String normalizeDomain(String domain) {
        return domain == null || domain.isEmpty() ? "" : domain;
    }

    /**
     * Selects the newest candidate using insert date and identifier as a tie-breaker.
     *
     * @param candidates candidates to compare
     * @return newest candidate
     */
    private static Candidate newest(Collection<Candidate> candidates) {
        return candidates.stream().max(AGE_COMPARATOR).orElseThrow();
    }

    /**
     * Selects the newest global candidate when present; otherwise selects the
     * newest domain-specific candidate.
     *
     * @param candidates candidates to compare
     * @return preferred newest candidate
     */
    private static Candidate preferredNewest(Collection<Candidate> candidates) {
        List<Candidate> globalCandidates = candidates.stream()
            .filter(Candidate::isGlobal)
            .toList();
        return newest(globalCandidates.isEmpty() ? candidates : globalCandidates);
    }

    /**
     * Detects cycles iteratively and removes one edge from each cycle. A local
     * edge is preferred for removal; a global edge is removed only from a cycle
     * containing no local edge.
     *
     * @param graph redirect graph for one validity interval
     */
    private static void breakCycles(Map<String, Edge> graph) {
        Set<String> completed = new HashSet<>();

        for (String start : graph.keySet()) {
            if (completed.contains(start)) continue;

            List<Edge> path = new ArrayList<>();
            Map<String, Integer> pathIndexes = new HashMap<>();
            String current = start;

            while (true) {
                Edge edge = graph.get(current);
                if (edge == null || edge.removed || completed.contains(current)) break;

                Integer cycleStart = pathIndexes.get(current);
                if (cycleStart != null) {
                    List<Edge> cycleEdges = path.subList(cycleStart, path.size());
                    List<Edge> localCycleEdges = cycleEdges.stream()
                        .filter(edgeToRemove -> !edgeToRemove.isGlobal())
                        .toList();
                    Collection<Edge> removableEdges = localCycleEdges.isEmpty() ? cycleEdges : localCycleEdges;
                    Edge newestCycleEdge = removableEdges.stream()
                        .max(Comparator.comparing(Edge::newestCandidate, AGE_COMPARATOR))
                        .orElseThrow();
                    newestCycleEdge.removed = true;
                    newestCycleEdge.candidates.forEach(candidate -> candidate.action = ActionType.DELETE_CYCLE);
                    break;
                }

                pathIndexes.put(current, path.size());
                path.add(edge);
                current = edge.target;
            }

            path.forEach(edge -> completed.add(edge.source));
        }
    }

    /**
     * Compresses global and local chains using separate traversal scopes.
     *
     * @param graph acyclic redirect graph for one validity interval
     */
    private static void optimizeChains(Map<String, Edge> graph) {
        optimizeChains(graph, true);
        optimizeChains(graph, false);
    }

    /**
     * Resolves terminal URLs with path compression. Global traversal follows only
     * global edges, while local traversal may follow both local and global edges.
     *
     * @param graph acyclic redirect graph
     * @param globalScope {@code true} to optimize global edges, {@code false} for local edges
     */
    private static void optimizeChains(Map<String, Edge> graph, boolean globalScope) {
        Map<String, String> terminals = new HashMap<>();

        for (Edge start : graph.values()) {
            if (start.removed || start.isGlobal() != globalScope || terminals.containsKey(start.source)) continue;

            List<Edge> path = new ArrayList<>();
            String current = start.source;
            String terminal;

            while (true) {
                terminal = terminals.get(current);
                if (terminal != null) break;

                Edge edge = graph.get(current);
                if (edge == null || edge.removed || (globalScope && !edge.isGlobal())) {
                    terminal = current;
                    break;
                }

                path.add(edge);
                current = edge.target;
            }

            for (int index = path.size() - 1; index >= 0; index--) {
                Edge edge = path.get(index);
                terminals.put(edge.source, terminal);
                if (edge.isGlobal() == globalScope) {
                    for (Candidate candidate : edge.candidates) candidate.resultNewUrl = terminal;
                }
            }
        }
    }

    /**
     * Marks duplicate final redirects for deletion. The oldest global record is
     * preserved when present; otherwise the oldest record is preserved separately
     * for every domain.
     *
     * @param candidates analyzed redirect candidates
     */
    private static void deduplicate(List<Candidate> candidates) {
        Map<DuplicateKey, List<Candidate>> duplicates = candidates.stream()
            .filter(candidate -> candidate.action == null)
            .collect(Collectors.groupingBy(
                Candidate::duplicateKey,
                LinkedHashMap::new,
                Collectors.toList()
            ));

        for (List<Candidate> duplicateRecords : duplicates.values()) {
            if (duplicateRecords.size() < 2) continue;

            List<Candidate> globalRecords = duplicateRecords.stream()
                .filter(Candidate::isGlobal)
                .toList();
            if (!globalRecords.isEmpty()) {
                Candidate oldestGlobal = globalRecords.stream().min(AGE_COMPARATOR).orElseThrow();
                for (Candidate candidate : duplicateRecords) {
                    if (candidate != oldestGlobal) candidate.action = ActionType.DELETE_DUPLICATE;
                }
                continue;
            }

            Map<String, List<Candidate>> recordsByDomain = duplicateRecords.stream()
                .collect(Collectors.groupingBy(
                    candidate -> candidate.domain,
                    LinkedHashMap::new,
                    Collectors.toList()
                ));
            for (List<Candidate> domainRecords : recordsByDomain.values()) {
                if (domainRecords.size() < 2) continue;
                Candidate oldest = domainRecords.stream().min(AGE_COMPARATOR).orElseThrow();
                for (Candidate candidate : domainRecords) {
                    if (candidate != oldest) candidate.action = ActionType.DELETE_DUPLICATE;
                }
            }
        }
    }

    /**
     * Result counters returned after executing a clearing plan.
     */
    @Getter
    public static class ExecutionResult {
        /** Number of redirects whose target URL was updated. */
        private final int updated;

        /** Number of deleted redirects. */
        private final int deleted;

        /** Number of plan entries not modified because the record was unavailable. */
        private final int skipped;

        /**
         * @param updated number of updated redirects
         * @param deleted number of deleted redirects
         * @param skipped number of plan entries not modified
         */
        ExecutionResult(int updated, int deleted, int skipped) {
            this.updated = updated;
            this.deleted = deleted;
            this.skipped = skipped;
        }
    }

    /** Internal mutable analysis representation of one database redirect. */
    private static class Candidate {
        private final Long id;
        private final String domain;
        private final String originalDomain;
        private final String oldUrl;
        private final String originalNewUrl;
        private String resultNewUrl;
        private final Integer redirectCode;
        private final java.util.Date publishDate;
        private final java.util.Date validTo;
        private final java.util.Date insertDate;
        private ActionType action;

        Candidate(UrlRedirectBean redirect) {
            id = redirect.getUrlRedirectId();
            domain = normalizeDomain(redirect.getDomainName());
            originalDomain = redirect.getDomainName();
            oldUrl = redirect.getOldUrl();
            originalNewUrl = redirect.getNewUrl();
            resultNewUrl = originalNewUrl;
            redirectCode = redirect.getRedirectCode();
            publishDate = redirect.getPublishDate();
            validTo = redirect.getValidTo();
            insertDate = redirect.getInsertDate();
        }

        Long id() {
            return id;
        }

        Long insertTime() {
            return insertDate == null ? null : insertDate.getTime();
        }

        LogicalKey logicalKey() {
            return new LogicalKey(oldUrl, dateValue(publishDate), dateValue(validTo));
        }

        DuplicateKey duplicateKey() {
            return new DuplicateKey(oldUrl, resultNewUrl);
        }

        boolean isGlobal() {
            return domain.isEmpty();
        }

        RedirectClearingAction toAction() {
            String proposedUrl = action == ActionType.UPDATE_OPTIMIZE ? resultNewUrl : null;
            return new RedirectClearingAction(
                id,
                action,
                oldUrl,
                originalNewUrl,
                proposedUrl,
                originalDomain,
                redirectCode,
                publishDate,
                validTo,
                insertDate
            );
        }
    }

    /** One selected source-to-target edge and all records representing it. */
    private static class Edge {
        private final String source;
        private final String target;
        private final List<Candidate> candidates;
        private boolean removed;

        Edge(String source, String target, List<Candidate> candidates) {
            this.source = source;
            this.target = target;
            this.candidates = candidates;
        }

        Candidate newestCandidate() {
            return preferredNewest(candidates);
        }

        boolean isGlobal() {
            return candidates.stream().anyMatch(Candidate::isGlobal);
        }
    }

    /** Groups competing targets by source URL and validity interval. */
    private record LogicalKey(String oldUrl, Long publishDate, Long validTo) {
        GraphKey graphKey() {
            return new GraphKey(publishDate, validTo);
        }
    }

    /** Separates redirect graphs by validity interval. */
    private record GraphKey(Long publishDate, Long validTo) {
    }

    /** Identifies redirects with the same source and final target URL. */
    private record DuplicateKey(String oldUrl, String newUrl) {
    }

    /**
     * @param date date to convert
     * @return epoch milliseconds, or {@code null} when the date is absent
     */
    private static Long dateValue(java.util.Date date) {
        return date == null ? null : date.getTime();
    }
}
