package sk.iway.iwcm.components.redirects;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.components.redirects.RedirectCleaningService.ExecutionResult;
import sk.iway.iwcm.system.UrlRedirectDB;

/**
 * Coordinates the domain-scoped redirect cleaning workflow for all
 * administrators on one application node. Completed plans are shared through
 * the application cache, while an atomic in-memory operation marker prevents
 * concurrent analysis and execution for the same domain ID.
 */
@Service
class RedirectCleaningPlanCoordinator {

    static final int PLAN_CACHE_SECONDS = 60 * 60;
    private static final String PLAN_CACHE_KEY_PREFIX = RedirectCleaningRestController.class.getName() + ".plan.";

    private final RedirectCleaningService clearingService;
    private final Cache cache;
    private final Runnable redirectCacheRefresh;
    private final ConcurrentMap<Integer, OperationType> activeOperations = new ConcurrentHashMap<>();

    /** Operations which exclusively modify the shared workflow state of one domain. */
    enum OperationType {
        ANALYZE,
        EXECUTE
    }

    /**
     * Creates the production coordinator using the application-wide WebJET cache.
     *
     * @param clearingService redirect analysis and execution service
     */
    @Autowired
    RedirectCleaningPlanCoordinator(RedirectCleaningService clearingService) {
        this(clearingService, Cache.getInstance());
    }

    /**
     * Creates a coordinator with an explicit cache, primarily for focused tests.
     *
     * @param clearingService redirect analysis and execution service
     * @param cache shared plan cache
     */
    RedirectCleaningPlanCoordinator(RedirectCleaningService clearingService, Cache cache) {
        this(clearingService, cache, UrlRedirectDB::refreshCache);
    }

    /**
     * Creates a coordinator with explicit shared state and refresh callback.
     *
     * @param clearingService redirect analysis and execution service
     * @param cache shared plan cache
     * @param redirectCacheRefresh callback invoked once after successful execution
     */
    RedirectCleaningPlanCoordinator(RedirectCleaningService clearingService, Cache cache, Runnable redirectCacheRefresh) {
        this.clearingService = clearingService;
        this.cache = cache;
        this.redirectCacheRefresh = redirectCacheRefresh;
    }

    /**
     * Returns the current shared plan for a domain.
     *
     * @param domainId current domain ID
     * @return cached plan, or {@code null} when absent or expired
     */
    RedirectCleaningPlan getPlan(int domainId) {
        return cache.getObject(getPlanCacheKey(domainId), RedirectCleaningPlan.class);
    }

    /**
     * Runs one domain analysis and replaces the shared plan only after the
     * analysis succeeds. A previous plan therefore remains available after an
     * analysis failure.
     *
     * @param domainId current domain ID
     * @param includeUnnamed whether the unnamed scope is included alongside the
     *        selected named domain
     * @return newly cached cleaning plan
     */
    RedirectCleaningPlan analyze(int domainId, boolean includeUnnamed) {
        acquire(domainId, OperationType.ANALYZE);
        try {
            RedirectCleaningPlan plan = clearingService.analyze(domainId, includeUnnamed);
            cache.setObjectSeconds(getPlanCacheKey(domainId), plan, PLAN_CACHE_SECONDS, false);
            return plan;
        } finally {
            release(domainId, OperationType.ANALYZE);
        }
    }

    /**
     * Executes the current shared plan, removes it after successful database
     * changes, and refreshes the runtime redirect cache before releasing the
     * domain operation marker. Failed execution preserves the plan for retry.
     *
     * @param domainId current domain ID
     * @return execution counters
     * @throws MissingPlanException when no non-empty cached plan exists
     */
    ExecutionResult execute(int domainId) {
        acquire(domainId, OperationType.EXECUTE);
        try {
            RedirectCleaningPlan plan = getPlan(domainId);
            if (plan == null || plan.isEmpty()) throw new MissingPlanException();

            ExecutionResult result = clearingService.execute(plan);
            cache.removeObject(getPlanCacheKey(domainId));
            try {
                redirectCacheRefresh.run();
            } catch (RuntimeException exception) {
                Logger.error(RedirectCleaningPlanCoordinator.class, "Redirect cache refresh failed after clearing", exception);
            }
            return result;
        } finally {
            release(domainId, OperationType.EXECUTE);
        }
    }

    /**
     * Atomically marks one operation as active for the domain.
     *
     * @param domainId current domain ID
     * @param requestedOperation requested operation
     * @throws OperationInProgressException when another operation is active
     */
    private void acquire(int domainId, OperationType requestedOperation) {
        OperationType activeOperation = activeOperations.putIfAbsent(domainId, requestedOperation);
        if (activeOperation != null) throw new OperationInProgressException(activeOperation);
    }

    /** Releases only the marker owned by the completing operation. */
    private void release(int domainId, OperationType operation) {
        activeOperations.remove(domainId, operation);
    }

    /** @return application cache key for one domain ID */
    static String getPlanCacheKey(int domainId) {
        return PLAN_CACHE_KEY_PREFIX + domainId;
    }

    /** Indicates that another redirect cleaning operation owns the domain marker. */
    static class OperationInProgressException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final OperationType activeOperation;

        OperationInProgressException(OperationType activeOperation) {
            super("Redirect cleaning operation already in progress: " + activeOperation);
            this.activeOperation = activeOperation;
        }

        OperationType getActiveOperation() {
            return activeOperation;
        }
    }

    /** Indicates that the shared cache contains no executable plan. */
    static class MissingPlanException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        MissingPlanException() {
            super("No non-empty redirect cleaning plan is available");
        }
    }
}
