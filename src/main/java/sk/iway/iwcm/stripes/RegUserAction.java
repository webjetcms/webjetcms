package sk.iway.iwcm.stripes;

import java.awt.Dimension;
import java.io.File;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import jakarta.servlet.http.HttpServletRequest;

import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.SimpleMessage;
import net.sourceforge.stripes.util.bean.BeanUtil;
import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.ValidationErrors;
import net.sourceforge.stripes.validation.ValidationMethod;
import sk.iway.Html2Text;
import sk.iway.Password;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.PageParams;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.SpamProtection;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.LogonTools;
import sk.iway.iwcm.common.UserTools;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.ShowDoc;
import sk.iway.iwcm.gallery.GalleryDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFsDB;
import sk.iway.iwcm.system.captcha.Captcha;
import sk.iway.iwcm.system.stripes.WebJETActionBean;
import sk.iway.iwcm.tags.support.ResponseUtils;
import sk.iway.iwcm.users.AuthorizeAction;
import sk.iway.iwcm.users.PasswordSecurity;
import sk.iway.iwcm.users.PasswordsHistoryDB;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UserGroupDetails;
import sk.iway.iwcm.users.UserGroupsDB;
import sk.iway.iwcm.users.UsersDB;

/**
 *  RegUserAction.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2007
 *@author       $Author: jraska $
 *@version      $Revision: 1.8 $
 *@created      Date: 16.10.2007 16:28:45
 *@modified     $Date: 2010/02/05 14:40:41 $
 */
public class RegUserAction extends WebJETActionBean
{
	public static final String REQUIRE_AUTHORIZATION_AFTER_VERIFICATION = "requireAuthorizationAfterVerification";

	UserDetails usr = null;
	FileBean userImage = null;

	/**
	 * Priprav udaje pouzivatela na zobrazenie vo formulari
	 */
	@Override
	public void setContext(ActionBeanContext context)
	{
		super.setContext(context);

		Identity loggedUser = getCurrentUser();

		usr = new UserDetails();
		Logger.debug(RegUserAction.class, "Seesion: "+getSession());
		if (loggedUser != null)
		{
			Logger.error(this,"mam lognuteho usera");
			int id = Tools.getIntValue(context.getRequest().getParameter("usr.userId"), -1);
			if (getSession().getAttribute("RegUserAction.ALLOW_SAVE_OTHER_USER")!=null && id > 0)
			{
				usr = UsersDB.getUser(id);
				//getSession().removeAttribute("RegUserAction.ALLOW_SAVE_OTHER_USER");
				getRequest().setAttribute("RegUserAction.ALLOW_SAVE_OTHER_USER", "true");
			}
			else
			{
				usr = UsersDB.getUser(loggedUser.getUserId());
			}
		}

		//toto tu treba kvoli UsersDB.getUser(loggedUser.getUserId()); - ak bol medzicasom zmazany z DB
		if (usr == null) usr = new UserDetails();

		//nastavime hodnotu hesla na nezmenenu
		if (Tools.isNotEmpty(usr.getPassword()))
		{
			usr.setPassword(UserTools.PASS_UNCHANGED);
		}
	}

	/**
	 * Uloz udaje pouzivatela do databazy
	 * @return
	 */
	public Resolution bSubmit()
	{
		//tato metoda berie hodnotu podla session, takze sa pouzije spravny jazyk
		Prop prop = Prop.getInstance(PageLng.getUserLng(getContext().getRequest()));

		Logger.debug(RegUserAction.class, "---> SAVE");
		PageParams pageParams = new PageParams(getRequest());
		if (usr != null)
		{
			String groupIdsEditable = pageParams.getValue("groupIdsEditable", "");
			if (Tools.isNotEmpty(groupIdsEditable))
			{
				StringTokenizer st = new StringTokenizer(groupIdsEditable, ",+; ");
				while (st.hasMoreTokens())
				{
					int userGroupId = Tools.getIntValue(st.nextToken(), -1);
					if (userGroupId>0)
					{
						usr.removeFromGroup(userGroupId);
					}
				}
			}

			boolean requireApprove = false;
			boolean requireEmailVerificationByGroup = false;

			//MBO: po potvrdeni mailu, treba aby ho autorizoval admin
			boolean requireAuthorizationAfterVerification = pageParams.getBooleanValue("requireAuthorizationAfterVerification", false);

			String groupIds = pageParams.getValue("groupIds", "1");
			//pridame mu pozadovane skupiny
			UserGroupsDB userGroupsDB = UserGroupsDB.getInstance(Constants.getServletContext(), true, "iwcm");
			StringTokenizer st = new StringTokenizer(groupIds, ",+; ");
			while (st.hasMoreTokens())
			{
				int userGroupId = Tools.getIntValue(st.nextToken(), -1);
				if (userGroupId>0)
				{
					UserGroupDetails ugd = userGroupsDB.getUserGroup(userGroupId);
					if (ugd!=null && ugd.isAllowUserEdit())
					{
						usr.addToGroup(userGroupId);
						if (usr.getUserId()<1 && ugd.isRequireApprove()) requireApprove = true;
						if (usr.getUserId()<1 && ugd.isRequireEmailVerification()) requireEmailVerificationByGroup = true;
					}
				}
			}

			//pridame skupiny podla preferencii z formularu (ak nejake boli zadane)
			String[] ugID = getRequest().getParameterValues("user_group_id");
			if (ugID != null && ugID.length > 0)
			{
				for (int i=0; i<ugID.length; i++)
				{
					int userGroupId = Tools.getIntValue(ugID[i], -1);
					if (userGroupId>0)
					{
						UserGroupDetails ugd = userGroupsDB.getUserGroup(userGroupId);
						if (ugd!=null && ugd.isAllowUserEdit())
						{
							usr.addToGroup(userGroupId);
							if (usr.getUserId()<1 && ugd.isRequireApprove()) requireApprove = true;
						}
						else if (ugd!=null)
						{
							//aby sa nestalo, ze ho chcu zaregistrovat do nejakej skupiny, ktora ma nastavene approve ale zabudli nastavit allow user edit
							if (usr.getUserId()<1 && ugd.isRequireApprove()) requireApprove = true;
						}
					}
				}
			}

            //user "dedi" skupiny po userovi ktory ho vytvara
            if(getSession().getAttribute("RegUserAction.REMOVE_USER_GROUPS") != null)
            {
                String[] removeFromGroups = Tools.getTokens(getSession().getAttribute("RegUserAction.REMOVE_USER_GROUPS")+"", ",");
                for(String groupToRemove:removeFromGroups)
                {
                    usr.removeFromGroup(Tools.getIntValue(groupToRemove, -1));
                }
            }

            if(getSession().getAttribute("RegUserAction.ADD_USER_GROUPS") != null)
            {
                String[] addFromGroups = Tools.getTokens(getSession().getAttribute("RegUserAction.ADD_USER_GROUPS")+"", ",");
                for(String groupToAdd:addFromGroups)
                {
                    usr.addToGroup(Tools.getIntValue(groupToAdd, -1));
                }
            }

			boolean requireEmailVerification = pageParams.getBooleanValue("requireEmailVerification", false);
			if (requireApprove) requireEmailVerification = false;

			if (requireEmailVerificationByGroup)
			{
				//tak predsa ano :)
				requireEmailVerification=true;
			}

			if ("true".equals(getSession().getAttribute("RegUserAction.ALLWAYS_AUTHORIZE")))
			{
				//toto tu je, ked sa cez web mozu spravovat pouzivatelia, vtedy po vytvoreni nechcem ani poslat email
				requireApprove = false;
				//getSession().removeAttribute("RegUserAction.ALLWAYS_AUTHORIZE");
			}

			usr.setAuthorized(!requireApprove);
			if (usr.getUserId()<1 && requireEmailVerification) usr.setAuthorized(false);
			boolean isNewUser = true;
			if (usr.getUserId()>0) isNewUser = false;

			if (usr.getUserId()<1) Adminlog.add(Adminlog.TYPE_USER_INSERT, "New user :"+ "id= " + usr.getUserId() + "login= " + usr.getLogin()+	"name= " + usr.getFirstName() +" "+usr.getLastName(), usr.getUserId(), -1);
			else  Adminlog.add(Adminlog.TYPE_USER_SAVE, "Update user :"+ "id= " + usr.getUserId() + "login= " + usr.getLogin()+	"name= " + usr.getFirstName() +" "+usr.getLastName(), usr.getUserId(), -1);

			uploadFileProcedure();

			if (requireAuthorizationAfterVerification)
			{
//				ulozi priznak do volneho pola
				String infoemail = pageParams.getValue("infoemail", "web@interway.sk");
				usr.setFieldE(REQUIRE_AUTHORIZATION_AFTER_VERIFICATION+"-"+infoemail);
			}
			boolean saveOK = UsersDB.saveUser(usr);

			String originalPassword = "";
			if (isNewUser) {
				originalPassword = usr.getPassword();
			}

			String messageKey = null;

			if (saveOK)
			{
				//ak mam defaultneho pouzivatela, zakazem podla neho pristupy na moduly
				if(isNewUser)
				{
					int defaultUserId = Constants.getInt("regUserDefaultUserId");
					if(defaultUserId > 0)
					{
						UserDetails defaultUser = UsersDB.getUser(defaultUserId);
						if(defaultUser != null && defaultUser.getUserId() > 0) copyUserDisabledItems(defaultUser.getUserId(), usr.getUserId());
					}
				}

				//treba poslat notifikaciu / schvalenie?
				String infoemail = pageParams.getValue("infoemail", "");

				//aby sa v info maili neposlala info o schvaleni
				if (isNewUser && requireEmailVerification) usr.setAuthorized(true);
				if (isNewUser && Tools.isEmail(infoemail) && !requireAuthorizationAfterVerification) sendEmailToAdmin(infoemail);
				if (isNewUser && requireEmailVerification) usr.setAuthorized(false);

				//updatni usera v session
				updateLoggedUser();

				boolean doNotLoginUser = pageParams.getBooleanValue("doNotLoginUser", false);
				if (pageParams.hasParameter("loginNewUser")) doNotLoginUser = !pageParams.getBooleanValue("loginNewUser", true);
				//existing users are allready logged
				if (isNewUser == false) doNotLoginUser = true;
				boolean sendUserWelcomeEmail = false;
				if (usr.isAuthorized())
				{
					if (isNewUser) {
						if (doNotLoginUser==false)
						{
							Identity user = (Identity)getSession().getAttribute(Constants.USER_KEY);
							if (user == null) LogonTools.logonUser(getRequest(), usr.getLogin(), originalPassword);
						}
						sendUserWelcomeEmail = true;

						if (doNotLoginUser==false)
						{
							messageKey = "components.user.newuser.saveSuccess_logged";
						}
						else
						{
							messageKey = "components.user.newuser.saveSuccess_notlogged";
						}
					} else {
						messageKey = "components.forum.bb.profile.save_success";
					}
				}
				else
				{
					//ak treba posli mu mail o registracii a ze to bude schvalene niekedy
					int notAuthorizedEmailDocId = pageParams.getIntValue("notAuthorizedEmailDocId", -1);
					if (isNewUser)
					{
						if (notAuthorizedEmailDocId>0)
						{
							sendNotAuthorizedInfoEmail(usr.getUserId(), originalPassword, notAuthorizedEmailDocId, getRequest());
							messageKey = "components.user.newuser.auth_email_sent";
						}
						else if (requireEmailVerification)
						{
							// MBO: aby som to jsp mohol mat includnute v stranke
							String authUrl = Tools.getBaseHref(getRequest()) + pageParams.getValue("emailVerificationAuthorizationLink", "/components/user/authorize.jsp");


							int originalDocId = Tools.getDocId(getRequest());
							if (originalDocId<1) originalDocId = pageParams.getIntValue("originalDocId", -1);
							if (originalDocId > 0)
							{
								String url = DocDB.getURLFromDocId(originalDocId, getRequest());
								authUrl = Tools.getBaseHref(getRequest()) + url;
							}
							authUrl = Tools.addParametersToUrlNoAmp(authUrl, "userId=!LOGGED_USER_ID!&hash=!AUTHORIZE_HASH!");

							DocDetails authDoc = new DocDetails();

							authDoc.setAuthorId(usr.getUserId());
							authDoc.setAuthorEmail(usr.getEmail());
							authDoc.setAuthorName(usr.getFirstName()+" "+usr.getLastName());

							authDoc.setTitle(prop.getText("components.user.requireEmailVerification.subject", Tools.getBaseHref(getRequest())));
							authDoc.setData(prop.getText("components.user.requireEmailVerification.body", Tools.getBaseHref(getRequest()), authUrl));

							boolean sendOK = sendNotAuthorizedInfoEmail(usr.getUserId(), originalPassword, authDoc, getRequest());
							if (sendOK) messageKey = "components.user.newuser.user_auth_email_sent";
							else messageKey = "components.user.newuser.user_auth_email_NOTsent";
						}
						else
						{
							messageKey = "components.user.newuser.auth_email_sent";
						}
					} else {
						messageKey = "groupedit.require_approve";
					}
				}

				if (messageKey != null)
				{
					getContext().getMessages().add(new SimpleMessage(prop.getText(messageKey)));
					getRequest().setAttribute("regUserMessageKey", messageKey);
				}
				getRequest().setAttribute("RegUserActionsaveOK", "true");


				//zapis redirect pre ajax
				int successDocId = pageParams.getIntValue("successDocId", -1);
				if (successDocId > 0)
				{
					String ajaxRedirectCode = "\nwindow.location.href='"+DocDB.getInstance().getDocLink(successDocId, getRequest())+"';\n";
					getRequest().setAttribute("ajaxRedirectCode", ajaxRedirectCode);
				}

				// after save interceptor
				String interceptorClassName = Tools.getStringValue(Constants.getString("stripesUserAfterSaveClass"),"");
				if (Tools.isEmpty(interceptorClassName))
				{
					//ak nebol zadany nazov triedy, kukni do PP
					interceptorClassName = pageParams.getValue("afterSaveInterceptor", "");
				}

				if (Tools.isNotEmpty(interceptorClassName))
				{
					try
					{
						@SuppressWarnings("unchecked")
						Class<? extends AfterRegUserSaveInterceptor> interceptorClass = (Class<? extends AfterRegUserSaveInterceptor>) Class.forName(interceptorClassName);
						AfterRegUserSaveInterceptor interceptor = interceptorClass.getDeclaredConstructor().newInstance();
						if (interceptor.intercept(usr, getRequest()))
						{
							//nastala zmena usera, uloz ho a updatni v session
							UsersDB.saveUser(usr);
							updateLoggedUser();
						}
						if (interceptor.shouldSendUserWelcomeEmail()!=null) sendUserWelcomeEmail = interceptor.shouldSendUserWelcomeEmail().booleanValue();
					}
					catch (Exception e)
					{
						sk.iway.iwcm.Logger.error(e);
					}
				}

				if (isNewUser && sendUserWelcomeEmail) {
					//posli email
					AuthorizeAction.sendInfoEmail(usr.getUserId(), originalPassword, null, getRequest());
				}
			}
			else
			{
				getRequest().setAttribute("errorText", prop.getText("components.user.newuser.errorSavingUser"));
			}
		}
		Logger.debug(RegUserAction.class, "---> SAVE - OK");
		return (new ForwardResolution("/components/maybeError.jsp"));
	}

	private void sendEmailToAdmin(String infoemail)
	{
		//id a heslo schvalovatela
		int uid = -1;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();

			ps = db_conn.prepareStatement("SELECT user_id, password FROM  users WHERE email=? AND is_admin="+DB.getBooleanSql(true)+UsersDB.getDomainIdSqlWhere(true)+ " ORDER BY user_id ASC");
			ps.setString(1, infoemail);
			rs = ps.executeQuery();
			if (rs.next())
			{
				uid = rs.getInt("user_id");
			}
			rs.close();
			ps.close();

			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		try
		{
			Prop prop = Prop.getInstance(getRequest());

			String url = Tools.getBaseHref(getRequest()) + "/admin";

			String message = prop.getText("reguser.new_registration") + "<br/>";

			if (!usr.isAuthorized())
			{
				message += prop.getText("reguser.need_approve") + " ";
				if (usr.getUserId() > 0 && uid > 0)
				{
					message += prop.getText("reguser.clink_on_link") + ":<br/>";
					String authUrl = url + "/v9/users/user-list/#dt-filter-id=" + usr.getUserId() + "&dt-select=true";
					message += "<a href='"+authUrl+"'>"+authUrl+"</a><br/><br/>";
				}
				else
				{
					message += "<br/>";
				}
			}

			message += "   " + prop.getText("reguser.firstname") + ": " + usr.getFirstName() + "<br/>";
			message += "   " + prop.getText("reguser.lastname") + ": " + usr.getLastName() + "<br/>";
			if (Tools.isNotEmpty(usr.getCompany())) message += "   " + prop.getText("reguser.company") + ": " + usr.getCompany() + "<br/>";
			if (Tools.isNotEmpty(usr.getAdress())) message += "   " + prop.getText("reguser.street") + ": " + usr.getAdress() + "<br/>";
			if (Tools.isNotEmpty(usr.getCity())) message += "   " + prop.getText("reguser.city") + ": " + usr.getCity() + "<br/>";
			if (Tools.isNotEmpty(usr.getZip())) message += "   " + prop.getText("reguser.zip") + ": " + usr.getZip() + "<br/>";
			if (Tools.isNotEmpty(usr.getCountry())) message += "   " + prop.getText("reguser.country") + ": " + usr.getCountry() + "<br/>";
			message += "   " + prop.getText("reguser.email") + ": " + usr.getEmail() + "<br/>";
			if (Tools.isNotEmpty(usr.getPhone())) message += "   " + prop.getText("reguser.phone") + ": " + usr.getPhone() + "<br/>";

			//skupiny kam je zaregistrovany
			UserGroupsDB ugDB = UserGroupsDB.getInstance();
			if (Tools.isNotEmpty(usr.getUserGroupsIds())) message += "   " + prop.getText("user.admin.userGroups") + ": " + ugDB.convertIdsToNames(usr.getUserGroupsIds()) + "<br/>";

			message += "<br/><br/>";

			String toEmail = infoemail;
			String fromName = usr.getFullName();

			String subject = prop.getText("reguser.subject");

			//tu sa nepouzije fromEmail, pretoze ak je email nedostupny
			//vrati sa ziadatelovi delivery failed vratane linky na schvalenie...
			SendMail.send(fromName, toEmail, toEmail, subject, message);
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
	}

	/**
	 * Aktualizuje prihlaseneho pouzivatela podla formu
	 */
	private void updateLoggedUser()
	{
		Identity userIdentity = getCurrentUser();
		if (userIdentity!=null && userIdentity.getUserId()==usr.getUserId())
		{
			//updatni usera
			userIdentity.setTitle(usr.getTitle());
			userIdentity.setFirstName(usr.getFirstName());
			userIdentity.setLastName(usr.getLastName());
			userIdentity.setLoginName(usr.getLogin());
			userIdentity.setCompany(usr.getCompany());
			userIdentity.setAdress(usr.getAdress());
			userIdentity.setPSC(usr.getPSC());
			userIdentity.setCountry(usr.getCountry());
			userIdentity.setEmail(usr.getEmail());
			userIdentity.setPhone(usr.getPhone());
			userIdentity.setCity(usr.getCity());
			userIdentity.setFieldA(usr.getFieldA());
			userIdentity.setFieldB(usr.getFieldB());
			userIdentity.setFieldC(usr.getFieldC());
			userIdentity.setFieldD(usr.getFieldD());
			userIdentity.setFieldE(usr.getFieldE());

			userIdentity.setDateOfBirth(usr.getDateOfBirth());
			userIdentity.setSexMale(usr.isSexMale());
			userIdentity.setPhoto(usr.getPhoto());
			userIdentity.setSignature(Html2Text.html2text(usr.getSignature()));

			if (usr.getPassword().length() > 1 && usr.getPassword().compareToIgnoreCase(UserTools.PASS_UNCHANGED) != 0)
			{
				usr.setPassword(usr.getPassword());
			}
			userIdentity.setFax(usr.getFax());
			userIdentity.setDeliveryCity(usr.getDeliveryCity());
			userIdentity.setDeliveryCompany(usr.getDeliveryCompany());
			userIdentity.setDeliveryCountry(usr.getDeliveryCountry());
			userIdentity.setDeliveryFirstName(usr.getDeliveryFirstName());
			userIdentity.setDeliveryLastName(usr.getDeliveryLastName());
			userIdentity.setDeliveryPhone(usr.getDeliveryPhone());
			userIdentity.setDeliveryPsc(usr.getDeliveryPsc());
			userIdentity.setDeliveryAdress(usr.getDeliveryAdress());

			userIdentity.setUserGroupsIds(usr.getUserGroupsIds());

			userIdentity.setPosition(usr.getPosition());
			userIdentity.setParentId(usr.getParentId());
		}
	}

	@ValidationMethod(on="bSubmit")
	public void validation(ValidationErrors errors)
	{
		Logger.debug(RegUserAction.class, "validation, errors="+errors);
		Prop prop = Prop.getInstance(PageLng.getUserLng(getRequest()));

		PageParams pageParams = new PageParams(getRequest());

		//toto tu musi byt, inak asi exspirovala session
		if (pageParams.hasParameter("groupIds")==false)
		{
			errors.add("sessionExpired", new SimpleError(prop.getText("pageparams.session_expired")));
		}

		if (ShowDoc.getXssRedirectUrl(getRequest())!=null)
		{
			errors.add("xssError", new SimpleError(prop.getText("system.xss.xss_notify")));
		}

		if (!SpamProtection.canPost("userform", null, getRequest()))
			errors.add("spamError", new SimpleError(prop.getText("checkform.fail_probablySpamBot")));

		if	(!Captcha.validateResponse(getRequest(), getRequest().getParameter("g-recaptcha-response"), "userform"))
			errors.add("captchaError", new SimpleError(prop.getText("captcha.nie.je.spravna")));


		String validateMethod = pageParams.getValue("validateMethod", null);
		if (Tools.isNotEmpty(validateMethod))
		{
			int i = validateMethod.lastIndexOf('.');
			String validateClass = validateMethod.substring(0, i);
			validateMethod = validateMethod.substring(i+1);
			//String
			try
			{
				Class<?> c = Class.forName(validateClass);
				Object o = c.getDeclaredConstructor().newInstance();
				Method m;
				Class<?>[] parameterTypes = new Class[] {RegUserAction.class, ValidationErrors.class};
				Object[] arguments = new Object[] {this, errors};
				m = c.getMethod(validateMethod, parameterTypes);
				m.invoke(o, arguments);
				Logger.println(RegUserAction.class, "errors="+errors);
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}
		}

		if (UserTools.USE_EMAIL_AS_LOGIN.equals(usr.getLogin()))
		{
			usr.setLogin(usr.getEmail());
		}

		//kontrola povinnych poli

		String required = pageParams.getValue("required","login,password,firstName,lastName,email");
		if (Tools.isEmpty(required)) required = "login,password,email";

		//password2 nie je v UserDetails
		required = Tools.replace(required, "password2+", "");

		StringTokenizer st = new StringTokenizer(required, ",+;");
		while (st.hasMoreTokens())
		{
			String property = st.nextToken();
			try
			{
				if (Tools.isNotEmpty(property))
				{
					String value = (String)BeanUtil.getPropertyValue(property, usr);
					if (Tools.isEmpty(value))
					{
						errors.add("usr."+property, new SimpleError(Tools.replace(prop.getText("validation.required.valueNotPresent"), "{0}", prop.getText("components.user.newuser."+property))));
					}
				}
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}
		}

		if (Tools.isNotEmpty(usr.getEmail()) && (usr.getEmail().indexOf('@') == -1 || usr.getEmail().indexOf('.') == -1))
		{
			errors.add("user.email", new SimpleError(prop.getText("userForm.err.email")));
		}

        if (getRequest().getAttribute("RegUserAction.ALLOW_SAVE_OTHER_USER")==null)
        {
            // pre akukolvek zmenu pozaduj stare heslo
            if (this.usr.getUserId() > 0)
            {
                try
                {
                    sk.iway.Password pass = new sk.iway.Password();
					String passwordHashDB = (new SimpleQuery()).forString("SELECT password FROM users WHERE user_id = ?"+UsersDB.getDomainIdSqlWhere(true), this.usr.getUserId());
					String passwordSaltDB = (new SimpleQuery()).forString("SELECT password_salt FROM users WHERE user_id = ?"+UsersDB.getDomainIdSqlWhere(true), this.usr.getUserId());
                    if (!this.usr.isInUserGroup(Constants.getInt("socialMediaUserGroupId",-1))) {
						if (Tools.isEmpty(this.usr.getOldPassword())) {
							errors.add("user.oldPassword", new SimpleError(prop.getText("components.user.newuser.wrong.oldPassword")));
						} else if (!(pass.encrypt(this.usr.getOldPassword()).equals(passwordHashDB) || PasswordSecurity.isPasswordCorrect(this.usr.getOldPassword(), passwordSaltDB, passwordHashDB))) { //NOSONAR
							//nerozlisujeme typ chyby
							errors.add("user.oldPassword", new SimpleError(prop.getText("components.user.newuser.wrong.oldPassword")));
						}
					}
                } catch (Exception ex)
                {
                    sk.iway.iwcm.Logger.error(ex);
                }
            }
        }

		//pokial je heslo zadane, skontroluj ho podla nastavenych podmienok
		if (Tools.isNotEmpty(usr.getPassword()))
		{
	    	//kontrola hesla podla nastavenych podmienok
			String constStr = "";
			if(usr.isAdmin())
			{
				constStr = "Admin";
			}
			int dlzkaHesla = Constants.getInt("password"+constStr+"MinLength");
			int pocetZnakov = Constants.getInt("password"+constStr+"MinCountOfSpecialSigns");
			int pocetVelkychPismen = Constants.getInt("password"+constStr+"MinUpperCaseLetters");
			int pocetMalychPismen = Constants.getInt("password"+constStr+"MinLowerCaseLetters");
			int pocetCisel = Constants.getInt("password"+constStr+"MinCountOfDigits");

			if(Password.checkPassword(false, usr.getPassword(), usr.isAdmin(), usr.getUserId(), null, null) && Password.equalsPasswords(usr.getPassword(), getRequest().getParameter("password2")))
			{
				//ulozim iba zmenene hesla
				if(usr.getPassword().trim().compareToIgnoreCase(UserTools.PASS_UNCHANGED) != 0)
				{
					Adminlog.add(Adminlog.TYPE_USER_CHANGE_PASSWORD, usr.getUserId(), "RegUserAction - user ("+usr.getLogin()+") successfully changed password", -1, -1);
				}
			}
			else
			{
				String errorText = prop.getText("logon.change_password.nesplna_nastavenia")+"<br/>";
				if(dlzkaHesla > 0)
					errorText += "- "+prop.getText("logon.change_password.min_length", String.valueOf(dlzkaHesla))+".<br/>";
				if(pocetVelkychPismen > 0)
					errorText += "- "+prop.getText("logon.change_password.count_of_upper_case", String.valueOf(pocetVelkychPismen))+".<br/>";
				if(pocetMalychPismen > 0)
					errorText += "- "+prop.getText("logon.change_password.count_of_lower_case", String.valueOf(pocetMalychPismen))+".<br/>";
				if(pocetCisel > 0)
					errorText += "- "+prop.getText("logon.change_password.count_of_digits", String.valueOf(pocetCisel))+".<br/>";
				if(pocetZnakov > 0)
					errorText += "- "+prop.getText("logon.change_password.count_of_special_sign", String.valueOf(pocetZnakov))+".<br/>";
				if(!Password.equalsPasswords(usr.getPassword(), getRequest().getParameter("password2")))
					errorText += "- "+prop.getText("logon.change_password.password2")+".<br/>";
				if(PasswordsHistoryDB.getInstance().existsPassword(usr.getPassword(),usr.getUserId()))
					errorText += "- "+prop.getText("logon.change_password.used_in_history2")+".<br/>";
				errors.add("passwordError", new SimpleError(errorText));
			}
			//END kontrola hesla
		}

		//ak je nastavene heslo na text random, tak sa nahodne vygeneruje
		if ("random".equals(usr.getPassword()))
		{
			//vygeneruj heslo
			String password = Password.generatePassword(5);
			getRequest().getSession().setAttribute("randomGeneratedPassword", password);
			usr.setPassword(password);
		}

		//aby vypisalo hlasku uz pri pokuse o zmenu mailu na taky ktory uz existuje
		if("cloud".equals(Constants.getInstallName()) )
		{
			UserDetails userDetails = UsersDB.getUser(usr.getEmail());
			if(userDetails  != null && userDetails.getUserId() != usr.getUserId())
			{
				errors.add("user.email",new SimpleError(prop.getText("users.import.email_allready_exists", usr.getEmail())));
			}
		}

		//kontrola ci to je vobec email
		if (required.contains("email") && !Tools.isEmail(usr.getEmail()))
			errors.add("user.email", new SimpleError(prop.getText("checkform.title.email")));

		//kontrola ci to je vobec telefonne cislo
		if (required.contains("phone") && !Tools.isPhoneNumber(usr.getPhone()))
			errors.add("usr.phone", new SimpleError(prop.getText("components.user.newuser.wrong.phone")));


		//kontrola na existujuci login
		boolean emailUnique = pageParams.getBooleanValue("emailUnique", false);

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();

			int psCounter = 1;

			String sql = "SELECT user_id FROM  users WHERE login=?";
			if (emailUnique) sql = "SELECT user_id FROM  users WHERE (login=? OR email=?)";
			if (usr.getUserId()>0) sql +=" AND user_id<>?"+UsersDB.getDomainIdSqlWhere(true);
			ps = db_conn.prepareStatement(sql);
			ps.setString(psCounter++, usr.getLogin());
			if (emailUnique) ps.setString(psCounter++, usr.getEmail());
			if (usr.getUserId()>0)  ps.setInt(psCounter++, usr.getUserId());
			rs = ps.executeQuery();

			if (rs.next())
			{
				Logger.debug(RegUserAction.class, "RS userId="+rs.getInt("user_id"));
				//uz existuje
				if (usr.getLogin().equals(usr.getEmail()) || emailUnique)
				{
					//login je ako email, ako login sa asi pouziva email adresa
					errors.add("user.email", new SimpleError(prop.getText("userForm.err.email_in_use")));
				}
				else
				{
					errors.add("user.login", new SimpleError(prop.getText("userForm.err.login_in_use")));
				}
			}

			rs.close();
			ps.close();
			db_conn.close();

			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		if (errors.size()>0 && userImage != null && userImage.getFileName() != null && userImage.getFileName().length() > 0)
		{
			String fileName = userImage.getFileName().toLowerCase();
			errors.add("userImage", new SimpleError(prop.getText("components.user.newuser.userImage.submitFailed", ResponseUtils.filter(fileName), ""+userImage.getSize())));
		}

		Logger.debug(RegUserAction.class, "validation koniec, errors="+errors);
	}

	/**
	 * Uload fotografie pouzivatela
	 * @return
	 */
	private boolean uploadFileProcedure()
	{
		boolean ret = false;
		try
		{
			if (usr != null && userImage != null)
			{
				Logger.debug(RegUserAction.class, "uploadFileProcedure, file="+userImage.getFileName());
				//Logger.println(this,"Image uploading...");
				//retrieve the file name
				String fileName = userImage.getFileName().toLowerCase();

				if (fileName != null && (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".gif") || fileName.endsWith(".png")))
				{
					//premenujem subor
					fileName = usr.getLogin()+ ".jpg";

					String realPath = null;

					boolean dirExists = false;
					String actualDir = Constants.getString("imagesRootDir") + "/" + sk.iway.iwcm.Constants.getString("galleryDirName") + "/user";
					String dirRealPath = Tools.getRealPath(actualDir);
					if (dirRealPath != null)
					{
						File fileDir = new File(dirRealPath);
						if (fileDir.exists() && fileDir.isDirectory())
						{

						}
						else
						{
							dirExists = fileDir.mkdirs();
						}

						if (dirExists)
						{
							Logger.println(RegUserAction.class,"Dir created");
						}
					}

					Prop prop = Prop.getInstance(Constants.getServletContext(), getRequest());
					Dimension[] dims = GalleryDB.getDimension(actualDir);

					//ak sa jedna o obrazok
					if (fileName.length() > 4)
					{
						realPath = Tools.getRealPath(actualDir+"/"+fileName); //NOSONAR
						if (realPath != null)
						{
							File f = new File(realPath);
							if (f.exists())
							{

							}
							Logger.debug(RegUserAction.class, "Saving photo to:"+f.getAbsolutePath());

							IwcmFsDB.writeFiletoDest(userImage.getInputStream(),f,(int)userImage.getSize());

							//ak existuje zmaz subor o_nazov, lebo by sa to znova updatlo zo stareho
							File origFile = new File(Tools.getRealPath(actualDir+"/o_"+fileName));
							if (origFile.exists())
							{
								Logger.debug(RegUserAction.class, "Deleting orig file: "+origFile.getAbsolutePath());
								if(origFile.delete() == false) return false;
							}

							//pridaj ho do galerie
							Logger.debug(RegUserAction.class, "Resizing photo:"+realPath);
							GalleryDB.resizePictureImpl(dims, realPath, null, prop, GalleryDB.getResizeMode(actualDir));

							usr.setPhoto(actualDir+ "/" +fileName);
						}
					}
				}

				ret = true;
			}

		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		return (ret);
	}

	private static void copyUserDisabledItems(int originalUserId, int newUserId)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			Map<String, String> permsTable = new Hashtable<>();

			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT item_name FROM user_disabled_items WHERE user_id=?");
			ps.setInt(1, originalUserId);
			rs = ps.executeQuery();
			while (rs.next())
			{
				permsTable.put(DB.getDbString(rs, "item_name"), "1");
			}
			rs.close();
			ps.close();

			ps = db_conn.prepareStatement("DELETE FROM user_disabled_items WHERE user_id=?");
			ps.setInt(1, newUserId);
			ps.execute();
			ps.close();

			ps = db_conn.prepareStatement("INSERT INTO user_disabled_items (user_id, item_name) VALUES (?, ?)");
			ps.setInt(1, newUserId);

			Iterator<String> keys = permsTable.keySet().iterator();
			while (keys.hasNext())
			{
				String itemName = keys.next();

				ps.setString(2, itemName);
				ps.execute();

				Logger.debug(RegUserAction.class, "copyUserDisabledItems userId="+newUserId+" itemName="+itemName);
			}

			ps.close();

			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
	}

	public UserDetails getUsr()
	{
		return usr;
	}

	public void setUser(UserDetails userDetails)
	{
		this.usr = userDetails;
	}

	public FileBean getUserImage()
	{
		return userImage;
	}

	public void setUserImage(FileBean userImage)
	{
		this.userImage = userImage;
	}

	/**
	 * Odosle email o tom, ze sa caka na autorizaciu
	 * @param userId
	 * @param docId
	 * @param request
	 * @return
	 */
	public static boolean sendNotAuthorizedInfoEmail(int userId, String password, int docId, HttpServletRequest request)
	{
		boolean emailSend = false;
		try
		{
			Logger.debug(RegUserAction.class, "sendNotAuthorizedInfoEmail - userId="+userId);

			DocDB docDB = DocDB.getInstance();
			DocDetails doc = docDB.getDoc(docId);

			return sendNotAuthorizedInfoEmail(userId, password, doc, request);
		}
		catch (Exception ex)
		{
			emailSend = false;
			sk.iway.iwcm.Logger.error(ex);
		}
		return(emailSend);
	}

	/**
	 * Odosle email o tom, ze sa caka na autorizaciu
	 * @param userId
	 * @param password
	 * @param doc
	 * @param request
	 * @return
	 */
	public static boolean sendNotAuthorizedInfoEmail(int userId, String password, DocDetails doc, HttpServletRequest request)
	{
		boolean emailSend = false;
		try
		{
			Logger.debug(RegUserAction.class, "sendNotAuthorizedInfoEmail - userId="+userId);

			UserDetails uform = UsersDB.getUser(userId);

			if (doc!=null && uform.getEmail().length() > 5 && uform.getEmail().indexOf('@') > 1)
			{
				String body = doc.getData();
				String subject = doc.getTitle();

				//	replacni !BR! za \n
				body = Tools.replace(body, "!BR!", "\n");
				body = Tools.replace(body, "!LOGIN_NAME!", uform.getLogin());
				body = Tools.replace(body, "!LOGGED_USER_LOGIN!", uform.getLogin());

				if(!Constants.getBoolean("passwordUseHash"))
				{
					body = Tools.replace(body, "!PASSWORD!", uform.getPassword());
					body = Tools.replace(body, "!LOGGED_USER_PASSWORD!", uform.getPassword());
				}
				else if(Tools.isNotEmpty(password))
				{
					body = Tools.replace(body, "!PASSWORD!", password);
					body = Tools.replace(body, "!LOGGED_USER_PASSWORD!", password);
				}

				body = Tools.replace(body, "!TITLE!", uform.getTitle());
				body = Tools.replace(body, "!NAME!", uform.getFullName());
				body = Tools.replace(body, "!FIRST_NAME!", uform.getFirstName());
				body = Tools.replace(body, "!LAST_NAME!", uform.getLastName());
				body = Tools.replace(body, "!LOGGED_USER_NAME!", uform.getFullName());

				body = Tools.replace(body, "!LOGGED_USER_EMAIL!", uform.getEmail());
				body = Tools.replace(body, "!LOGGED_USER_COMPANY!", uform.getCompany());
				body = Tools.replace(body, "!LOGGED_USER_CITY!", uform.getCity());
				body = Tools.replace(body, "!LOGGED_USER_COUNTRY!", uform.getCountry());
				body = Tools.replace(body, "!LOGGED_USER_PHONE!", uform.getPhone());
				body = Tools.replace(body, "!LOGGED_USER_ZIP!", uform.getPSC());
				body = Tools.replace(body, "!LOGGED_USER_ID!", Integer.toString(uform.getUserId()));
				String authorizeHash  = UsersDB.getGenerateAuthorizeHash(uform.getUserId());
				String hash = authorizeHash != null ? authorizeHash : "";
				body = Tools.replace(body, "!AUTHORIZE_HASH!", hash);

				//uprav relativne cesty
				body = SendMail.createAbsolutePath(body, request);

				//String body2 = new String(body.getBytes("windows-1250"));
				emailSend = SendMail.send(doc.getAuthorName(), doc.getAuthorEmail(), uform.getEmail(), subject, body);
			}
		}
		catch (Exception ex)
		{
			emailSend = false;
			sk.iway.iwcm.Logger.error(ex);
		}
		return(emailSend);
	}
}
