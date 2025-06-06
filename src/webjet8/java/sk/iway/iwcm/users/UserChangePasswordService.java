package sk.iway.iwcm.users;

import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.ui.ModelMap;

import sk.iway.Password;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.AdminlogBean;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.SpamProtection;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.users.userdetail.UserDetailsEntity;
import sk.iway.iwcm.components.users.userdetail.UserDetailsRepository;
import sk.iway.iwcm.components.users.userdetail.UserDetailsService;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.helpers.MailHelper;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.logon.UserForm;
import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.tags.support_logic.ResponseUtils;

public class UserChangePasswordService {

    public static final String LOGINS_SEPARATOR = ",";
    private static final String CANCEL_ACTION = "cancelChangePasswordAction";

    private UserChangePasswordService() {
        //utility class
    }

    /**
     * Handle request for password change. If all is ok, send email with link to change password page.
     *
     * @param request
     * @param login
     * @return
     */
	public static boolean sendPassword(HttpServletRequest request, String login) {
		if (SpamProtection.canPost("passwordSend", null, request) == false) {
			Prop prop = Prop.getInstance(request);

			Logger.error(UsersDB.class, prop.getText("logon.error.blocked"));
			request.setAttribute("errors", prop.getText("logon.error.blocked"));
			request.setAttribute("error.logon.user.blocked", "true");
			return false;
		}

        //IF is set, use custom method
		String method = Constants.getString("sendPasswordMethod");
		if (Tools.isNotEmpty(method)) {
			int i = method.lastIndexOf('.');
			String clazz = method.substring(0, i);
			method = method.substring(i+1);
			//String
			try {
				Class<?> c = Class.forName(clazz);
				Object o = c.getDeclaredConstructor().newInstance();
				Method m;
				Class<?>[] parameterTypes = new Class<?>[] {HttpServletRequest.class, String.class, String.class};
				Object[] arguments = new Object[] {request, login, login};
				m = c.getMethod(method, parameterTypes);
				return((boolean)m.invoke(o, arguments));
			}
			catch (Exception ex) {
				sk.iway.iwcm.Logger.error(ex);
				return false;
			}
		}

        List<UserDetailsEntity> suitableUsers = getAllSuitableLogins(login, isAdminSection(request));

        //Is there suitable user?
        if(suitableUsers.size() == 0) {
            Logger.println(UserChangePasswordService.class, "Suitable user NOT found :" + login);

            if (Constants.getBoolean("formLoginProtect")) {
                //nastavime na uspech aj ked user neexistuje aby sa to nedalo rozlisit
                request.setAttribute("passResultEmail", "@");
            } else {
                Logger.println(UserChangePasswordService.class,"K zadanemu uzivatelovi neexistuje email");
                request.setAttribute("passResultEmailFail","true");
            }

            return false;
        } else {
            //Its ok send email
            try{
                sendPasswordEmail(request, suitableUsers);
                return true;
            } catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
        }

        return false;
	}

    private static boolean isAdminSection(HttpServletRequest request) {
        if(request == null) return false;
        Object isAdminSectionObj = request.getAttribute(UsersDB.IS_ADMIN_SECTION_KEY);
        if (isAdminSectionObj == null) {
            return false;
        }
        if (isAdminSectionObj instanceof Boolean) {
           return (Boolean) isAdminSectionObj;
        }
        return false;
    }

    /**
     * Get all suitable logins for change password action.
     * Users MUST be admin, not disabled, by in good domain if its required.
     *
     * @param login
     * @return
     */
    private static List<UserDetailsEntity> getAllSuitableLogins(String loginOrEmail, boolean onlyAdmins) {
        List<UserDetailsEntity> suitableUsers = new ArrayList<>();
        UserDetailsRepository udr = Tools.getSpringBean("userDetailsRepository", UserDetailsRepository.class);

        //Find only ADMIN users -> non admin cant login in
        if (loginOrEmail != null && loginOrEmail.contains("@")) {
            //Entered EMAIL

            if(onlyAdmins == true)
                suitableUsers.addAll( udr.findAllByEmailAndAdminTrueOrderByIdDesc(DB.fixAiCiValue(loginOrEmail)) );
            else
                suitableUsers.addAll( udr.findAllByEmailOrderByIdDesc(DB.fixAiCiValue(loginOrEmail)) );
        } else {
            //Entered LOGIN

            if(onlyAdmins == true)
                suitableUsers.addAll( udr.findByLoginAndAdminTrueOrderByIdDesc(DB.fixAiCiValue(loginOrEmail)) );
            else
                suitableUsers.addAll( udr.findByLoginOrderByIdDesc(DB.fixAiCiValue(loginOrEmail)) );
        }

        //Filter user by DOMAIN ... IF it's needed
        if (UserDetailsService.isUsersSplitByDomain()) {
            int domainId = CloudToolsForCore.getDomainId();

            suitableUsers = suitableUsers.stream()
                .filter(user -> user.getDomainId() == domainId)
                .collect(Collectors.toList());
        }

        //Filter disabled users out
        suitableUsers = suitableUsers.stream()
            .filter(user -> UserDetailsService.isUserDisabled(user) == false)
            .collect(Collectors.toList());

        //Filter out users with INVALID email
        suitableUsers = suitableUsers.stream()
            .filter(user -> Tools.isEmail(user.getEmail()) == true)
            .collect(Collectors.toList());

        return suitableUsers;
    }

    /**
     * Send email with link to change password page.
     *
     * @param request
     * @param users
     * @throws Exception
     */
    private static void sendPasswordEmail(HttpServletRequest request, List<UserDetailsEntity> users) throws Exception {
        UserDetailsEntity newestUser = users.get(0);

		Prop prop = Prop.getInstance(Constants.getServletContext(), request);
		//if we are able to decrypt his/her original password
		String subject = prop.getText("logon.mail.lost_password") + " " + Tools.getBaseHref(request);
		if (!Constants.getBoolean("passwordUseHash"))
		{
			String text;
			text = prop.getText("logon.mail.message") + "\n";
			text += prop.getText("logon.mail.login_name") + ": " + newestUser.getLogin() + "\n";
			text += prop.getText("logon.mail.password") + ": " + newestUser.getPassword() + "\n";
			// from fromEmail toEmail subject text
			SendMail.send(newestUser.getFullName(), newestUser.getEmail(), newestUser.getEmail(), subject, text);
		} else {

            String allLogins = users.stream()
                .map(user -> user.getLogin())
                .collect(Collectors.joining(","));

			int randomNumber = new SecureRandom().nextInt();
			String loginHash = new Password().encrypt( allLogins );
			String auth = new Password().encrypt(Integer.toString(randomNumber));
			Adminlog.add(Adminlog.TYPE_USER_CHANGE_PASSWORD, newestUser.getId().intValue(), "Vyžiadanie zmeny hesla", randomNumber, UsersDB.APPROVE_APPROVE);
			//String text = prop.getText("logon.password.change_at")+"\n";

            // pageUrl is set depending if its request from admin section or not
			String pageUrl = isAdminSection(request) == true ? Constants.getString("changePasswordPageUrlAdmin") : Constants.getString("changePasswordPageUrl");

			if (request !=null && request.getAttribute("sendPasswordUrl") != null) pageUrl = (String)request.getAttribute("sendPasswordUrl");

            String currentLng = (request != null) ? Prop.getLng(request, false) : Constants.getString("defaultLanguage");

			pageUrl = Tools.getBaseHref(request) + pageUrl + "?language=" + currentLng + "&login="+loginHash+"&auth="+auth;

            String cancelActionLink = pageUrl + "&act=" + CANCEL_ACTION;

			String propKey = Tools.getRequestAttribute(request,  "sendPasswordTextKey", "logon.password.changeEmailText");
			String subjectKey = Tools.getRequestAttribute(request,  "sendPasswordSubjectKey", null);
			if (subjectKey != null)
			{
				subject = prop.getText(subjectKey, Tools.getBaseHref(request), DocDB.getDomain(request));
			}

			String fromName = Tools.getRequestAttribute(request, "sendPasswordFromName", newestUser.getFullName());
			String fromEmail = Tools.getRequestAttribute(request, "sendPasswordFromEmail", newestUser.getEmail());

			String text = prop.getText(propKey, pageUrl, String.valueOf(Constants.getInt("passwordResetValidityInMinutes")), cancelActionLink);

			new MailHelper().
				setFromEmail(fromEmail).
				setFromName(fromName).
				addRecipient(newestUser.getEmail()).
				setSubject(subject).
				setMessage(text).
				send();
		}
		if (request!=null) request.setAttribute("passResultEmail", newestUser.getEmail());
	}

    public static AdminlogBean getChangePasswordAdminlogBean(String login, String auth) {
        UserDetails user = UsersDB.getUser(login);

		return new ComplexQuery().
			setSql("SELECT * FROM "+ConfDB.ADMINLOG_TABLE_NAME+" WHERE log_type=? AND user_id = ? AND sub_id1 = ?").
			setParams(Adminlog.TYPE_USER_CHANGE_PASSWORD, user.getUserId(), Integer.valueOf(auth)).
		 	singleResult(new Mapper<AdminlogBean>(){;
				public AdminlogBean map(ResultSet rs) throws SQLException{
					return new AdminlogBean(rs);
				}
		});
    }

    /**
     * Verify if received logins are valid. If YES, return true, otherwise false.
     *
     * @param receivedLogins
     * @param selectedLogin
     * @param auth
     * @param request
     * @return
     */
    public static boolean verifyLoginValue(String receivedLogins, String selectedLogin, String auth, HttpServletRequest request) {
        //Check if custom login is implemented , if YES dont do a check
        String method = Constants.getString("sendPasswordMethod");
        if(Tools.isEmpty(method) == false) return true;

        if(Tools.isEmpty(receivedLogins) || Tools.isEmpty(selectedLogin) || Tools.isEmpty(auth)) return false;

        //There can be multiple logins separated by LOGINS_SEPARATOR
        String[] logins = receivedLogins.split(LOGINS_SEPARATOR);
        if(logins.length == 0) return false;
        else if(logins.length == 1) {
            //We have only one option -> that one option MUST be in adminlog
            AdminlogBean log = getChangePasswordAdminlogBean(logins[0], auth);
            //If AdminlogBean was returned -> it means login is valid, and we can change password
            if(log != null) return true;
        } else {
            //We have multiple options
            //Use FIRST login to get AdminlogBean
            AdminlogBean log = getChangePasswordAdminlogBean(logins[0], auth);
            if(log != null) {
                //AdminlogBean was found -> FIRST login is valid, FOUND all logins by email
                UserDetails user = UsersDB.getUser(logins[0]);
                List<UserDetailsEntity> users = getAllSuitableLogins(user.getEmail(), isAdminSection(request));
                //Check if selected login is in list of valid logins
                return users.stream().anyMatch(u -> u.getLogin().equals(selectedLogin));
            } else {
                //AdminlogBean was not found -> probably wrong received logins
                return false;
            }
        }

        return true;
    }

    /**
     * Delete adminlog record for change password action.
     * @param loginsStr
     * @param auth
     */
    public static void deleteChangePasswordAdminlogBean(String loginsStr, String auth) {
        if(Tools.isEmpty(loginsStr) || Tools.isEmpty(auth)) return;

        //There can be multiple logins separated by LOGINS_SEPARATOR
        String[] logins = loginsStr.split(LOGINS_SEPARATOR);
        if(logins.length == 0) return;

        UserDetails user = UsersDB.getUser(logins[0]);
        deleteChangePasswordAdminlogBean(user, auth);
    }

    /**
     * Delete adminlog record for change password action.
     *
     * @param user
     * @param auth
     */
    public static void deleteChangePasswordAdminlogBean(UserDetails user, String auth) {
        //zmaz zaznam z audit tabulky (aby druhy krat linka nefungovala)
        new SimpleQuery().execute("DELETE FROM " + ConfDB.ADMINLOG_TABLE_NAME + " WHERE log_type=? AND user_id=? AND sub_id1=?", Adminlog.TYPE_USER_CHANGE_PASSWORD, user.getUserId(), Tools.getIntValue(auth, -1));
    }

    /**
     * Call {@link #getPreparedUserForm(HttpServletRequest, ModelMap)} with model as NULL -> CALLED from JSP
     * @param request
     * @return
     */
    public static UserForm getPreparedUserForm(HttpServletRequest request) {
        return getPreparedUserForm(request, null);
    }

    /**
     * Prepare use form for change password action. IF all is ok, return UserForm object, otherwise NULL.
     * If problem occurs, set attribute to model to show error message.
     * @param request
     * @param model
     * @return
     */
    public static UserForm getPreparedUserForm(HttpServletRequest request, ModelMap model) {
        String auth = request.getParameter("auth");
        String login = request.getParameter("login");
        String action = request.getParameter("act");

        UserForm userForm = null;

        try {
            login = new Password().decrypt(login);
            auth = new Password().decrypt(auth);

            //Login handle
			login = ResponseUtils.filter(login);

            //Login CAN BE combination of more logins separated by LOGINS_SEPARATOR
			String[] logins = login.split(LOGINS_SEPARATOR);

            //Its important use the newest login (position 0), it match created AdminlogBean
			AdminlogBean log = getChangePasswordAdminlogBean(logins[0], auth);

            //Not found = not valid
            if(log == null) {
                // Show err msg
                if(model != null) model.addAttribute("changePasswordActionFailed", true);
                return userForm;
            }

            // FIRST check if this is cancel action
            if(CANCEL_ACTION.equals(action)) {
                //Delete adminlog record
                deleteChangePasswordAdminlogBean(logins[0], auth);

                //Set param to show message
                if(model != null) model.addAttribute(CANCEL_ACTION, true);

                //Return empty userForm
                return userForm;
            }

            long timeAskedFor = log.getCreateDate().getTime();
			long timeNow = System.currentTimeMillis();
			long validity = Constants.getInt("passwordResetValidityInMinutes")*60L*1000L;

			if (timeNow - timeAskedFor > validity) {
                // Show err msg
                if(model != null) model.addAttribute("changePasswordActionFailed", true);

                //No more valid
                return userForm;
            }

            //Valid
            userForm = new UserForm();
            userForm.setLogin(login);
            userForm.setAuth(auth);
            userForm.setSelectedLogin( logins[0] );

            return userForm;
        } catch (Exception e) {
            sk.iway.iwcm.Logger.error(e);

            // Show err msg
            if(model != null) model.addAttribute("changePasswordActionFailed", true);

            return userForm;
        }
    }
}
