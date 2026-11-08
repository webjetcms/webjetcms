package sk.iway.iwcm.branch;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.DBPool;
import java.sql.*;

/** Session selection is only a hint; every request rechecks user, domain and package state. */
public final class BranchSelection {
    public static final String SESSION_KEY = BranchSelection.class.getName();
    public static final String ID_HEADER = "X-WebJET-Branch";
    public static final String DOMAIN_HEADER = "X-WebJET-Branch-Domain";
    private BranchSelection() { }

    public record Selected(long branchId, String domainName) implements java.io.Serializable { }

    public static BranchContext selected(HttpServletRequest request, Identity user, String domain) {
        Object value = request.getSession().getAttribute(SESSION_KEY);
        if (!(value instanceof Selected selected) || !domain.equals(selected.domainName()))
            return BranchContext.live(domain, user.getUserId());
        if (selected.branchId() == 0) return BranchContext.live(domain, user.getUserId());
        requireBranch(selected.branchId(), domain);
        return new BranchContext(selected.branchId(), domain, user.getUserId(), BranchContext.Mode.ADMIN);
    }

    public static BranchEntity requireBranch(long id, String domain) {
        try(Connection connection=DBPool.getConnection()) {BranchReadiness.verify(connection);}
        catch(SQLException|IllegalStateException ex) {throw new ResponseStatusException(HttpStatus.CONFLICT,"Branch prerequisites failed: "+ex.getMessage(),ex);}
        BranchEntity branch = new BranchService().find(id, domain).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Publishing package not found"));
        if ("MERGED".equals(branch.getState()) || "CANCELLED".equals(branch.getState()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Publishing package is closed; explicitly select production");
        return branch;
    }

    /** The gateway resolves the logical domain from server metadata, never from its query string. */
    public static String previewDomain(long id, Identity user) {
        try (Connection connection = DBPool.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "SELECT domain_name FROM branches WHERE id=?")) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
                String domain = result.getString(1);
                if (!GroupsDB.getInstance().getUserRootDomainNames(user).contains(domain))
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No access to preview domain");
                requireBranch(id, domain);
                return domain;
            }
        } catch (SQLException ex) { throw new IllegalStateException("Cannot resolve preview domain", ex); }
    }
}
