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

    public Identity requireUser(HttpServletRequest request) {
        if (!Constants.getBoolean("branchesEnabled"))
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
        String domain = selected instanceof String value && !value.isBlank() ? value : Tools.getServerName(request);
        if (domain == null || domain.isBlank()
                || !GroupsDB.getInstance().getUserRootDomainNames(user).contains(domain))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No access to the selected domain");
        return domain;
    }

    public void requireExpectedDomain(String actual, String expected) {
        if (!actual.equals(expected))
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
