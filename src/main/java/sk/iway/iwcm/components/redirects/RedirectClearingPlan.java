package sk.iway.iwcm.components.redirects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;

/**
 * Immutable serializable snapshot of redirect clearing analysis for one selected domain.
 * The snapshot is stored in the HTTP session and executed without recalculating its actions.
 */
@Getter
class RedirectClearingPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Normalized domain for which this plan was prepared. */
    private final String analyzedDomain;

    /** Immutable list of operations to execute. */
    private final List<RedirectClearingAction> actions;

    /** Number of database records loaded for analysis. */
    private final int analyzedRecords;

    /** Number of records excluded because they are not analyzable exact redirects. */
    private final int ignoredRecords;

    /**
     * Creates an immutable clearing snapshot.
     *
     * @param analyzedDomain normalized selected domain
     * @param actions operations produced by analysis
     * @param analyzedRecords number of loaded redirect records
     * @param ignoredRecords number of ignored redirect records
     */
    RedirectClearingPlan(String analyzedDomain, List<RedirectClearingAction> actions, int analyzedRecords, int ignoredRecords) {
        this.analyzedDomain = analyzedDomain;
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
        return (int) actions.stream().filter(RedirectClearingAction::isDelete).count();
    }

    /**
     * @return {@code true} when the analysis produced no operations
     */
    public boolean isEmpty() {
        return actions.isEmpty();
    }
}
