package sk.iway.iwcm.components.redirects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;

/**
 * Immutable serializable snapshot of redirect cleaning analysis for one domain.
 * The snapshot is shared through the application cache and executed without
 * recalculating its actions.
 */
@Getter
class RedirectCleaningPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Domain ID used as the shared application cache scope. */
    private final int analyzedDomainId;

    /** Normalized domain for which this plan was prepared. */
    private final String analyzedDomain;

    /** Whether the unnamed scope was explicitly included alongside a named domain. */
    private final boolean includeUnnamed;

    /** Immutable list of operations to execute. */
    private final List<RedirectCleaningAction> actions;

    /** Number of database records loaded for analysis. */
    private final int analyzedRecords;

    /**
     * Number of records excluded from analysis: regular-expression redirects,
     * dated redirects, and redirects with an empty source or target URL.
     */
    private final int ignoredRecords;

    /**
     * Creates an immutable cleaning snapshot.
     *
     * @param analyzedDomainId selected domain ID
     * @param analyzedDomain normalized selected domain name
     * @param includeUnnamed whether the unnamed scope was explicitly included
     *        alongside a named domain
     * @param actions operations produced by analysis
     * @param analyzedRecords number of loaded redirect records
     * @param ignoredRecords number of regular-expression, dated, or invalid
     *        empty-URL redirect records excluded from analysis
     */
    RedirectCleaningPlan(
        int analyzedDomainId,
        String analyzedDomain,
        boolean includeUnnamed,
        List<RedirectCleaningAction> actions,
        int analyzedRecords,
        int ignoredRecords
    ) {
        this.analyzedDomainId = analyzedDomainId;
        this.analyzedDomain = analyzedDomain;
        this.includeUnnamed = includeUnnamed;
        this.actions = Collections.unmodifiableList(new ArrayList<>(actions));
        this.analyzedRecords = analyzedRecords;
        this.ignoredRecords = ignoredRecords;
    }

    /**
     * @return number of target URL updates in this plan
     */
    public int getUpdateCount() {
        return (int) actions.stream().filter(action -> !action.isDelete()).count();
    }

    /**
     * @return number of redirect deletions in this plan
     */
    public int getDeleteCount() {
        return (int) actions.stream().filter(RedirectCleaningAction::isDelete).count();
    }

    /**
     * @return {@code true} when the analysis produced no operations
     */
    public boolean isEmpty() {
        return actions.isEmpty();
    }
}
