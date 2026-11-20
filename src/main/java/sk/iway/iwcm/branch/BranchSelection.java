package sk.iway.iwcm.branch;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.doc.GroupsDB;
import java.sql.SQLException;

/** Session selection is only a hint; every request rechecks user, domain and package state. */
public final class BranchSelection {
    public static final String SESSION_KEY = BranchSelection.class.getName();
    public static final String ID_HEADER = "X-WebJET-Branch";
    public static final String DOMAIN_HEADER = "X-WebJET-Branch-Domain";
    private static final String SCREEN_BRANCH = BranchSelection.class.getName() + ".screenBranch";
    private BranchSelection() { }

    public record Selected(long branchId, int domainId) implements java.io.Serializable { }

    public static BranchContext selected(HttpServletRequest request, Identity user, String domain) {
        return selected(request, user, domain, false);
    }

    static BranchContext selected(HttpServletRequest request, Identity user, String domain, boolean clearClosed) {
        var session = request.getSession();
        Object value = session.getAttribute(SESSION_KEY);
        if (!(value instanceof Selected selected) || BranchDomains.id(domain) != selected.domainId())
            return BranchContext.live(domain, user.getUserId());
        if (selected.branchId() == 0) return BranchContext.live(domain, user.getUserId());
        BranchEntity branch = findBranch(selected.branchId(), domain);
        if (isClosed(branch)) {
            if (!clearClosed) throw closedBranch();
            synchronized (session) {
                // Another tab may already have changed the selection while we were reading the package.
                if (selected.equals(session.getAttribute(SESSION_KEY))) {
                    session.removeAttribute(SESSION_KEY);
                    session.removeAttribute(BranchProductionSection.ATTRIBUTE);
                    return BranchContext.live(domain, user.getUserId());
                }
            }
            return selected(request, user, domain);
        }
        RequestBean.setAttribute(SCREEN_BRANCH, branch);
        return new BranchContext(selected.branchId(), domain, user.getUserId(), BranchContext.Mode.ADMIN);
    }

    /** Metadata already verified for this request, used for the initial administration header. */
    public static BranchEntity screenBranch(long id, String domain) {
        return RequestBean.getAttribute(SCREEN_BRANCH) instanceof BranchEntity branch
            && branch.getId() == id && branch.getDomainId() == BranchDomains.id(domain) ? branch : null;
    }

    public static BranchEntity requireBranch(long id, String domain) {
        BranchEntity branch = findBranch(id, domain);
        if (isClosed(branch)) throw closedBranch();
        return branch;
    }

    private static boolean isClosed(BranchEntity branch) {
        return "MERGED".equals(branch.getState()) || "CANCELLED".equals(branch.getState());
    }

    private static ResponseStatusException closedBranch() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Publishing package is closed; reload the administration screen");
    }

    private static BranchEntity findBranch(long id, String domain) {
        try {BranchReadiness.verify();}
        catch(SQLException|IllegalStateException ex) {throw new ResponseStatusException(HttpStatus.CONFLICT,"Branch prerequisites failed: "+ex.getMessage(),ex);}
        return BranchService.getInstance().find(id, domain).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Publishing package not found"));
    }

    /** The gateway resolves the logical domain from server metadata, never from its query string. */
    public static String previewDomain(long id, Identity user) {
        String domain = BranchDomains.name(BranchService.getInstance().previewDomainId(id));
        if (!GroupsDB.getInstance().getUserRootDomainNames(user).contains(domain))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No access to preview domain");
        requireBranch(id, domain);
        return domain;
    }
}
