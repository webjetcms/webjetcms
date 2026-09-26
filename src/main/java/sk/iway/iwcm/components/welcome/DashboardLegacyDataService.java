package sk.iway.iwcm.components.welcome;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.admin.layout.AuditDto;
import sk.iway.iwcm.admin.layout.DocDetailsDto;
import sk.iway.iwcm.admin.layout.UserDto;
import sk.iway.iwcm.common.AdminTools;
import sk.iway.iwcm.stat.SessionDetails;
import sk.iway.iwcm.stat.SessionHolder;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

/** Retains the optional legacy overview without loading it on every dashboard visit. */
@Service
public class DashboardLegacyDataService {
    public Map<String, Object> load(Identity user, String domain, int rootGroupId) {
        if (user == null || !user.isAdmin()) throw new AccessDeniedException("Administrator login is required");
        int size = Math.max(1, Math.min(20, Constants.getInt("dashboardRecentSize")));
        List<DocDetailsDto> pages = user.isEnabledItem("menuWebpages")
            ? AdminTools.getRecentPages(40, rootGroupId, user).stream()
                .filter(page -> DashboardRecentPagesService.isAccessible(page, user, domain))
                .limit(size).map(DocDetailsDto::new).toList()
            : List.of();
        List<AuditDto> audit = user.isEnabledItem("cmp_adminlog")
            ? Adminlog.getLastEvents(size).stream().map(AuditDto::new).toList() : List.of();
        List<UserDto> admins = new ArrayList<>();
        if (user.isEnabledItem("welcomeShowLoggedAdmins")) {
            Set<Integer> visited = new HashSet<>();
            for (SessionDetails session : SessionHolder.getInstance().getList()) {
                if (session.getLoggedUserId() <= 0 || !session.isAdmin() || !visited.add(session.getLoggedUserId())) continue;
                UserDetails active = UsersDB.getUser(session.getLoggedUserId());
                if (active == null) continue;
                UserDto admin = new UserDto(active);
                admin.setAdminSettings(null);
                admins.add(admin);
            }
        }
        return Map.of("changedPages", pages, "adminLog", audit, "admins", admins);
    }
}
