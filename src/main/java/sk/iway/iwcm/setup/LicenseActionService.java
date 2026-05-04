package sk.iway.iwcm.setup;

import java.io.IOException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.ui.Model;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.LogonTools;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

public class LicenseActionService {

    private static final String SUCCESS = "/admin/setup/license_saved";
    private static final String ERROR = "/admin/setup/license";

	private LicenseActionService() {
		//utility class
	}

	/**
	 * Update WebJET license. License is not checked, but user must give correct login username and password and
	 * this user must have ADMIN role.
	 *
	 * @param licenseForm - bean oontaining login and license
	 * @param model
	 * @param request
	 * @param response
	 * @return
	 * @throws IOException
	 * @throws ServletException
	 */
	public static String updateLicense(LicenseFormBean licenseForm, Model model, HttpServletRequest request, HttpServletResponse response) {
		Map<String, String> errors = new Hashtable<>();

		//Validate user data and right to do this action
		UserDetails user = validateUserLogin(licenseForm, errors);

		if (errors.get("ERROR_KEY") != null) {
			Logger.error(LicenseActionService.class,"su nejake chyby v logovacom formulari");
			setModel(model, licenseForm, true, errors.get("ERROR_KEY"), true, false);
			return ERROR;
		}

		String license = licenseForm.getLicense();
		if (user != null && license != null && user.getUserId() > 0 && user.isAdmin()) {
			//Update existing license
			int result = (new SimpleQuery()).executeWithUpdateCount("UPDATE " + ConfDB.CONF_TABLE_NAME + " SET value=? WHERE name='license'", license.toLowerCase());

			//In this case, DB table conf does not contain license YET. Can happen if WebJET is used without license (free version)
			//So perform insert of license
			if(result < 1)
				result = (new SimpleQuery()).executeWithUpdateCount("INSERT INTO " + ConfDB.CONF_TABLE_NAME + " (name, value) VALUES (?, ?)", "license", license.toLowerCase());

			//Set refresh
			if (Tools.getIntValue(result, 0) > 0) {
				HttpSession session = request.getSession();
				session.setAttribute("pageRefresh", "");
				InitServlet.restart();
			}
		}

		setModel(model, null, false, null, true, true);

		return SUCCESS;
	}

	/**
	 * Check taht user data are correct (good username and passwd).
	 * User must exist, be authorized, be admin.
	 *
	 * @param licenseForm
	 * @param errors - Map that will be filled with found arrors
	 * @return
	 */
	private static UserDetails validateUserLogin(LicenseFormBean licenseForm, Map<String, String> errors) {
		Prop prop = Prop.getInstance();

        if(licenseForm == null) {
            errors.put("ERROR_KEY", prop.getText("approveAction.err.badPass"));
            return null;
        }

        String username = licenseForm.getUsername();
        String password = licenseForm.getPassword();

		if (Tools.isEmpty(username) || Tools.isEmpty(password)) {
			errors.put("ERROR_KEY", prop.getText("approveAction.err.badPass"));
			return null;
		}

		//Get user from DB based on login from form
		UserDetails user = UsersDB.getUser(username);

		//Check if user exist
		if (user != null) {
			//Check password
			String passwordInDb = null;
			try {
				passwordInDb = (new SimpleQuery()).forString("SELECT password FROM users WHERE login=?", username);
			} catch(IllegalStateException ex) {
				//Salt fiel does not EXIST yet -> in case when we run setup without license (it's not inicialized yet)
			}

			String salt = null;
			try {
				salt = (new SimpleQuery()).forString("SELECT password_salt FROM users WHERE login=?", username);
			} catch(IllegalStateException ex) {
				//Salt fiel does not EXIST yet -> in case when we run setup without license (it's not inicialized yet)
			}

			if (LogonTools.isPasswordCorrect(password, salt, passwordInDb)) {
				if (user.isAuthorized()) {
					if (!user.isAdmin()) {
                        //User is no admin, he has no right o do this action
						Logger.error(LicenseActionService.class,"user nie je administrator");
                        errors.put("ERROR_KEY", prop.getText("setup.license.no_right"));
						return null;
					}

					try {
						if (user.getEditablePages().length() > 0 && user.getEditableGroups().length() == 0)
							user.setEditableGroups(Integer.toString(Constants.getInt("systemPagesMyPages")));
					} catch (Exception ex) {}
				}
			} else {
				//Login password is incorrect
				Logger.error(LicenseActionService.class,"zle heslo");
                errors.put("ERROR_KEY", prop.getText("approveAction.err.badPass"));
                return null;
			}
		} else {
			//This user does not exist
			Logger.error(LicenseActionService.class,"user neexistuje");
            errors.put("ERROR_KEY", prop.getText("approveAction.err.badPass"));
            return null;
		}

		if (user.getUserId() > 0) {
			if (user.isAuthorized() == false) {
                //Is user is not authorized, just return message about incorrect login params
				Logger.println(LicenseActionService.class,"JE neautorizovany!!");
                errors.put("ERROR_KEY", prop.getText("approveAction.err.badPass"));
                return null;
			} else {
				//Update user last logon value
				(new SimpleQuery()).execute("UPDATE  users SET last_logon=? WHERE user_id=?", new Date());
			}
		}

		return user;
	}

	/**
	 * Prepare model attributes, based on input values. Model will be used by page.
	 *
	 * @param model
	 * @param request
	 * @param response
	 */
	public static void setModel(Model model, HttpServletRequest request, HttpServletResponse response) {
		//Create empty initialiyation of object, used for FE form
		LicenseFormBean licenseForm = new LicenseFormBean();

		//
		String lng = request.getParameter("language");
		if(Tools.isNotEmpty(lng)) {
            PageLng.setUserLng(request, response, lng);
            request.getSession().setAttribute(Prop.SESSION_I18N_PROP_LNG, lng);
			licenseForm.setPageLngIndicator(lng);
    	} else {
			String oldLng = (String) request.getSession().getAttribute(Prop.SESSION_I18N_PROP_LNG);
			if(Tools.isNotEmpty(oldLng)) licenseForm.setPageLngIndicator(oldLng);
		}

		//
		setModel(model, licenseForm, false, null, false, false);
	}

	/**
	 * Prepare model attributes, based on input values. Model will be used by page.
	 *
	 * @param model
	 * @param licenseForm
	 * @param isErr - does error occur
	 * @param errMsg - msg or info about that error
	 * @param disableLng - if we want disable option to change language
	 * @param isSave - it's after save action
	 */
	private static void setModel(Model model, LicenseFormBean licenseForm, Boolean isErr, String errMsg, Boolean disableLng, boolean isSave) {
		//Informing FE what key to use, when creating page
		if(isSave)
			// page /admin/setup/license
			model.addAttribute("isLicenseSave", true);
		else
			// page /admin/setup/license_saved
			model.addAttribute("isLicense", true);

		//Object that will be used in filling license form
		model.addAttribute("licenseForm", licenseForm);
		//Style file content for page
		model.addAttribute("cmpCss", FileTools.readFileContent("/components/cmp.css"));
		//Indicator if error occur
		model.addAttribute("licenseErr", isErr);
		//Text of that error
		model.addAttribute("licenseErrMsg", errMsg);
		//If true, user will not see select to change language
		model.addAttribute("disableLng", disableLng);
	}
}
