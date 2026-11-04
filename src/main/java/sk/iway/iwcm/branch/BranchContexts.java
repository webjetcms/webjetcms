package sk.iway.iwcm.branch;

import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.RequestBean;

/** Request-scoped context access using WebJET's existing request lifecycle. */
public final class BranchContexts {
    public static final String ATTRIBUTE = BranchContext.class.getName();

    private BranchContexts() { }

    /** Background operations have LIVE context unless explicitly passed another context. */
    public static BranchContext current() {
        Object value = RequestBean.getAttribute(ATTRIBUTE);
        return value instanceof BranchContext context ? context : BranchContext.live("", 0);
    }

    public static BranchContext current(HttpServletRequest request) {
        Object value = request == null ? null : request.getAttribute(ATTRIBUTE);
        return value instanceof BranchContext context ? context : current();
    }

    /** Installs a verified context and restores the previous context even after an exception. */
    public static Scope open(HttpServletRequest request, BranchContext context) {
        return new Scope(request, Objects.requireNonNull(context, "context"));
    }

    /** Forces technical reads to LIVE without changing the administrator's session selection. */
    public static Scope live(HttpServletRequest request, String domainName, int userId) {
        return open(request, BranchContext.live(domainName, userId));
    }

    /** Rejects stale screens, including two LIVE screens belonging to different domains. */
    public static void requireSameSelection(BranchContext selected, long branchId, String domainName) {
        if (selected.branchId() != branchId || !selected.domainName().equals(domainName))
            throw new IllegalStateException("Branch context changed; reload the screen");
    }

    public static final class Scope implements AutoCloseable {
        private final HttpServletRequest request;
        private final Object requestValue;
        private final Object beanValue;
        private boolean closed;

        private Scope(HttpServletRequest request, BranchContext context) {
            this.request = request;
            requestValue = request == null ? null : request.getAttribute(ATTRIBUTE);
            beanValue = RequestBean.getAttribute(ATTRIBUTE);
            if (request != null) request.setAttribute(ATTRIBUTE, context);
            RequestBean.setAttribute(ATTRIBUTE, context);
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            if (request != null) {
                if (requestValue == null) request.removeAttribute(ATTRIBUTE);
                else request.setAttribute(ATTRIBUTE, requestValue);
            }
            RequestBean.setAttribute(ATTRIBUTE, beanValue);
        }
    }
}
