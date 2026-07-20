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
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.Getter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.redirects.RedirectClearingAction.ActionType;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.system.RedirectsRepository;
import sk.iway.iwcm.system.UrlRedirectBean;

/**
 * Analyzes unconditional exact redirects and executes the resulting immutable
 * clearing plan.
 *
 * Regular-expression redirects and redirects with either a
 * publication date or a validity end date are ignored.
 *
 * Analysis selects preferred targets, breaks cycles, compresses chains, and
 * removes duplicates without recursive traversal or per-row database queries.
 * Every named domain is analyzed independently. Redirects with a {@code null}
 * or empty domain form one additional independent domain scope.
 */
@Service
public class RedirectClearingService {

    private static final int EXECUTION_BATCH_SIZE = 500;

    private static final Comparator<Candidate> AGE_COMPARATOR = Comparator
        .comparing(Candidate::insertTime, Comparator.nullsFirst(Comparator.naturalOrder()))
        .thenComparing(Candidate::id, Comparator.nullsFirst(Comparator.naturalOrder()));

    private final RedirectsRepository redirectsRepository;
    private final IntFunction<String> domainNameResolver;

    /**
     * Creates the redirect clearing service.
     *
     * @param redirectsRepository repository used to load and modify redirects
     */
    @Autowired
    public RedirectClearingService(RedirectsRepository redirectsRepository) {
        this(redirectsRepository, RedirectClearingService::resolveDomainName);
    }

    /**
     * Creates a service with an explicit domain resolver for focused tests.
     *
     * @param redirectsRepository repository used to load and modify redirects
     * @param domainNameResolver resolves a root group ID to its domain name
     */
    RedirectClearingService(RedirectsRepository redirectsRepository, IntFunction<String> domainNameResolver) {
        this.redirectsRepository = redirectsRepository;
        this.domainNameResolver = domainNameResolver;
    }

    /**
     * Loads exact redirects accessible from the selected domain and prepares a
     * clearing plan without modifying the database.
     *
     * @param currentDomainId selected application domain ID
     * @param includeUnnamed whether the independent unnamed scope is included
     *        alongside the selected named domain
     * @return immutable clearing plan
     */
    public RedirectClearingPlan analyze(int currentDomainId, boolean includeUnnamed) {
        String normalizedDomain = normalizeDomain(domainNameResolver.apply(currentDomainId));
        List<UrlRedirectBean> redirects = redirectsRepository.findAllForRedirectClearing(normalizedDomain, includeUnnamed);
        return analyze(currentDomainId, normalizedDomain, includeUnnamed, redirects);
    }

    /**
     * Resolves the domain name from its root group ID. A missing root group is
     * rejected to avoid accidentally treating an invalid domain as unnamed.
     *
     * @param domainId selected root group ID
     * @return domain name stored on the root group
     */
    private static String resolveDomainName(int domainId) {
        GroupDetails rootGroup = GroupsDB.getInstance().getGroup(domainId);
        if (rootGroup == null) {
            throw new IllegalStateException("Unable to resolve redirect clearing domain ID: " + domainId);
        }
        return rootGroup.getDomainName();
    }

    /**
     * Analyzes a supplied redirect collection. This package-level entry point is
     * used by tests and includes both the selected named scope and the unnamed
     * scope, matching the legacy analysis behavior.
     *
     * @param currentDomain selected domain
     * @param redirects redirects to analyze
     * @return immutable clearing plan
     */
    RedirectClearingPlan analyze(String currentDomain, List<UrlRedirectBean> redirects) {
        return analyze(-1, currentDomain, true, redirects);
    }

    /**
     * Analyzes a supplied redirect collection with an explicit unnamed-scope
     * selection. Records outside the selected scopes are not counted as ignored.
     *
     * @param currentDomain selected domain
     * @param includeUnnamed whether the independent unnamed scope is included
     *        alongside the selected named domain
     * @param redirects redirects available to the analysis
     * @return immutable clearing plan
     */
    RedirectClearingPlan analyze(String currentDomain, boolean includeUnnamed, List<UrlRedirectBean> redirects) {
        return analyze(-1, currentDomain, includeUnnamed, redirects);
    }

    /** Performs analysis for an application domain and its selected scopes. */
    private RedirectClearingPlan analyze(
        int currentDomainId,
        String currentDomain,
        boolean includeUnnamed,
        List<UrlRedirectBean> redirects
    ) {
        String normalizedCurrentDomain = normalizeDomain(currentDomain);
        List<Candidate> candidates = new ArrayList<>();
        int analyzedRecords = 0;
        int ignoredRecords = 0;

        for (UrlRedirectBean redirect : redirects) {
            if (!isInScope(redirect, normalizedCurrentDomain, includeUnnamed)) continue;
            analyzedRecords++;
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
            Candidate newest = newest(versionRecords);
            String winningTarget = newest.originalNewUrl;

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

        return new RedirectClearingPlan(
            currentDomainId,
            normalizedCurrentDomain,
            includeUnnamed,
            actions,
            analyzedRecords,
            ignoredRecords
        );
    }

    /**
     * Executes exactly the actions stored in a previously analyzed plan. Updates
     * and deletes are grouped into batches of at most 500 identifiers. Records no
     * longer accessible from the analyzed domain are skipped by repository queries.
     *
     * @param plan immutable plan to execute
     * @return counts of updated, deleted, and skipped records
     */
    @Transactional(transactionManager = "webjet2022TransactionManager")
    public ExecutionResult execute(RedirectClearingPlan plan) {
        String analyzedDomain = plan.getAnalyzedDomain();

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
                batchUpdated += redirectsRepository.updateNewUrlForRedirectClearing(entry.getValue(), entry.getKey(), analyzedDomain);
            }
            int batchDeleted = deleteIds.isEmpty() ? 0 : redirectsRepository.deleteForRedirectClearing(deleteIds, analyzedDomain);

            updated += batchUpdated;
            deleted += batchDeleted;
            skipped += batch.size() - batchUpdated - batchDeleted;
        }

        return new ExecutionResult(updated, deleted, skipped);
    }

    /**
     * Tests whether a redirect belongs to the selected named scope or, when
     * requested, the independent unnamed scope.
     */
    private static boolean isInScope(UrlRedirectBean redirect, String currentDomain, boolean includeUnnamed) {
        String redirectDomain = normalizeDomain(redirect.getDomainName());
        return Objects.equals(redirectDomain, currentDomain) || (includeUnnamed && redirectDomain.isEmpty());
    }

    /**
     * @param redirect database redirect to inspect
     * @return whether the record is a non-empty, unconditional exact redirect
     *         supported by clearing
     */
    private static boolean isAnalyzable(UrlRedirectBean redirect) {
        return Tools.isNotEmpty(redirect.getOldUrl()) &&
            Tools.isNotEmpty(redirect.getNewUrl()) &&
            !redirect.getOldUrl().startsWith("regexp:") &&
            redirect.getPublishDate() == null &&
            redirect.getValidTo() == null;
    }

    /**
     * Normalizes both representations of the unnamed domain scope to an empty string.
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
     * Detects cycles iteratively and removes the newest edge from each cycle.
     * The supplied graph contains redirects from exactly one domain scope.
     *
     * @param graph redirect graph for one domain
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
                    Edge newestCycleEdge = cycleEdges.stream()
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
     * Resolves terminal URLs with path compression inside one domain scope.
     *
     * @param graph acyclic redirect graph for one domain
     */
    private static void optimizeChains(Map<String, Edge> graph) {
        Map<String, String> terminals = new HashMap<>();

        for (Edge start : graph.values()) {
            if (start.removed || terminals.containsKey(start.source)) continue;

            List<Edge> path = new ArrayList<>();
            String current = start.source;
            String terminal;

            while (true) {
                terminal = terminals.get(current);
                if (terminal != null) break;

                Edge edge = graph.get(current);
                if (edge == null || edge.removed) {
                    terminal = current;
                    break;
                }

                path.add(edge);
                current = edge.target;
            }

            for (int index = path.size() - 1; index >= 0; index--) {
                Edge edge = path.get(index);
                terminals.put(edge.source, terminal);
                for (Candidate candidate : edge.candidates) candidate.resultNewUrl = terminal;
            }
        }
    }

    /**
     * Marks duplicate final redirects for deletion. Duplicate keys include the
     * normalized domain, so every domain scope preserves its own oldest record.
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

            Candidate oldest = duplicateRecords.stream().min(AGE_COMPARATOR).orElseThrow();
            for (Candidate candidate : duplicateRecords) {
                if (candidate != oldest) candidate.action = ActionType.DELETE_DUPLICATE;
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
            return new LogicalKey(domain, oldUrl);
        }

        DuplicateKey duplicateKey() {
            return new DuplicateKey(domain, oldUrl, resultNewUrl);
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
            return newest(candidates);
        }
    }

    /** Groups competing targets by domain and source URL. */
    private record LogicalKey(String domain, String oldUrl) {
        GraphKey graphKey() {
            return new GraphKey(domain);
        }
    }

    /** Separates redirect graphs by domain. */
    private record GraphKey(String domain) {
    }

    /** Identifies redirects with the same domain, source, and final target URL. */
    private record DuplicateKey(String domain, String oldUrl, String newUrl) {
    }
}
