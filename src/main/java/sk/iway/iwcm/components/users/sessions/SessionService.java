package sk.iway.iwcm.components.users.sessions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.stat.SessionClusterService;
import sk.iway.iwcm.stat.SessionDetails;
import sk.iway.iwcm.stat.SessionHolder;
import sk.iway.iwcm.users.UsersDB;

/** Authorizes session operations and builds safe administrator summaries from local and cluster data. */
@Service
public class SessionService {
    /**
     * Lists administrators visible in the active domain, with distinct sessions across cluster nodes.
     * The caller's list permission is independent of the permission to end another administrator's sessions.
     *
     * @param actor authenticated administrator requesting the list
     * @return alphabetically ordered administrator summaries without session IDs
     */
    public List<LoggedAdministratorDto> loggedAdministrators(Identity actor) {
        requirePermission(actor, "welcomeShowLoggedAdmins");
        // getList applies the existing cloud/external-directory domain filter. Only these accounts may be expanded from cluster data.
        List<SessionDetails> localSessions = SessionHolder.getInstance().getList();
        Map<Integer, LoggedAdministratorDto> administrators = new LinkedHashMap<>();
        for (SessionDetails session : localSessions) {
            int id = session.getLoggedUserId();
            if (id <= 0 || !activeAdminSession(session) || administrators.containsKey(id)) continue;
            var user = UsersDB.getUserCached(id);
            // The cached account must still be an administrator; stale session flags alone do not grant visibility.
            if (user == null || !user.isAdmin()) continue;
            var item = new LoggedAdministratorDto();
            item.setUserId(id);
            item.setFullName(user.getFullName());
            item.setEmail(user.getEmail());
            item.setLogin(user.getLogin());
            item.setCurrent(id == actor.getUserId());
            administrators.put(id, item);
        }
        Map<String, SessionDetails> sessionsById = new LinkedHashMap<>();
        try {
            for (SessionDetails session : SessionClusterService.getSessionsForUsers(administrators.keySet()))
                sessionsById.put(session.getSessionId(), session);
        } catch (Exception exception) {
            // Local sessions remain useful if a cluster snapshot cannot be read.
            Logger.error(SessionService.class, "Could not read cluster session summaries", exception);
        }
        // Cluster snapshots also include this node. Deduplicate by session ID and prefer the fresher local activity/metadata.
        for (SessionDetails session : localSessions) {
            if (administrators.containsKey(session.getLoggedUserId()) && activeAdminSession(session))
                sessionsById.put(session.getSessionId(), session);
        }
        for (LoggedAdministratorDto item : administrators.values()) {
            List<SessionDetails> sessions = sessionsById.values().stream().filter(session -> session.getLoggedUserId() == item.getUserId()).toList();
            item.setSessionCount(sessions.size());
            item.setLastActivity(sessions.stream().mapToLong(SessionDetails::getLastActivity).max().orElse(0));
            item.setClients(sessions.stream().map(SessionService::sessionClient).distinct().toList());
        }
        List<LoggedAdministratorDto> result = new ArrayList<>(administrators.values());
        result.sort(Comparator.comparing(LoggedAdministratorDto::getFullName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    /**
     * Ends only an owned session other than the one making this request.
     * Remote invalidation is queued only after the cluster snapshot confirms both the ID and owner.
     *
     * @param actor authenticated administrator
     * @param currentSessionId session making the request, which must remain active
     * @param sessionId owned session to end
     * @return rejected, completed or accepted with cluster propagation pending
     */
    public SessionLogoutResultDto logoutSession(Identity actor, String currentSessionId, String sessionId) {
        requireAdmin(actor);
        if (Tools.isEmpty(sessionId) || sessionId.equals(currentSessionId)) return new SessionLogoutResultDto(false, false);
        SessionHolder holder = SessionHolder.getInstance();
        SessionDetails local = holder.get(sessionId);
        if (local != null) {
            boolean success = local.getLoggedUserId() == actor.getUserId() && holder.invalidateSession(actor.getUserId(), sessionId);
            return new SessionLogoutResultDto(success, false);
        }
        for (var node : SessionClusterService.getUserSessionsAllNodes(actor.getUserId())) {
            for (var session : node.path("userSessions")) {
                if (sessionId.equals(session.path("sessionId").asText()) && session.path("loggedUserId").asInt() == actor.getUserId()) {
                    holder.invalidateSession(actor.getUserId(), sessionId);
                    return new SessionLogoutResultDto(true, true);
                }
            }
        }
        return new SessionLogoutResultDto(false, false);
    }

    /**
     * Ends all sessions of another administrator visible in the active domain and audits the acting account.
     *
     * @param actor administrator with users.edit_admins permission; list permission is not required
     * @param userId other administrator whose sessions should end
     * @return accepted logout and whether remote invalidation remains pending
     */
    public SessionLogoutResultDto logoutAdministrator(Identity actor, int userId) {
        requirePermission(actor, "users.edit_admins");
        if (userId <= 0 || userId == actor.getUserId()) throw new IllegalArgumentException("Select another administrator");
        SessionHolder holder = SessionHolder.getInstance();
        List<SessionDetails> local = holder.getList().stream().filter(session -> session.getLoggedUserId() == userId && activeAdminSession(session)).toList();
        var target = UsersDB.getUserCached(userId);
        if (local.isEmpty() || target == null || !target.isAdmin()) throw new AccessDeniedException("Administrator is not visible in this domain");
        Set<String> sessionIds = new LinkedHashSet<>();
        for (SessionDetails session : local) sessionIds.add(session.getSessionId());
        for (SessionDetails session : SessionClusterService.getSessionsForUsers(Set.of(userId))) sessionIds.add(session.getSessionId());
        boolean pending = false;
        for (String sessionId : sessionIds) {
            if (sessionId == null || sessionId.isBlank()) continue;
            if (!holder.invalidateSession(userId, sessionId)) pending = true;
        }
        Adminlog.add(Adminlog.TYPE_USER_LOGOFF, "Administrator " + actor.getLogin() + " ended all sessions of administrator " + target.getLogin(), userId, -1);
        return new SessionLogoutResultDto(true, pending);
    }

    private static boolean activeAdminSession(SessionDetails session) {
        return session.isAdmin() && !"INVALIDATE".equals(session.getRemoteAddr());
    }

    private static void requireAdmin(Identity actor) {
        if (actor == null || !actor.isAdmin()) throw new AccessDeniedException("Administrator login is required");
    }

    private static void requirePermission(Identity actor, String permission) {
        requireAdmin(actor);
        if (!actor.isEnabledItem(permission)) throw new AccessDeniedException("Required permission: " + permission);
    }

    /** Joins only known browser and operating-system labels for the session summary. */
    private static String sessionClient(SessionDetails session) {
        String browser = session.getBrowserName();
        String system = session.getOperatingSystem();
        if (system == null || system.isBlank() || "unknown".equalsIgnoreCase(system)) return browser == null ? "" : browser;
        if (browser == null || browser.isBlank()) return system;
        return browser + " · " + system;
    }
}
