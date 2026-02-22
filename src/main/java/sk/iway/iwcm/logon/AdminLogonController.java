package sk.iway.iwcm.logon;

import java.util.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import sk.iway.Password;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.LogonTools;
import sk.iway.iwcm.components.users.userdetail.UserDetailsService;
import sk.iway.iwcm.components.users.userdetail.UserDetailsRepository;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.stat.StatDB;
import sk.iway.iwcm.system.googleauth.GoogleAuthenticator;
import sk.iway.iwcm.system.googleauth.GoogleAuthenticatorKey;
import sk.iway.iwcm.system.googleauth.GoogleAuthenticatorQRGenerator;
import sk.iway.iwcm.system.ntlm.AuthenticationFilter;
import sk.iway.iwcm.system.spring.SpringUrlMapping;
import sk.iway.iwcm.system.spring.oauth2.OAuth2LoginHelper;
import sk.iway.iwcm.tags.support.ResponseUtils;
import sk.iway.iwcm.users.PasswordSecurity;
import sk.iway.iwcm.users.UserChangePasswordService;
import sk.iway.iwcm.users.UsersDB;

/**
 * LogonController.java
 *
 * Class LogonController is used for
 *
 *
 * Title        webjet8
 * Company      Interway a.s. (www.interway.sk)
 * Copyright    Interway a.s. (c) 2001-2018
 * @author      $Author: mhruby $
 * @version     $Revision: 1.0 $
 * created      14.9.2018 10:55
 * modified     14.9.2018 10:55
 */

@Controller
@RequestMapping("/admin/")
public class AdminLogonController {

    private static final String LOGON_FORM = "/admin/skins/webjet8/logon-spring";
    private static final String CHANGE_PASSWORD_FORM = "/admin/skins/webjet8/logon-spring-change-password";
    private static final String TWOFA_PASSWORD_FORM = "/admin/skins/webjet8/logon-spring-2fa";
    private static final String LICENSE = "/wjerrorpages/setup/license";

    private final UserDetailsRepository userDetailsRepository;

    @Autowired
    public AdminLogonController(UserDetailsRepository userDetailsRepository) {
        this.userDetailsRepository = userDetailsRepository;
    }

    /**
     * This method is used from email link to change password
     * @param request
     * @param session
     * @return
     */
    @GetMapping("logon/changePassword")
    public String showChangePasswordForm(ModelMap model, HttpServletRequest request, HttpSession session) {
        UserForm userForm = UserChangePasswordService.getPreparedUserForm(request, model);

        if(userForm == null) {
            model.addAttribute("userForm", new UserForm());
            return LOGON_FORM;
        }

        model.addAttribute("userForm", userForm);
        return CHANGE_PASSWORD_FORM;
    }

    @PostMapping("logon/changePassword")
    public String edit(@ModelAttribute("userForm") UserForm userForm, ModelMap model, HttpServletRequest request, HttpServletResponse response, HttpSession session) {

        Identity user = null;
        String selectedLoginFromSelect = userForm.getSelectedLogin();
        String changePasswordAuth = userForm.getAuth();
        List<String> errors = new ArrayList<>();

        // This is special
        //  -> can contain only 1 login value when reseting password via login
        //  -> when reseting password via email, it can contain multiple login's separeted by UserChangePasswordService.LOGINS_SEPARATOR
        String login = userForm.getLogin();

        if (Tools.isNotEmpty(changePasswordAuth)) {
            // Verify selected login and in one shoot it will verify changePasswordAuth
            if(UserChangePasswordService.verifyLoginValue(login, selectedLoginFromSelect, changePasswordAuth, request) == true)
                user = new Identity( UsersDB.getUser(selectedLoginFromSelect) );
        } else {
            user = (Identity)session.getAttribute(Constants.USER_KEY+"_changepassword");
        }

        if (user == null) {
            // zaskodnik tu nema co robit
            return LOGON_FORM;
        }
        Prop prop = Prop.getInstance(request.getServletContext(), request);

        model.addAttribute("userForm", userForm);

        // je tam daco a je to rovnake?
        if(Tools.isEmpty(userForm.getNewPassword()) || Tools.isEmpty(userForm.getRetypeNewPassword()) || !(userForm.getNewPassword().equals(userForm.getRetypeNewPassword()))) {
            errors.add(prop.getText("logon.change_password.password_not_match"));
            model.addAttribute("errorsList", errors);
            return CHANGE_PASSWORD_FORM;
        }

        String currentPassword = userDetailsRepository.getPasswordByUserId((long)user.getUserId());
        if (Constants.getBoolean("passwordUseHash") && currentPassword.equals(PasswordSecurity.calculateHash(userForm.getNewPassword(), userDetailsRepository.getPasswordSaltByUserId((long)user.getUserId()))) || currentPassword.equals(userForm.getNewPassword())) {
            // povodne heslo je rovnake ako nove heslo
            errors.add(prop.getText("logon.change_password.old_password_match_new"));
            model.addAttribute("errorsList", errors);
            return CHANGE_PASSWORD_FORM;
        } else if (Password.checkPassword(true, userForm.getNewPassword(), true, user.getUserId(), session, errors)){
            user.setPassword(userForm.getNewPassword());
            UserDetailsService.savePassword(userForm.getNewPassword(), user.getUserId());

            Adminlog.add(Adminlog.TYPE_USER_CHANGE_PASSWORD, user.getUserId(), "UsrLogonAction - user (" + user.getLogin() + ") successfully changed password", -1, -1);

            //During password change action, user is obtained via login, not via session
            if(Tools.isEmpty(changePasswordAuth) == true) {
                session.removeAttribute(Constants.USER_KEY+"_changepassword");
                LogonTools.setUserToSession(session, user);
            }

            this.determineLanguage(session, request, response);
            this.determineDefaultWebPagesDirectory(user, session);
            this.checkForNewHelp(session, user);
            this.determineRootWebPageDirectory(session, user);

            if (Tools.isNotEmpty(changePasswordAuth)) {
                // Delete admin log - so change password action will no longer be available
                UserChangePasswordService.deleteChangePasswordAdminlogBean(login, changePasswordAuth);

                // Redirect to login page where they can test new password
                // - with changePasswordActionSuccess parameter
                return "redirect:/admin/logon/?act=changePasswordActionSuccess";
            } else {
                String forwardAfterToken = (String)session.getAttribute("adminAfterLogonRedirect");
                if (Tools.isEmpty(forwardAfterToken))
                    return "redirect:/admin/v9/";
                return "redirect:" + forwardAfterToken;
            }
        } else {
            if (errors.size()>0) model.addAttribute("errorsList", errors);
            return CHANGE_PASSWORD_FORM;
        }
    }

    @GetMapping("logon.struts")
    @PostMapping("logon.struts")
    //presmerovanie stareho Struts volania /admin/logon.do na novy form
    public String logonDoRedirect() {
        return "redirect:/admin/logon/";
    }

    @GetMapping("logon/")
    public String showForm(UserForm userForm, ModelMap model, HttpServletRequest request, HttpSession session)
    {
        Identity user = UsersDB.getCurrentUser(session);
        if (user != null && user.isAdmin())
        {
            //user je uz prihlaseny, preforwardnime ho na /admin/
            return "redirect:/admin/v9/";
        }

        //--------------Prihlasenie prebieha cez NTLM, nema sa co prihlasovat cez formularik-----------
        if (AuthenticationFilter.weTrustIIS())
        {
            return "redirect:"+ AuthenticationFilter.getForbiddenURL();
        }

        if  (request.getParameter("language")==null && request.getSession().getAttribute(Prop.SESSION_I18N_PROP_LNG)==null && Tools.isNotEmpty(Constants.getString("defaultLanguage")))
        {
            request.getSession().setAttribute(Prop.SESSION_I18N_PROP_LNG, Constants.getString("defaultLanguage"));
        }

        String adminHost = Constants.getString("multiDomainAdminHost");
        //out.println("adminHost="+adminHost+" domain="+DocDB.getDomain(request));

        String serverName = Tools.getServerName(request);
        if (("iwcm.interway.sk".equals(request.getServerName())==false && "localhost".equals(request.getServerName())==false) && Tools.isNotEmpty(adminHost) && (","+adminHost+",").indexOf(","+serverName+",")==-1)
        {
            if (Constants.getBoolean("adminLogonShowSimpleErrorMessage"))
            {
                return "/404.jsp";
            }
            else
            {
                String[] hosts = Tools.getTokens(adminHost, ",");
                return "redirect:"+request.getScheme()+"://"+hosts[0]+"/admin/";
            }
        }

        if (Tools.isNotEmpty(Constants.getString("defaultLanguage"))) userForm.setLanguage(Constants.getString("defaultLanguage"));

        String language = request.getParameter("language");
        if (Tools.isNotEmpty(language) && language.length()==2)
        {
            userForm.setLanguage(language);
        }

        LogonTools.saveAfterLogonRedirect(request);

        // Spracuj OAuth2 chyby zo session
        String oauth2LogonError = (String)session.getAttribute("oauth2_logon_error");
        if (oauth2LogonError != null) {
            Prop prop = Prop.getInstance(request);
            String errorMessage = switch (oauth2LogonError) {
                case "accessDenied" -> prop.getText("logon.err.noadmin");
                case "oauth2_email_not_found" -> prop.getText("logon.err.oauth2_email_not_found");
                case "oauth2_user_create_failed" -> prop.getText("logon.err.oauth2_user_create_failed");
                case "oauth2_exception" -> prop.getText("logon.err.oauth2_exception");
                default -> prop.getText("logon.err.oauth2_unknown");
            };
            model.addAttribute("errors", errorMessage);
            session.removeAttribute("oauth2_logon_error");
        }

        if(request.getParameter("loginName") != null)
        {
            String loginName = request.getParameter("loginName");
            request.setAttribute(UsersDB.IS_ADMIN_SECTION_KEY, true);
            UserChangePasswordService.sendPassword(request,loginName);
        }

        String autoRedirect = addOAuth2UrlsToModel(request, model);
        if (Tools.isNotEmpty(autoRedirect)) {
            model.addAttribute("autoRedirect", autoRedirect);
            if (Tools.isEmpty(oauth2LogonError)) {
                if (request.getParameter("logoff") == null) {
                    // Automatic redirect to first OAuth2 provider instead of showing logon form
                    return "redirect:" + autoRedirect;
                }
            }
        }

        return LOGON_FORM;
    }

    private String addOAuth2UrlsToModel(HttpServletRequest request, ModelMap model) {
        String autoRedirectUrl = null;

        Map<String, String> logonUrls = OAuth2LoginHelper.getLogonUrls(true, request);
        if (logonUrls.size() > 0) {
            HttpSession session = request.getSession();
            // Nastav explicitný atribút pre OAuth2 admin login
            OAuth2LoginHelper.setAdminLogin(request);

            // Ak adminAfterLogonRedirect neexistuje, nastav defaultnú hodnotu
            if (session.getAttribute("adminAfterLogonRedirect") == null) {
                session.setAttribute("adminAfterLogonRedirect", "/admin/");
            }

            model.addAttribute("logonUrls", logonUrls);
            String oauth2AdminLogonAutoRedirect = Constants.getString("oauth2_adminLogonAutoRedirect");
            if (Tools.isNotEmpty(oauth2AdminLogonAutoRedirect)) {
                autoRedirectUrl = logonUrls.get(oauth2AdminLogonAutoRedirect); // redirect to specified provider if auto redirect is enabled
            }
        }
        return autoRedirectUrl;
    }

    @PostMapping("logon/")
    public String submit(@ModelAttribute("userForm") UserForm userForm, BindingResult result, ModelMap model, HttpServletRequest request, HttpServletResponse response) {
        if (InitServlet.verify(request) == false)
            return SpringUrlMapping.redirect(LICENSE);

        //session fixation ochrana
        LogonTools.invalidateSessionOnFirstPost(request);
        HttpSession session = request.getSession();

        //fix: after passwordExpiryDay notifycation it was possible to go to logon and skip verification
        session.removeAttribute(Constants.USER_KEY+"_changepassword");

        Prop prop = Prop.getInstance(request.getServletContext(), request);

        String twoFaRedirect = verify2FaKey(request);
        if (Tools.isNotEmpty(twoFaRedirect)) return twoFaRedirect;

        Identity user = new Identity();
        Map<String, String> errors = new Hashtable<>();
        LogonTools.logon(userForm.getUsername(), userForm.getPassword(), user, errors, request, prop);

        if (errors.get("ERROR_KEY")!=null) {
            Logger.error(this,"su nejake chyby v logovacom formulari");
            model.addAttribute("errors", errors.get("ERROR_KEY"));
            addOAuth2UrlsToModel(request, model);
            return LOGON_FORM;
        }

        // Save our logged-in userForm in the session
        user.setLoginName(userForm.getUsername());
        user.setValid(true);

        // pouzivatel je prihlaseny
        LogonTools.setUserToSession(session, user);

        if (!(Password.checkPassword(true, userForm.getPassword(), user.isAdmin(), user.getUserId(), session, null))) {
            // ma slabe heslo
            session.removeAttribute(Constants.USER_KEY);
            session.setAttribute(Constants.USER_KEY+"_changepassword", user);
            userForm.setPassword(userForm.getPassword().replace(".", "*")); // bezpecny placeholder
            model.addAttribute("userForm", userForm);
            model.addAttribute("isAdmin", user.isAdmin());
            return CHANGE_PASSWORD_FORM;
        }

		twoFaRedirect = set2FaAuthForm(user, request);
        if (Tools.isNotEmpty(twoFaRedirect)) return twoFaRedirect;


        this.determineLanguage(session, request, response);
        this.determineDefaultWebPagesDirectory(user, session);
        this.checkForNewHelp(session, user);
        this.determineRootWebPageDirectory(session, user);
        StatDB.addAdmin(request);

        String adminAfterLogonRedirect = (String)session.getAttribute("adminAfterLogonRedirect");
        if (Tools.isNotEmpty(adminAfterLogonRedirect)) {
            if (adminAfterLogonRedirect.startsWith("/admin/v9/") || adminAfterLogonRedirect.startsWith("/admin/approve") || (adminAfterLogonRedirect.startsWith("/apps/") && adminAfterLogonRedirect.contains("/admin/"))) {
                return "redirect:" + adminAfterLogonRedirect;
            }
        }

        return "redirect:/admin/v9/";
    }


    /**
     * Set admin session for oauth2 return after successful authentication
     * @param request
     * @return
     */
    @GetMapping("logon/setadmin/")
    @ResponseBody
    public String setAdmin(HttpServletRequest request) {
        OAuth2LoginHelper.setAdminLogin(request);
        return "OK";
    }

    private void determineLanguage(HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        String lng = ResponseUtils.filter(request.getParameter("language"));
        if (Tools.isEmpty(lng)) lng = Constants.getString("defaultLanguage");
        PageLng.setUserLng(request, response, lng);
        session.setAttribute(Prop.SESSION_I18N_PROP_LNG, lng);
    }

    private void determineDefaultWebPagesDirectory(Identity user, HttpSession session) {
        //nastav predvoleny adresar Na Schalenie
        DocDB docDB = DocDB.getInstance();
        List<DocDetails> docsToApprove = docDB.getDocsForApprove(user.getUserId());
        if (docsToApprove!=null && docsToApprove.size()>0)
            session.setAttribute(Constants.SESSION_GROUP_ID, Integer.toString(Constants.getInt("systemPagesDocsToApprove")));
    }


    private void determineRootWebPageDirectory(HttpSession session, Identity user) {
        if (Tools.isNotEmpty(user.getEditableGroups())) {
            //prestav v session default host na prvy z editable groups
            int groupId = getUserFirstEditableGroup(user);
            if (groupId > 0)
                setSessionGroup(groupId, session);
        } else if (Constants.getBoolean("enableStaticFilesExternalDir")) {
            int groupId = Constants.getInt("rootGroupId");
            setSessionGroup(groupId, session);
        }
    }

    private void checkForNewHelp(HttpSession session, Identity user) {
        //ziskaj datum posledneho prihlasenia a datum najnovsej novinky
        /*try
        {
            IwcmFile[] newHelpFiles = (new IwcmFile(Tools.getRealPath("/admin/help/sk/new"))).listFiles();
            Arrays.sort(newHelpFiles,
                    new Comparator<IwcmFile>()
                    {
                        @Override
                        public int compare(IwcmFile f1, IwcmFile f2)
                        {
                            if (f1.isDirectory())
                            {
                                //return(-1);
                            }
                            if (f2.isDirectory())
                            {
                                //return(1);
                            }

                            int lm1 = 0;
                            int lm2 = 0;

                            try
                            {
                                if (f1.isFile()) lm1 = Tools.getIntValue(f1.getName().substring(0, f1.getName().indexOf('.')), -1);
                                if (f2.isFile()) lm2 = Tools.getIntValue(f2.getName().substring(0, f2.getName().indexOf('.')), -1);
                            }
                            catch (Exception e)
                            {
                                //ignoruj
                            }

                            //Logger.println(this,"compare: " + lm1 + " " + f1.getName() + " vs " + lm2 + " " + f2.getName());

                            if (lm1 == lm2) return(0);
                            else if (lm1 > lm2) return(-1);
                            else return(1);
                        }
                    });

            //najnovsi je na pozicii 0
            IwcmFile newestHelpFile = newHelpFiles[0];
            //Logger.println(this,"NEWEST: " + newestHelpFile.getName());

            //skontroluj, ci uz videl tento subor
            long lastSeenDate = Adminlog.getLastDate(Adminlog.TYPE_HELP_LAST_SEEN, user.getUserId());

            if (lastSeenDate < newestHelpFile.lastModified())
            {
                //este nevidel, treba mu zobrazit
                session.setAttribute("show_help_file_after_logon", newestHelpFile.getName());
                Adminlog.add(Adminlog.TYPE_HELP_LAST_SEEN, user.getUserId(), "helpfile: " + newestHelpFile.getName(), -1, -1);
            }
        }
        catch (Exception e)
        {
            Logger.error(AdminLogonController.class, e);
        }*/
    }

    /**
     * Nastavi session ID adresara web stranok a preview domenu
     * @param groupId
     * @param session
     */
    private static void setSessionGroup(int groupId, HttpSession session)
    {
        session.setAttribute(Constants.SESSION_GROUP_ID, String.valueOf(groupId));

        GroupDetails root = GroupsDB.getInstance().getGroup(groupId);
        if (root != null && Tools.isNotEmpty(root.getDomainName()))
        {
            session.setAttribute("preview.editorDomainName", root.getDomainName());
        }
    }

    /**
     * Overi odoslanu hodnotu 2FA cisla, ak je nespravna vrati linku na formular, inak presmerovanie do administracie
     * Vrati NULL ak 2FA nie je aktivovana
     * @param request
     * @return
     */
    private static String verify2FaKey(HttpServletRequest request) {
        HttpSession session = request.getSession();
        if (session.getAttribute("token")!=null)
		{
			//ocakava sa token
			String generatedToken = (String)session.getAttribute("token");

			try{

				String insertedCodeString = request.getParameter("token");
				int insertedCode = Integer.parseInt(insertedCodeString);

				GoogleAuthenticator gAuth = new GoogleAuthenticator();

				int generatedCode = gAuth.getTotpPassword(generatedToken);

				Logger.debug(AdminLogonController.class,"userToken : " + insertedCode + "\n token : "+gAuth.getTotpPassword(generatedToken)+ "\n code : "+generatedCode );

				if (insertedCode == generatedCode)
				{
                    String token = (String)session.getAttribute("token");
					session.removeAttribute("token");
					Identity sessionUserAfterToken = (Identity)session.getAttribute("adminUser_waitingForToken");
					session.removeAttribute("adminUser_waitingForToken");
					if (sessionUserAfterToken!=null) {
                        LogonTools.setUserToSession(session, sessionUserAfterToken);

                        //set user code
                        String currentCode = new SimpleQuery().forString("SELECT mobile_device FROM users WHERE user_id = ?", sessionUserAfterToken.getUserId());
                        if (Tools.isNotEmpty(token) && Tools.isEmpty(currentCode)) {
                            new SimpleQuery().execute("UPDATE users SET mobile_device = ? WHERE user_id = ?", token, sessionUserAfterToken.getUserId());
                            sessionUserAfterToken.setMobileDevice(currentCode);
                        }
                    }

					return "redirect:/admin/v9/";
				}

			}catch (NumberFormatException e){
                //asi nebolo zadane cislo, cize kod je zly
			}
			//ak nie znova vrat logon
			request.setAttribute("errors", "wrongCode");
			//wrongCode
			return TWOFA_PASSWORD_FORM;
		}
        return null;
    }

    /**
     * Overi ci je zapnute 2FA, ak ano, vrati linku na formular
     * @param user
     * @param session
     * @return
     */
    private String set2FaAuthForm(Identity user, HttpServletRequest request) {

        String mobileDevice = userDetailsRepository.getMobileDeviceByUserId((long)user.getUserId());

        //ak je aktivovana dvojfaktorova autentifikacia a user ma nastaveny devicekey
        if (Tools.isNotEmpty(mobileDevice) || Constants.getBoolean("isGoogleAuthRequiredForAdmin") ) {
            HttpSession session = request.getSession();

            if (Tools.isEmpty(mobileDevice) || mobileDevice.length()<5) {  // - je forced gauth ^ cfg premennou
                GoogleAuthenticator gAuth = new GoogleAuthenticator();
                final GoogleAuthenticatorKey key = gAuth.createCredentials();
                session.setAttribute("token", key.getKey());	// hodime si do session novo vygenerovane credentials
                session.setAttribute("QRURL", GoogleAuthenticatorQRGenerator.getOtpAuthURL("WebJET " + Constants.getInstallName() + " (" + Tools.getServerName(request) + ")", user.getLogin(), key));
                session.setAttribute("scratchcode", key.getScratchCodes().get(0).toString());
            }else{
                session.setAttribute("token", mobileDevice);
            }

            // Google Authenticator
            //String token = RandomStringUtils.secure().next(4, false, true);
            //sendToken(mobileDevice, token);
            session.setAttribute("adminUser_waitingForToken", user);
            session.removeAttribute(Constants.USER_KEY);
            //Logger.debug(LogonAction.class, "LogonAction dualFactorToken: "+mobileDevice);
            // zobraz naspat admin
            return TWOFA_PASSWORD_FORM;

        }

        return null;
    }

    public static int getUserFirstEditableGroup(Identity user)
	{
		int[] editableGroups = Tools.getTokensInt(user.getEditableGroups(), ",");
		if (editableGroups!=null && editableGroups.length>0)
		{
			for (int groupId : editableGroups)
			{
				if (groupId > 0)
				{
					return groupId;
				}
			}
		}
		return -1;
	}

    @PostMapping("/rest/removeSession")
    @PreAuthorize("@WebjetSecurityService.isAdmin()")
    @ResponseBody
    public String removeSession(@RequestParam("sessionId") String sessionId, HttpServletRequest request) {
        Identity user = UsersDB.getCurrentUser(request);
        boolean success = false;
        if (user != null) {
            success = sk.iway.iwcm.stat.SessionHolder.getInstance().invalidateSession(user.getUserId(), sessionId);
        }
        return "{success: "+success+"}";
    }
}
