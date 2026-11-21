package sk.iway.iwcm.branch;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.system.stripes.CSRF;
import sk.iway.iwcm.users.UsersDB;

/** Server-side functional permission and domain checks for branch management. */
@Component
public class BranchAccess {
    public static final String PERMISSION = "menuBranches";

    /** Returns whether working versions are enabled with their required domain separation. */
    public static boolean isEnabled() {
        return Constants.getBoolean("branchesEnabled") && Constants.getBoolean("enableStaticFilesExternalDir");
    }

    public Identity requireUser(HttpServletRequest request) {
        if (!isEnabled())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Branches are disabled");
        Identity user = UsersDB.getCurrentUser(request);
        if (user == null || !user.isAdmin() || !user.isEnabledItem(PERMISSION))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return user;
    }

    /** Reads the selected admin domain without invoking DocDB's legacy docid/historyid side effects. */
    public String requireDomain(HttpServletRequest request, Identity user) {
        HttpSession session = request.getSession(false);
        Object selected = session == null ? null : session.getAttribute("preview.editorDomainName");
        java.util.List<String> domains = GroupsDB.getInstance().getUserRootDomainNames(user);
        String domain = selected instanceof String value && !value.isBlank() ? value : null;
        if (domain == null) {
            // A separate admin host has no content root; bootstrap the same permitted default as the layout.
            String host = Tools.getServerName(request);
            domain = domains.contains(host) ? host : domains.stream().filter(value -> value != null && !value.isBlank()).findFirst().orElse(null);
            if (domain != null) request.getSession().setAttribute("preview.editorDomainName", domain);
        }
        if (domain == null || !domains.contains(domain))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No access to the selected domain");
        return domain;
    }

    public void requireExpectedDomain(int actual, Integer expected) {
        if (expected == null || actual != expected)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Administration domain changed; reload the screen");
    }

    /** Uses the same non-consuming token verification and API-token exemption as PathFilter. */
    public void requireCsrf(HttpServletRequest request) {
        if (request.getAttribute("csrfDisabled") != null) return;
        HttpSession session = request.getSession(false);
        if (session == null || !CSRF.verifyTokenAjax(session, request.getHeader("X-CSRF-Token")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid CSRF token");
    }
}
