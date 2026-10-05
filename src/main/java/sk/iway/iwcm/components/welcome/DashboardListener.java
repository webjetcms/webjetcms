package sk.iway.iwcm.components.welcome;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.JsonTools;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.admin.ThymeleafEvent;
import sk.iway.iwcm.admin.layout.MenuService;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.stat.SessionClusterService;
import sk.iway.iwcm.system.spring.events.WebjetEvent;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.iwcm.users.devices.AdminDeviceService;

/** Supplies lightweight initial data while expensive widget previews load independently. */
@Component
public class DashboardListener {
    private final DashboardSettingsService settingsService;
    private final DashboardNoticeService noticeService;
    private final AdminDeviceService deviceService;

    public DashboardListener(DashboardSettingsService settingsService, DashboardNoticeService noticeService, AdminDeviceService deviceService) {
        this.settingsService = settingsService;
        this.noticeService = noticeService;
        this.deviceService = deviceService;
    }

    /** Embeds account settings, system notices and current sessions in the dashboard template. */
    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.admin.ThymeleafEvent' && event.source.page=='dashboard'")
    protected void setOverviewData(final WebjetEvent<ThymeleafEvent> event) {
        HttpServletRequest request = event.getSource().getRequest();
        Identity user = UsersDB.getCurrentUser(request);
        if (user == null || !user.isAdmin()) return;
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("dashboardMenu", new MenuService(request).getMenu());
            data.put("userName", user.getFullName());
            data.put("currentDomain", DocDB.getDomain(request));

            //use rootGroup only in multiweb, because its hard to use all groupIds in a whole domain, we need to update stat tables to use domain_id sometimes
            if (InitServlet.isTypeCloud()) data.put("statRootGroupId", CloudToolsForCore.getRootGroupId(request));
            else data.put("statRootGroupId", -1);

            data.put("settings", settingsService.load(user.getUserId(), DashboardRestController.domainKey(request)));
            data.put("notices", noticeService.load(user, request));
            boolean securityEventRequested = request.getParameter("securityEvent") != null;
            data.put("securityEventRequested", securityEventRequested);
            if (securityEventRequested) {
                data.put("requestedSecurityEvent", null);
                try {
                    data.put("requestedSecurityEvent", deviceService.findEvent(user, request.getParameter("securityEvent")));
                } catch (IllegalStateException exception) {
                    Logger.error(DashboardListener.class, "Cannot load requested administrator login event (" + exception.getClass().getSimpleName() + ")");
                }
            }
            data.put("currentSessions", new ObjectMapper().readTree(SessionClusterService.getSessionInfo(request.getSession().getId(), user.getUserId())));
            event.getSource().getModel().addAttribute("overviewData", JsonTools.objectToJSON(data));
        } catch (JsonProcessingException exception) {
            Logger.error(DashboardListener.class, exception);
        }
    }

}
