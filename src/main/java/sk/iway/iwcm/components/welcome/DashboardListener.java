package sk.iway.iwcm.components.welcome;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.JsonTools;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.admin.ThymeleafEvent;
import sk.iway.iwcm.admin.layout.MenuService;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.stat.SessionClusterService;
import sk.iway.iwcm.stat.SessionDetails;
import sk.iway.iwcm.stat.SessionHolder;
import sk.iway.iwcm.system.spring.events.WebjetEvent;
import sk.iway.iwcm.users.UsersDB;

/** Supplies lightweight initial data while expensive widget previews load independently. */
@Component
public class DashboardListener {
    private final DashboardSettingsService settingsService;
    private final DashboardNoticeService noticeService;

    public DashboardListener(DashboardSettingsService settingsService, DashboardNoticeService noticeService) {
        this.settingsService = settingsService;
        this.noticeService = noticeService;
    }

    /** Embeds account settings, system notices, current sessions and authorized administrator names in the dashboard template. */
    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.admin.ThymeleafEvent' && event.source.page=='dashboard'")
    protected void setOverviewData(final WebjetEvent<ThymeleafEvent> event) {
        HttpServletRequest request = event.getSource().getRequest();
        Identity user = UsersDB.getCurrentUser(request);
        if (user == null || !user.isAdmin()) return;
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("dashboardMenu", new MenuService(request).getMenu());
            data.put("userName", user.getFirstName());
            data.put("currentDomain", DocDB.getDomain(request));
            data.put("settings", settingsService.load(user.getUserId(), DashboardRestController.domainKey(request)));
            data.put("notices", noticeService.load(user, request));
            data.put("currentSessions", new ObjectMapper().readTree(SessionClusterService.getSessionInfo(request.getSession().getId(), user.getUserId())));
            if (user.isEnabledItem("welcomeShowLoggedAdmins")) data.put("loggedAdmins", loggedAdmins());
            event.getSource().getModel().addAttribute("overviewData", JsonTools.objectToJSON(data));
        } catch (JsonProcessingException exception) {
            Logger.error(DashboardListener.class, exception);
        }
    }

    /** Shows each currently logged-in administrator once without exposing their account or session DTO. */
    private List<Map<String, Object>> loggedAdmins() {
        Set<Integer> visited = new LinkedHashSet<>();
        List<Map<String, Object>> items = new ArrayList<>();
        for (SessionDetails session : SessionHolder.getInstance().getList()) {
            int id = session.getLoggedUserId();
            if (id <= 0 || !session.isAdmin() || !visited.add(id)) continue;
            var user = UsersDB.getUserCached(id);
            if (user == null || !user.isAdmin()) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", id);
            item.put("fullName", user.getFullName());
            item.put("email", user.getEmail());
            items.add(item);
        }
        items.sort(Comparator.comparing(item -> String.valueOf(item.get("fullName")), String.CASE_INSENSITIVE_ORDER));
        return items;
    }
}
