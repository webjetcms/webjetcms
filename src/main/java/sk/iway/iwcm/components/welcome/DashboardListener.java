package sk.iway.iwcm.components.welcome;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.ui.ModelMap;

import sk.iway.iwcm.*;
import sk.iway.iwcm.admin.ThymeleafEvent;
import sk.iway.iwcm.admin.layout.AuditDto;
import sk.iway.iwcm.admin.layout.DocDetailsDto;
import sk.iway.iwcm.admin.layout.UserDto;
import sk.iway.iwcm.common.AdminTools;
import sk.iway.iwcm.components.todo.ToDoBean;
import sk.iway.iwcm.components.todo.ToDoDB;
import sk.iway.iwcm.components.users.userdetail.UserDetailsRepository;
import sk.iway.iwcm.doc.DebugTimer;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.stat.SessionClusterService;
import sk.iway.iwcm.stat.SessionDetails;
import sk.iway.iwcm.stat.SessionHolder;
import sk.iway.iwcm.system.ntlm.AuthenticationFilter;
import sk.iway.iwcm.system.spring.events.WebjetEvent;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

/**
 * Doplna data pre admin cast, pocuva na URL /admin/v9/dashboard/
 */
@Component
public class DashboardListener {

    @Autowired
    UserDetailsRepository userDetailsRepository;

    /**
     * Pripravi data pre overview/welcome obrazovku, zatial taketo skarede natvrdo
     * riesenie
     *
     * @param event
     */
    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.admin.ThymeleafEvent' && event.source.page=='dashboard'")
    protected void setOverviewData(final WebjetEvent<ThymeleafEvent> event) {
        try {
            DebugTimer dt = new DebugTimer("DashboardListener");

            ModelMap model = event.getSource().getModel();
            HttpServletRequest request = event.getSource().getRequest();
            Identity user = UsersDB.getCurrentUser(request);
            Prop prop = Prop.getInstance(request);

            // backdata
            WelcomeDataBean backData = WelcomeDataBackTime.getWelcomeDataBackTime();
            model.addAttribute("overviewBackData", JsonTools.objectToJSON(backData));
            dt.diff("After backData");

            // prihlaseny admini
            List<UserDto> admins = new ArrayList<>();
            if (user.isEnabledItem("welcomeShowLoggedAdmins")) {
                Set<Integer> allreadyAddedUserIds = new HashSet<>();
                List<SessionDetails> sessions = SessionHolder.getInstance().getList();
                for (SessionDetails session : sessions) {
                    if (session.getLoggedUserId() > 0 && session.isAdmin()) {
                        Integer userId = Integer.valueOf(session.getLoggedUserId());
                        if (allreadyAddedUserIds.contains(userId))
                            continue;

                        UserDetails u = UsersDB.getUser(session.getLoggedUserId());
                        if (u != null) {
                            UserDto admin = new UserDto(u);
                            //clear admin settings to reduce data size
                            admin.setAdminSettings(null);
                            admins.add(admin);
                            allreadyAddedUserIds.add(userId);
                        }
                    }
                }
            }
            //data for webjet-overview-dashboard-mini-app-users.vue
            model.addAttribute("overviewAdmins", JsonTools.objectToJSON(admins));
            dt.diff("After admins");
            model.addAttribute("overviewCurrentSessions", SessionClusterService.getSessionInfo(request.getSession().getId(), user.getUserId()));
            dt.diff("After currentSessions");

            int size = Constants.getInt("dashboardRecentSize");

            // posledne stranky
            List<DocDetails> recentPages = AdminTools.getMyRecentPages(user, size);
            List<DocDetailsDto> recentPagesDto = recentPages.stream().map(DocDetailsDto::new)
                    .collect(Collectors.toList());
            model.addAttribute("overviewRecentPages", JsonTools.objectToJSON(recentPagesDto));
            dt.diff("After recent pages");

            // zmenene stranky
            List<DocDetails> changedPages = AdminTools.getRecentPages(40, user);
            List<DocDetailsDto> changedPagesDto = changedPages.stream().limit(size).map(DocDetailsDto::new)
                    .collect(Collectors.toList());
            model.addAttribute("overviewChangedPages", JsonTools.objectToJSON(changedPagesDto));
            dt.diff("After changed pages");

            // audit
            List<AdminlogBean> adminlog = Adminlog.getLastEvents(size);
            List<AuditDto> adminlogDto;
            if (user.isEnabledItem("cmp_adminlog")) adminlogDto = adminlog.stream().map(AuditDto::new).collect(Collectors.toList());
            else adminlogDto = new ArrayList<>();
            model.addAttribute("overviewAdminlog", JsonTools.objectToJSON(adminlogDto));
            dt.diff("After adminlog");

            // to do
            List<ToDoBean> todos = (new ToDoDB()).getToDo(user.getUserId());
            model.addAttribute("overviewTodo", JsonTools.objectToJSON(todos));
            dt.diff("After todo");

            //ak existuje subor /WEB-INF/update/error-log.txt tak zobrazime link nanho
            IwcmFile logFile = new IwcmFile(Tools.getRealPath("/WEB-INF/update/error-log.txt"));
            boolean showErrorLog = false;
            if(logFile.exists())
            {
                IwcmFile tmpLogFile = new IwcmFile(Tools.getRealPath("/files/protected/admin/error-log.txt"));
                FileTools.copyFile(logFile, tmpLogFile);
                showErrorLog = true;
            }
            model.addAttribute("showErrorLog", showErrorLog);
            dt.diff("After error log");

            //check minimal java version
            int requiredJavaVersion = Constants.getInt("javaMinimalVersion");
            String currentJavaVersion = System.getProperty("java.version");
            if ("tester".equals(user.getLogin()) && request.getParameter("javaVersion") != null) {
                currentJavaVersion = request.getParameter("javaVersion");
            }
            if (Tools.isNotEmpty(currentJavaVersion) && requiredJavaVersion > 0) {
                int i = currentJavaVersion.indexOf(".");
                if (i > 0) {
                    int currentJavaVersionMajor = Tools.getIntValue(currentJavaVersion.substring(0, i), -1);
                    if (currentJavaVersionMajor > 0 && requiredJavaVersion > currentJavaVersionMajor) {
                        String message = prop.getText("system.javaVersionWarningText", ""+requiredJavaVersion, currentJavaVersion);
                        model.addAttribute("javaVersionWarningText", message);
                    }
                }
            }

            //Warning to license expiration date coming in 2 months
            Long expirationDate = Constants.getLong("licenseExpiryDate");
            if(expirationDate > 0L) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.MONTH, 2);
                if(cal.getTimeInMillis() >= expirationDate)
                {
                    String message = prop.getText("overview.license.expirationWarning", Tools.formatDate(expirationDate));
                    model.addAttribute("licenceExpirationWarningText", message);
                }
            }

            if (Constants.getBoolean("useAmazonSES")) {
                String message = prop.getText("overview.useAmazonSES.deprecated");
                model.addAttribute("amazonSesWarningText", message);
            }

            boolean show2FARecommendation = false;
            if (Constants.getBoolean("2factorAuthEnabled")) {
                String overview2fawarning = prop.getText("overview.2fa.warning");
                if (Tools.isEmpty(Constants.getString("ldapProviderUrl")) && Tools.isEmpty(Constants.getString("adminLogonMethod")) && AuthenticationFilter.weTrustIIS()==false && Tools.isNotEmpty(overview2fawarning) && overview2fawarning.length() > 2) {
                    String mobileDevice = userDetailsRepository.getMobileDeviceByUserId((long)user.getUserId());
                    if (Tools.isEmpty(mobileDevice)) {
                        show2FARecommendation = true;
                    }
                }
            }
            model.addAttribute("show2FARecommendation", show2FARecommendation);

        } catch (JsonProcessingException e) {
            Logger.error(DashboardListener.class, e);
        }
    }
}
