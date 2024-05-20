package sk.iway.iwcm.common;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import sk.iway.Password;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.stat.StatDB;
import sk.iway.iwcm.stripes.AfterLogonLogoffInterceptor;
import sk.iway.iwcm.system.multidomain.MultiDomainFilter;
import sk.iway.iwcm.system.spring.WebjetAuthentificationProvider;
import sk.iway.iwcm.users.PasswordSecurity;
import sk.iway.iwcm.users.PasswordsHistoryBean;
import sk.iway.iwcm.users.PermissionGroupBean;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UserGroupsDB;
import sk.iway.iwcm.users.UsersDB;

public class LogonTools {

    protected LogonTools() {
        //utility class
    }

    /**
     * Skontroluje ci sa moze pouzivatel prihlasit vzhladom na zadane datumy mozneho prihlasenia
     * @param rs
     * @return
     */
    public static boolean checkAllowLoginDates(ResultSet rs)
    {
        try
        {
            java.sql.Date start = rs.getDate("allow_login_start");
            java.sql.Date end = rs.getDate("allow_login_end");

            long startL = 0;
            long endL = Long.MAX_VALUE;
            if (start != null) startL = start.getTime();
            if (end != null) endL = end.getTime() + (60*60*24 * 1000);

            long now = Tools.getNow();

            if (now > startL && now < endL) return(true);
        }
        catch (SQLException ex)
        {
            sk.iway.iwcm.Logger.error(ex);
        }
        return(false);
    }

    /**
     *  Description of the Method
     *
     *@param  username  Description of the Parameter
     *@param  password  Description of the Parameter
     *@param  user      Description of the Parameter
     *@param  errors    Description of the Parameter
     *@param  request   Description of the Parameter
     *@return           Description of the Return Value
     */
    public static String logon(String username, String password, Identity user, Map<String, String> errors, HttpServletRequest request, sk.iway.iwcm.i18n.Prop prop)
    {
        String forward = "logon_ok_admin";

        if (LogonTools.isLoginBlocked(request)) {
            request.setAttribute("logon.error.blocked", "1");
            errors.put("ERROR_KEY", prop.getText("logon.error.blocked"));
            return forward;
        }

        String logonMethod = Constants.getString("adminLogonMethod");
        String tryNormalLogon = null;
        String adminUsersAllowNormalLogon = Constants.getString("adminUsersAllowNormalLogon"); //moznost pre niektore loginy sa prihlasit normalne (ak pouzivame LDAP prihlasovanie)
        if (Constants.getBoolean("adminLogonMethodAllowNormalTry")) tryNormalLogon = request.getParameter("tryNormalLogon");

        if (Tools.isNotEmpty(logonMethod) && tryNormalLogon==null && (Tools.isEmpty(adminUsersAllowNormalLogon) ||
             Tools.containsOneItem(Tools.getTokens(adminUsersAllowNormalLogon, ",|"), new String[]{username}) == false))
        {
            int i = logonMethod.lastIndexOf('.');
            String beforePostClass = logonMethod.substring(0, i);
            logonMethod = logonMethod.substring(i+1);
            //String
            try
            {
                Class<?> c = Class.forName(beforePostClass);
                Object o = c.getDeclaredConstructor().newInstance();
                Method m;
                Class<?>[] parameterTypes = new Class<?>[] {String.class, String.class, Identity.class, Map.class, HttpServletRequest.class, sk.iway.iwcm.i18n.Prop.class};
                Object[] arguments = new Object[] {username, password, user, errors, request, prop};
                m = c.getMethod(logonMethod, parameterTypes);
                return((String)m.invoke(o, arguments));
            }
            catch (Exception ex)
            {
                sk.iway.iwcm.Logger.error(ex);
            }
        }

        if (username == null || password == null)
        {
            return ("");
        }
        StringBuilder err = new StringBuilder();
        //String user_name = "";
        //String user_pass ="";

        try
        {

            Connection db_conn = DBPool.getConnection();
            try
            {
                String sql = "";

                if (request.getParameter("emailLogon") != null && "true".equalsIgnoreCase(request.getParameter("emailLogon")))
                {
                    sql = "SELECT * FROM users WHERE "+DB.fixAiCiCol("email")+"=?";
                }
                else
                {
                    sql = "SELECT * FROM  users WHERE "+DB.fixAiCiCol("login")+"=?";
                }

                sql += UsersDB.getDomainIdSqlWhere(true);

                PreparedStatement ps = db_conn.prepareStatement(sql);
                try
                {
                    ps.setString(1, DB.fixAiCiValue(username));
                    ResultSet db_result = ps.executeQuery();
                    try
                    {
                        if (db_result.next())
                        {
                            //skontroluj ci je platny datum prihlasenia
                            boolean allowDateLogin = checkAllowLoginDates(db_result);
                            if (allowDateLogin == false)
                            {
                                err.append("<li>").append(prop.getText("logon.err.dateLoginDissabled", DB.getDbDate(db_result, "allow_login_start"), DB.getDbDate(db_result, "allow_login_end")));
                                request.setAttribute("logon.err.dateLoginDissabled", "true");

                                Adminlog.add(Adminlog.TYPE_USER_LOGON, "LogonAction - login date disabled: name= " + username, -1, -1);
                            }
                            else
                            {
                                //skontroluj heslo
                                boolean passok = false;
                                String salt = db_result.getString("password_salt");
                                String passwordInDb = db_result.getString("password");
                                sk.iway.Password pass = new sk.iway.Password();
                                //spatna kompatibilita je potrebna, ak admin zmeni passwordUseHash a zabudne zavolat update_passwords.jsp
                                if (pass.encrypt(password).equals(passwordInDb) || PasswordSecurity.isPasswordCorrect(password, salt, passwordInDb))
                                {
                                    passok = true;
                                }
                                if (passok==false)
                                {
                                    //skus overit mobiletoken, pre istotu znova kontroluj header, aby sa nedal pouzit na bezne prihlasenie
                                    if (Tools.isNotEmpty(request.getHeader(Constants.getString("logonTokenHeaderName"))))
                                    {
                                        try
                                        {
                                            String apiKey = db_result.getString("api_key");
                                            if (Tools.isNotEmpty(apiKey))
                                            {
                                                //splitni, je to vo formate salt|token
                                                int i = apiKey.indexOf("|");
                                                if (i > 0)
                                                {
                                                    salt = apiKey.substring(0, i);
                                                    passwordInDb = apiKey.substring(i+1);
                                                    if (PasswordSecurity.isPasswordCorrect(password, salt, passwordInDb)) passok = true;
                                                }

                                            }
                                        } catch (Exception ex) {
                                            //
                                        }
                                    }
                                }
                                if (passok)
                                {
                                    user.setAuthorized(db_result.getBoolean("authorized"));

                                    if (user.isAuthorized())
                                    {
                                        //je to ok, mozeme ho presunut do hlavneho menu
                                        UsersDB.fillUserDetails(user, db_result);

                                        //user_name = username;
                                        if (user.isAdmin())
                                        {
                                            if(request.getAttribute("isMobileVersion") != null || "true".equals(request.getParameter("isMobileVersion")))
                                            {
                                                forward = "logon_mobile";
                                            }
                                            else
                                            {
                                                forward = "logon_ok_admin";
                                            }
                                            Adminlog.add(Adminlog.TYPE_USER_LOGON, user.getUserId(), "LogonAction - user (ADMIN) successfully loged: name=" + username , -1, -1);
                                        }
                                        else
                                        {
                                            Logger.error(LogonTools.class,"user nie je administrator");
                                            //errors.add(ActionMessages.GLOBAL_ERROR, new ActionMessage("error.logon.noadmin"));
                                            err.append("<li>").append(prop.getText("logon.err.noadmin"));
                                            request.setAttribute("logon.err.noadmin", "true");
                                            //return (mapping.findForward(forward));

                                            if (Constants.getBoolean("auditDontLogUsrlogon")==false) Adminlog.add(Adminlog.TYPE_USER_LOGON, user.getUserId(), "LogonAction - user successfully loged: name=" + username , -1, -1);
                                        }

                                        setUserPerms(user);
                                    }
                                    else
                                    {
                                        err = new StringBuilder("<li>").append(prop.getText("logon.err.userUnknown"));
                                        request.setAttribute("logon.err.userUnknown", "true");

                                        Adminlog.add(Adminlog.TYPE_USER_LOGON, "LogonAction - user not authorized: name= " + username, -1, -1);
                                    }
                                }
                                else
                                {
                                    Logger.error(LogonTools.class,"zle heslo");
                                    //ma zle heslo, napis chybu
                                    //errors.add(ActionMessages.GLOBAL_ERROR, new ActionMessage("error.logon.wrong.pass"));
                                    err.append("<li>").append(prop.getText("logon.err.wrongPass"));
                                    request.setAttribute("logon.err.wrongPass", "true");

                                    Adminlog.add(Adminlog.TYPE_USER_LOGON, "LogonAction - wrong password: name= " + username, -1, -1);
                                }
                            }

                        }
                        else
                        {
                            Logger.error(LogonTools.class,"user neexistuje");
                            //zadany user neexistuje
                            //errors.add(ActionMessages.GLOBAL_ERROR, new ActionMessage("error.logon.user.unknown"));
                            err.append("<li>").append(prop.getText("logon.err.userUnknown"));
                            request.setAttribute("logon.err.userUnknown", "true");

                            Adminlog.add(Adminlog.TYPE_USER_LOGON, "LogonAction - user unknown: name= " + username, -1, -1);
                        }
                    }
                    finally { db_result.close(); }
                }
                finally { ps.close(); }
                if (user.getUserId()>0)
                {
                    if (user.isAuthorized()==false)
                    {
                        //Logger.println(this,"JE neautorizovany!!");
                        //ak nie je autorizovany, napis iba ze user neexistuje a nezaoberaj sa heslom
                        err = new StringBuilder("<li>").append(prop.getText("logon.err.userUnknown"));
                        request.setAttribute("logon.err.userUnknown", "true");

                        Adminlog.add(Adminlog.TYPE_USER_LOGON, "LogonAction - user unknown: name= " + username, -1, -1);
                    }
                    else
                    {
                        //nacitaj pristupove prava
                        UsersDB.setDisabledItems(user);

                        ps = db_conn.prepareStatement("UPDATE  users SET last_logon=? WHERE user_id=?");
                        try
                        {
                            ps.setTimestamp(1, new Timestamp( (new java.util.Date()).getTime()));
                            ps.setInt(2, user.getUserId());
                            ps.execute();
                        }
                        finally { ps.close(); }
                    }
                }
            }
            finally { db_conn.close(); }
        }
        catch (Exception ex)
        {
            Logger.error(LogonTools.class,"LogonAction: error");
            sk.iway.iwcm.Logger.error(ex);
        }

        if (err.toString().trim().length()>1)
        {
            errors.put("ERROR_KEY", prop.getText("approveAction.err.badPass"));
            //aby sa vzdy zobrazil dialog poslat heslo
            request.setAttribute("logon.err.wrongPass", "true");

            setLoginBlocked(request);
        }

        return (forward);
    }


    /**
     * Nastavi userovi prava na adresare (editable groups a pages)
     * @param user
     */
    public static void setUserPerms(Identity user)
    {
        try
        {
            if (user.getEditablePages().length()>0 && user.getEditableGroups().length()==0)
            {
                user.setEditableGroups(Integer.toString(Constants.getInt("systemPagesMyPages")));
            }

            //#23245
            StringBuilder permWritableFolders = new StringBuilder();
            List<PermissionGroupBean> permissionGroups = UserGroupsDB.getPermissionGroupsFor(user.getUserId());
            // pre kazdu skupinu prav pridame prava
            for (PermissionGroupBean permGroup : permissionGroups)
            {
                if(Tools.isNotEmpty(permGroup.getWritableFolders()))
                {
                    if(!permGroup.getWritableFolders().startsWith(" "))
                        permWritableFolders.append(" ");

                    permWritableFolders.append(permGroup.getWritableFolders());
                }
            }
            user.setWritableFolders(user.getWritableFolders()+permWritableFolders.toString());

            //#20460 uzivatelom sa nastavia rovnake prava na subory ako su nastavene vo na web strankach
            if(Constants.getBoolean("userPermsActualPageAutomatic") && user.getEditableGroups().length() > 0 && (Tools.isNotEmpty(user.getWritableFolders()) || Constants.getBoolean("defaultDisableUpload") == true ))
            {
                StringBuilder sb = new StringBuilder(user.getWritableFolders());
                int groupId = -1;
                GroupDetails groupDetails;
                String domainAlias = "";
                for(String group_id:Tools.getTokens(user.getEditableGroups(), ","))
                {
                    groupId = Tools.getIntValue(group_id,-1);
                    groupDetails = GroupsDB.getInstance().getGroup(groupId);
                    domainAlias = "";
                    if(Tools.isNotEmpty(groupDetails.getDomainName()))
                        domainAlias = MultiDomainFilter.getDomainAlias(groupDetails.getDomainName());
                    if(Tools.isNotEmpty(domainAlias))
                        domainAlias = "/"+domainAlias; //NOSONAR

                    for(String directory : new String[]{"/images","/files"})
                    {
                        String path = Tools.replace(UploadFileTools.getPageUploadSubDir(-1, groupId, null, directory+domainAlias), "//", "/");
                        //replace dvojiteho aliasu, niekedy sa to tam tak doplni a nemam energiu hladat preco a co by sa pokazilo potom
                        if (Tools.isNotEmpty(domainAlias))
                        {
                            path = Tools.replace(path, domainAlias+domainAlias, domainAlias);
                        }

                        if (user.isFolderWritable(path)) continue;

                        if (sb.isEmpty()==false) sb.append("\n");
                        sb.append(path);
                        if(path.endsWith("/"))
                            sb.append("*");
                        else
                            sb.append("/*");
                    }
                }
                Logger.debug(LogonTools.class, "Adding WritableFolders: "+Tools.replace(sb.toString(), user.getWritableFolders(), ""));
                if (sb.isEmpty()==false) sb.append("\n");
                user.setWritableFolders(sb.toString()+permWritableFolders);
            }

            //#23245
            //pridame mu prava na grupy z user perm. skupiny
            // pre kazdu skupinu prav pridame prava
            for (PermissionGroupBean permGroup : permissionGroups) {
                if (Tools.isNotEmpty(permGroup.getEditableGroups())) {
                    if (Tools.isEmpty(user.getEditableGroups()))
                        user.setEditableGroups(permGroup.getEditableGroups());
                    else
                        user.setEditableGroups(user.getEditableGroups() + "," + permGroup.getEditableGroups());
                }
            }
            //pridame mu prava na webstranky (documents) z user perm. skupiny
            // pre kazdu skupinu prav pridame prava
            for (PermissionGroupBean permGroup : permissionGroups) {
                if (Tools.isNotEmpty(permGroup.getEditablePages())) {
                    if (Tools.isEmpty(user.getEditablePages()))
                        user.setEditablePages(permGroup.getEditablePages());
                    else
                        user.setEditablePages(user.getEditablePages() + "," + permGroup.getEditablePages());
                }
            }
        } catch (Exception ex) {

        }
    }

	public static void auditLogon(List<String> errors, Identity user, String username, HttpServletRequest request) {
		if(errors.isEmpty() && user != null)
		{
			if (Constants.getBoolean("auditDontLogUsrlogon")==false) Adminlog.add(Adminlog.TYPE_USER_LOGON, user.getUserId(), "LogonTools - user "+(user.isAdmin() ? "(ADMIN) " : " ")+"successfully loged: name=" + username , -1, -1);
			StatDB.addAdmin(request);
		}
		else
		{
			String lng = PageLng.getUserLng(request);
			Prop prop = Prop.getInstance(lng);
			StringBuffer logErrors = new StringBuffer("");
			logErrors.append(" name=").append(username).append("\n");
            Iterator<String> iterator = errors.iterator();
            while(iterator.hasNext())
			{
				String errorKey = iterator.next();
				logErrors.append(prop.getText(errorKey));
				if(iterator.hasNext())
					logErrors.append("\n");
			}
			int userId = -1;
			if (user != null) userId = user.getUserId();
			Adminlog.add(Adminlog.TYPE_USER_LOGON, userId, "LogonTools - su nejake chyby v logovacom formulari:\n"+logErrors.toString(), -1, -1);
		}
	}

    @SuppressWarnings("unchecked")
    public static List<String> logonUser(HttpServletRequest request, String username, String password)
    {
        String logonMethod = Constants.getString("usrLogonMethod");
        if (Tools.isNotEmpty(logonMethod) && request.getParameter("tryNormalLogon")==null && request.getAttribute("tryNormalLogon")==null)
        {
            int i = logonMethod.lastIndexOf('.');
            String beforePostClass = logonMethod.substring(0, i);
            logonMethod = logonMethod.substring(i+1);
            //String
            try
            {
                //Adminlog.add(Adminlog.TYPE_USER_LOGON,"Logon attempt calling "+beforePostClass+"."+logonMethod+", username: " + username,-1,-1);

                Class<?> c = Class.forName(beforePostClass);
                Object o = c.getDeclaredConstructor().newInstance();
                Method m;
                Class<?>[] parameterTypes = new Class<?>[] {String.class, String.class, HttpServletRequest.class};
                Object[] arguments = new Object[] {username, password, request};
                m = c.getMethod(logonMethod, parameterTypes);
                Object result = m.invoke(o, arguments);
                List<String> errors = null;
                if (result instanceof List) {
                    try {
                        errors = (List<String>)result;
                    } catch (Exception ex) {
                        Logger.error(LogonTools.class, ex);
                    }
                }
                if (errors == null) errors = new ArrayList<>();

				auditLogon(errors, UsersDB.getCurrentUser(request), username, request);

				return errors;
            }
            catch (Exception ex)
            {
                Adminlog.add(Adminlog.TYPE_USER_LOGON,"Logon attempt calling "+beforePostClass+"."+logonMethod+", username: " + username + " failed, error="+ex.getMessage(),-1,-1);
                sk.iway.iwcm.Logger.error(ex);
            }
        }

        List<String> errors = new ArrayList<>();
        Identity user = new Identity();
        HttpSession session = request.getSession();
		/*
		 * potrebne pre kontrolu hesla, konkretne preto, aby som vedel zistit ci je uzivatel admin,
		 * alebo nie, kedze sa v user nastavuje admin na false a to je mozne nastavit len raz
		 */
        Identity passUser = new Identity();
        try
        {

            boolean passok = false;
            Connection db_conn = DBPool.getConnection(request);
            try
            {
                String sql = "";

                if ("true".equalsIgnoreCase(request.getParameter("emailLogon")))
                {
                    sql = "SELECT * FROM  users WHERE email=?";
                }
                else
                {
                    sql = "SELECT * FROM  users WHERE login=?";
                }

                sql += UsersDB.getDomainIdSqlWhere(true);

                PreparedStatement ps = db_conn.prepareStatement(sql);
                try
                {
                    ps.setString(1, username);
                    ResultSet db_result = ps.executeQuery();
                    try
                    {
                        if (db_result.next())
                        {
                            //skontroluj ci je platny datum prihlasenia
                            boolean allowDateLogin = LogonTools.checkAllowLoginDates(db_result);
                            if (allowDateLogin == false)
                            {
                                //ma zle heslo, napis chybu
                                errors.add("error.logon.wrong.pass");

                                request.setAttribute("error.logon.wrong.dateLoginDissabled", "true");
                                request.setAttribute("error.logon.wrong.dateLoginDissabled-start", DB.getDbDate(db_result, "allow_login_start"));
                                request.setAttribute("error.logon.wrong.dateLoginDissabled-end", DB.getDbDate(db_result, "allow_login_end"));

                                Adminlog.add(Adminlog.TYPE_USER_LOGON, "LogonAction - login date disabled: name= " + username, -1, -1);
                            }
                            else
                            {
                                String passwordInDb = db_result.getString("password");
                                String salt = db_result.getString("password_salt");

                                //skontroluj heslo
                                sk.iway.Password pass = new sk.iway.Password();
                                if (pass.encrypt(password).equals(db_result.getString("password")) || PasswordSecurity.isPasswordCorrect(password, salt, passwordInDb))
                                {
                                    passok = true;

                                    UsersDB.fillUserDetails(passUser, db_result);

                                    UsersDB.fillUserDetails(user, db_result);

                                    if (Constants.getBoolean("enableAdminInWebLogon") && user.isAdmin())
                                    {
                                        //kvoli blogom - nacitanie prav

                                        //nacitaj pristupove prava - pouziva sa napr. v module blog - nie je admin (isAdmin bude false, viz nizsie), mame ale jeho prava
                                        LogonTools.setUserPerms(user);
                                        LogonTools.setUserPerms(passUser);

                                        UsersDB.setDisabledItems(user);
                                        UsersDB.setDisabledItems(passUser);
                                    }
                                    else
                                    {
                                        //aby mu nefungovalo pristup do adminu (bezpecnost)
                                        user.setAdmin(false);
                                    }
                                }
                                else
                                {
                                    Logger.error(LogonTools.class,"zle heslo");
                                    //ma zle heslo, napis chybu
                                    errors.add("error.logon.wrong.pass");
                                    request.setAttribute("error.logon.wrong.pass", "true");
                                }

                                if (user.isAuthorized()==false)
                                {
                                    errors.add("error.logon.user.unknown");
                                    Logger.error(LogonTools.class,"neautorizovany");
                                    //ma zle heslo, napis chybu
                                    if (Constants.getBoolean("formLoginProtect"))
                                    {
                                        //nepiseme o aku chybu ide
                                        request.setAttribute("error.logon.wrong.pass", "true");
                                    }
                                    else
                                    {
                                        request.setAttribute("error.logon.user.unknown", "true");
                                        request.setAttribute("error.logon.user.unknown.notAuthorized", "true");
                                    }
                                }
                            }
                        }
                        else
                        {
                            //zadany user neexistuje
                            Logger.error(LogonTools.class,"user neexistuje");
                            errors.add("error.logon.user.unknown");
                            if (user != null && user.isAuthorized()==false)
                            {
                                //nepiseme o aku chybu ide
                                request.setAttribute("error.logon.wrong.pass", "true");
                            }
                            else
                            {
                                request.setAttribute("error.logon.user.unknown", "true");
                            }
                        }
                    }
                    finally { db_result.close(); }
                }
                finally { ps.close(); }
            }
            finally { db_conn.close(); }

            if (passok)
            {
                int checkForAlarmDocId = checkForAlarm(user);
                if(checkForAlarmDocId != 0)
                {
                    session.setAttribute("alarm_warning", Integer.toString(checkForAlarmDocId));
                }
                else
                {
                    session.setAttribute("alarm_warning", "0");
                }
            }

        }
        catch (Exception ex)
        {
            Logger.error(LogonTools.class,"LogonAction: error");
            sk.iway.iwcm.Logger.error(ex);
        }

        // Save our logged-in user in the session
        user.setPassword(password);
        user.setValid(true);

        if (user.isAuthorized())
        {
            LogonTools.setUserToSession(session, user);

            //kontrola hesla podla nastavenych podmienok

            //nastavi nove heslo na aktualne
            if(session.getAttribute(Constants.USER_KEY+"_changepassword") != null)
            {
                String newPassword = request.getParameter("newPassword");
                String retypeNewPassword = request.getParameter("retypeNewPassword");
                if(newPassword.equals(retypeNewPassword))
                {
                    password = newPassword;
                }
                else
                {
                    Logger.error(LogonTools.class,"nove heslo a opakovanie noveho hesla sa nezhoduju");
                    errors.add("passwordsNotMatch");
                    request.setAttribute("passwordsNotMatch", "true");

                    session.removeAttribute(Constants.USER_KEY);

                    Adminlog.add(Adminlog.TYPE_USER_CHANGE_PASSWORD, user.getUserId(), "LogonTools - user ("+user.getLogin()+") password to change not match", -1, -1);

                    return errors;
                }
            }
            //END nastavi nove heslo na aktualne

            if(Password.checkPassword(true, password, passUser.isAdmin(), passUser.getUserId(), session, null))
            {
                //updatnem heslo na nove
                if(session.getAttribute(Constants.USER_KEY+"_changepassword") != null)
                {

                    try
                    {
                        Password pass = new Password();
                        String encryptedPassword = "";
                        PasswordsHistoryBean passwordsHistoryBean = new PasswordsHistoryBean(user.getUserId(),"", UsersDB.getSalt(user.getUserId()));
                        if (Constants.getBoolean("passwordUseHash"))
                        {
                            encryptedPassword = PasswordSecurity.calculateHash(password, UsersDB.getSalt(user.getUserId()));
                        }
                        else
                        {
                            encryptedPassword = pass.encrypt(password);
                            passwordsHistoryBean.setSalt("");
                        }
                        passwordsHistoryBean.setPassword(encryptedPassword);
                        DB.execute("UPDATE users SET password=? WHERE user_id=?", encryptedPassword, Integer.valueOf(user.getUserId()));
                        passwordsHistoryBean.saveIfNotExistsAndDeleteOld();
                        Adminlog.add(Adminlog.TYPE_USER_CHANGE_PASSWORD, user.getUserId(), "LogonTools - user ("+user.getLogin()+") successfully changed password", -1, -1);
                    }
                    catch (Exception ex)
                    {
                        sk.iway.iwcm.Logger.error(ex);
                    }
                }
                session.removeAttribute(Constants.USER_KEY+"_changepassword");
            }
            else
            {
                session.removeAttribute(Constants.USER_KEY);
                session.setAttribute(Constants.USER_KEY+"_changepassword", passUser);
            }
            //END kontrola hesla

        }

        auditLogon(errors, user, username, request);

        if (errors.isEmpty()==false) {
            setLoginBlocked(request);
        }

        return errors;
    }

    public static List<String> logonUserWithAllChecks(HttpServletRequest request, String username, String password) {
        List<String> errors = new ArrayList<>();

        //15888 #11: akceptuje sa len submit cez HTTP POST
		if("post".equalsIgnoreCase(request.getMethod()) == false) return null;

        //session fixation ochrana
        LogonTools.invalidateSessionOnFirstPost(request);

        //pravdepodobne submit nejakeho robota
        if (username == null || password == null) return null;

        String url = PathFilter.getOrigPath(request);
        HttpSession session = request.getSession();
		Prop prop = Prop.getInstance(request);

        //nemoze sa prihlasit lebo sa zle predtym prihlasil
		if(isLoginBlocked(request))
		{
			Logger.error(LogonTools.class, prop.getText("logon.error.blocked"));
			request.setAttribute("logon_message",prop.getText("logon.error.blocked"));
			request.setAttribute("error.logon.user.blocked", "true");
			if (errors.contains("error.logon.user.blocked")==false) errors.add("error.logon.user.blocked");

			Adminlog.add(Adminlog.TYPE_USER_LOGON,"Logon attempt TIME BLOCKED, username: " + username,-1,-1);
			return errors;
		}

        if(session.getAttribute(Constants.USER_KEY+"_changepassword") != null)
		{
			String newPassword = request.getParameter("newPassword");
			String retypeNewPassword = request.getParameter("retypeNewPassword");

			if(Tools.isEmpty(newPassword) || Tools.isEmpty(retypeNewPassword))
			{
				errors.add("passwordsNotMatch");

				Adminlog.add(Adminlog.TYPE_USER_LOGON,"Logon attempt CHANGE PASSWORD REQUIRED, username: " + username,-1,-1);
				return errors;
			}
		}

        errors = LogonTools.logonUser(request, username, password);

        // Report any errors we have discovered back to the original form
		if (!errors.isEmpty())
		{
			Logger.error(LogonTools.class, "su nejake chyby v logovacom formulari: "+url);
			return errors;
		}

        Identity user = UsersDB.getCurrentUser(request);
		if (user==null || user.isAuthorized()==false)
		{
            //there is allready logged user, so it means he doesnt' have permission to access this page
            errors.add("editor.permsDenied");
			request.setAttribute("unauthorized", "unauthorized");
			return errors;
		}

        callLogonLogoffInterceptor(user, request);

        return errors;
    }

    public static void afterLogon(Identity user, HttpServletRequest request, HttpServletResponse response)
	{
		String afterLogon = Constants.getString("afterLogonMethod");
		if (Tools.isNotEmpty(afterLogon))
		{
			int i = afterLogon.lastIndexOf('.');
			String customClass = afterLogon.substring(0, i);
			afterLogon = afterLogon.substring(i+1);
			//String
			try
			{
				Class<?> c = Class.forName(customClass);
				Object o = c.getDeclaredConstructor().newInstance();
				Method m;
				Class<?>[] parameterTypes = new Class<?>[] {Identity.class, HttpServletRequest.class, HttpServletResponse.class};
				Object[] arguments = new Object[] {user, request, response};
				m = c.getMethod(afterLogon, parameterTypes);
				m.invoke(o, arguments);
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}
		}
	}

	private static void callLogonLogoffInterceptor(UserDetails user, HttpServletRequest request)
	{
		String interceptorClassName = Constants.getString("stripesLogonLogoffInterceptorClass");

		if (Tools.isEmpty(interceptorClassName) && request.getAttribute("afterSaveInterceptor") != null) {
			interceptorClassName = Tools.getStringValue((String) request.getAttribute("afterSaveInterceptor"), "");
		}

		if (Tools.isNotEmpty(interceptorClassName)) {
			try
			{
				@SuppressWarnings("unchecked")
				Class<? extends AfterLogonLogoffInterceptor> interceptorClass = (Class<? extends AfterLogonLogoffInterceptor>) Class.forName(interceptorClassName);
				AfterLogonLogoffInterceptor interceptor = interceptorClass.getDeclaredConstructor().newInstance();

				interceptor.logon(user, request);
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}
	}

    public static int checkForAlarm(Identity user)
    {
        int userId;
        int warning = -1;
        int docId = 0;

        int alarmId = 1;
        try
        {
            userId = user.getUserId();
            Connection db_conn = DBPool.getConnection();
            try
            {
                PreparedStatement ps = db_conn.prepareStatement("SELECT * FROM user_alarm WHERE user_id=?");
                try
                {
                    ps.setInt(1, userId);
                    ResultSet rs = ps.executeQuery();
                    try
                    {
                        if(rs.next())
                        {
                            warning = rs.getInt("warning");
                        }
                    }
                    finally { rs.close(); }
                }
                finally { ps.close(); }

                ps = db_conn.prepareStatement("SELECT * FROM alarm_action WHERE alarm_id=?");
                try
                {
                    ps.setInt(1, alarmId);
                    ResultSet rs = ps.executeQuery();
                    try
                    {
                        if(rs.next())
                        {
                            docId = rs.getInt("doc_id");
                        }
                    }
                    finally { rs.close(); }
                }
                finally { ps.close(); }
            }
            finally { db_conn.close(); }

            if(warning == 1)
            {
                return docId;

            }
            //Logger.println(this,"WARNING: "+ warning);
        }
        catch (Exception ex)
        {
            sk.iway.iwcm.Logger.error(ex);
        }
        return (0);
    }

    /**
     * Ochrana Session Fixation (MFSR pentesty) ktora zabezpeci pri PRVOM odoslani (POST) logon formularu invalidnutie session
     * @param request
     */
    public static void invalidateSessionOnFirstPost(HttpServletRequest request)
    {
        if("post".equalsIgnoreCase(request.getMethod()) == false) return;

        final String SESSION_KEY = "session_fixation_invalidated";
        //session fixation - pri prihlaseni vzdy generujeme novu session
        if (request.getSession().getAttribute(SESSION_KEY)==null)
        {
            //toto potrebujeme zachovat po destroyi session (session fixation)
            String[] preservedSessionObjectNames = Tools.getTokens(Constants.getStringExecuteMacro("logonPreservedSessionObjects"), ",");
            Map<String, Object> preservedSessionObjects = new Hashtable<>();
            for (String name : preservedSessionObjectNames)
            {
                Object o = request.getSession().getAttribute(name);
                if (o != null) preservedSessionObjects.put(name, o);
            }

            request.getSession(false).invalidate();
            request.getSession().setAttribute(SESSION_KEY, 1);

            //preserve atributov
            for (String name : preservedSessionObjectNames)
            {
                Object o = preservedSessionObjects.get(name);
                if (o != null) request.getSession().setAttribute(name, o);
            }
        }
    }

    /**
     * Ulozi URL pred zobrazenim logon formu na ktoru sa po prihlaseni presmeruje
     * @param request
     */
    public static void saveAfterLogonRedirect(HttpServletRequest request)
    {
        HttpSession session = request.getSession();

        Identity user = UsersDB.getCurrentUser(request);
        //ak uz je prihlaseny user, neulozime (asi len nema prava)
        if (user != null) return;

        //ak uz mame nieco ulozene neprepiseme to
        if (session.getAttribute("adminAfterLogonRedirect")!=null) return;

        //bereme orig_path, je potrebne toto volat zo vsetkych miest pred redirectom dalej
        //teoreticky by sa dal pouzit referer, ale skrz viacere presmerovania a bezpecnost to nemozeme pouzit
        String origPath = (String)request.getAttribute("path_filter_orig_path");

        Logger.debug(LogonTools.class, "adminAfterLogonRedirect="+session.getAttribute("adminAfterLogonRedirect"));

        if (origPath != null && origPath.indexOf("logon")==-1 && origPath.equals("/admin/")==false && origPath.equals("/admin")==false && origPath.indexOf("/admin/index.jsp")==-1 && origPath.indexOf("/admin/welcome.jsp")==-1 && session.getAttribute("adminAfterLogonRedirect")==null)
        {
            //ukladame iba taketo cesty, nech sa nam tam neulozi odkaz na css, js alebo nieco podobne
            if (origPath.endsWith(".do") || origPath.endsWith(".jsp") || origPath.endsWith("/") || origPath.endsWith(".action"))
            {
                Logger.debug(LogonTools.class, "mainLink=" + request.getParameter("mainLink"));
                if (request.getParameter("mainLink") != null)
                {
                    origPath = request.getParameter("mainLink");
                }
                else
                {
                    String QS = (String) request.getAttribute("path_filter_query_string");
                    if (Tools.isNotEmpty(QS)) origPath = origPath + "?" + QS;
                }

                Logger.debug(LogonTools.class, "origPath=" + origPath);
                if (Tools.isNotEmpty(origPath) &&
                        origPath.indexOf("welcome.jsp") == -1 && origPath.indexOf("refresher") == -1 &&
                        origPath.indexOf("FCKeditor") == -1 && origPath.indexOf("logon.jsp") == -1 &&
                        origPath.indexOf("ajax") == -1 && origPath.indexOf("/admin/todo/") == -1 &&
                        origPath.indexOf("combine.jsp") == -1 && origPath.indexOf("edituser.do") == -1 &&
                        origPath.equals("/admin/index.jsp") == false && origPath.equals("/admin/") == false)
                {
                    session.setAttribute("adminAfterLogonRedirect", origPath);
                }
            }
        }
    }

    /**
     * Nastavi usera do session a nastavi spring prava
     * @param session
     * @param user
     */
    public static Authentication setUserToSession(HttpSession session, Identity user)
    {
        if (session != null) session.setAttribute(Constants.USER_KEY, user);

        try
        {
            //prihlasenie pre SPRING / REST
            //RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
            if (Constants.getServletContext().getAttribute("springContext")!=null)
            {
                //ApplicationContext context = (ApplicationContext) Constants.getServletContext().getAttribute("springContext");
                //AuthenticationManager authenticationManager = context.getBean("authenticationManagerBean", AuthenticationManager.class);
                final Authentication authentication = WebjetAuthentificationProvider.authenticate(user);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                return authentication;
            }
        }
        catch (Exception ex)
        {
            sk.iway.iwcm.Logger.error(ex);
        }

        return null;
    }

    /**
     * Test if login is not time/IP blocked
     * @param request
     * @return
     */
    public static boolean isLoginBlocked(HttpServletRequest request) {
        Cache cache = Cache.getInstance();

        String ipAddress = Tools.getRemoteIP(request);
        String LOGON_BLOCKED_IP_CACHE_KEY = "logon_ip_"+ipAddress;
        String LOGON_BLOCKED_USERNAME_TIME_CACHE_KEY = "logon_blocked_"+ipAddress+"_time";
        String LOGON_BLOCKED_USERNAME_COUNT_CACHE_KEY = "logon_blocked_"+ipAddress+"_count";

		Calendar cal = (Calendar)cache.getObject(LOGON_BLOCKED_IP_CACHE_KEY);
        if (cal != null) {
            Logger.debug(LogonTools.class, "Mam cal: "+Tools.formatDateTimeSeconds(cal.getTimeInMillis()));
        }

        //#20489
		Calendar calDelay = (Calendar)cache.getObject(LOGON_BLOCKED_USERNAME_TIME_CACHE_KEY);
		if(Constants.getInt("logonBlockedAfterUnsuccessCount") > 0 && Constants.getInt("logonLoginBlockedDelay") > 0 &&
				(calDelay == null || calDelay.getTimeInMillis() < Tools.getNow()))
		{
			Integer userLogonCount = (Integer)cache.getObject(LOGON_BLOCKED_USERNAME_COUNT_CACHE_KEY);
			if(userLogonCount != null)
			{
				Logger.debug(LogonTools.class, "Pokus cislo : " + userLogonCount);
				//nastavime blokovanie na login (a je jedno, ze potom zada spravne heslo)
				if( userLogonCount.intValue() > Constants.getInt("logonBlockedAfterUnsuccessCount"))
				{
					Calendar cal3 = Calendar.getInstance();
					cal3.add(Calendar.SECOND, Constants.getInt("logonLoginBlockedDelay"));
					Logger.debug(LogonTools.class, "Blokujem ip "+ipAddress+" na " + (Constants.getInt("logonLoginBlockedDelay")/60)+" minut");
					cal = cal3;
				}
			}
		}
		if(calDelay != null && (cal == null || calDelay.getTimeInMillis() > cal.getTimeInMillis()))
		{
			cal = calDelay;
		}
        //nemoze sa prihlasit lebo sa zle predtym prihlasil
		if(cal != null && cal.getTimeInMillis() > Tools.getNow())
		{
            return true;
        }
        return false;
    }

    /**
     * Cache info about bad credentials/login to block for 10 seconds
     * @param request
     */
    public static void setLoginBlocked(HttpServletRequest request) {
        Cache cache = Cache.getInstance();

        String ipAddress = Tools.getRemoteIP(request);
        String LOGON_BLOCKED_IP_CACHE_KEY = "logon_ip_"+ipAddress;
        String LOGON_BLOCKED_USERNAME_TIME_CACHE_KEY = "logon_blocked_"+ipAddress+"_time";
        String LOGON_BLOCKED_USERNAME_COUNT_CACHE_KEY = "logon_blocked_"+ipAddress+"_count";

        if (Constants.getInt("logonBlockedDelay")>0)
        {
            Calendar cal2=Calendar.getInstance();
            cal2.add(Calendar.SECOND, Constants.getInt("logonBlockedDelay"));
            cache.setObjectSeconds(LOGON_BLOCKED_IP_CACHE_KEY, cal2, Constants.getInt("logonBlockedDelay")+10, false);
        }

        Integer userLogonCount = (Integer)cache.getObject(LOGON_BLOCKED_USERNAME_COUNT_CACHE_KEY);
        if(userLogonCount != null)
        {
            userLogonCount = Integer.valueOf(userLogonCount.intValue()+1);

            Logger.debug(LogonTools.class, "Pokus cislo : " + userLogonCount);
            cache.setObjectSeconds(LOGON_BLOCKED_USERNAME_COUNT_CACHE_KEY, userLogonCount, Constants.getInt("logonLoginBlockedDelay"), false);
            //nastavime blokovanie na login (a je jedno, ze potom zada spravne heslo)
            if( userLogonCount > Constants.getInt("logonBlockedAfterUnsuccessCount"))
            {
                Calendar cal3 = Calendar.getInstance();
                cal3.add(Calendar.SECOND, Constants.getInt("logonLoginBlockedDelay"));
                Logger.debug(LogonTools.class, "Blokujem ip "+ipAddress+" na " + Constants.getInt("logonLoginBlockedDelay")+" sekund");
                cache.setObjectSeconds(LOGON_BLOCKED_USERNAME_TIME_CACHE_KEY, cal3, Constants.getInt("logonLoginBlockedDelay"), false);
            }
        }
        else
        {
            //Logger.debug(UsrLogonAction.class, "Nastavujem pocet pokusov na : 1 a trvanie na "+(Constants.getInt("logonLoginBlockedDelay") / 60)+" minut");
            cache.setObjectSeconds(LOGON_BLOCKED_USERNAME_COUNT_CACHE_KEY, 1, Constants.getInt("logonLoginBlockedDelay"), false);
        }
    }
}
