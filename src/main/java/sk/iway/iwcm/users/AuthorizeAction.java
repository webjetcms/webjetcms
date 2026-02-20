
package sk.iway.iwcm.users;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.StringTokenizer;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.RandomStringUtils;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.stripes.RegUserAction;


/**
 *  Autorizácia registrovaneho navstevnika webu
 *
 *@Title        magma-web
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.11 $
 *@created      Nedeľe, 2002, júl 7
 *@modified     $Date: 2004/03/08 14:53:59 $
 */
public class AuthorizeAction
{
	public static boolean sendInfoEmail(int userId, Identity approveByUser, HttpServletRequest request)
	{
		return sendInfoEmail(userId, null, approveByUser, request);
	}

	/**
	 * Posle email o registracii s menom a heslom
	 * @param userId
	 * @param password
	 * @param request
	 * @return
	 */
	public static boolean sendInfoEmail(int userId, String password, Identity approveByUser, HttpServletRequest request)
	{
		boolean emailSend = false;
		try
		{
			UserDetails uform = UsersDB.getUser(userId);
			Logger.debug(AuthorizeAction.class, "sendInfoemail, uform: " + uform.getUserId());

			boolean userGenerated = false;
			if (approveByUser==null)
			{
				approveByUser = new Identity();
				approveByUser.setLastName(Tools.getServerName(request));
				approveByUser.setEmail(uform.getEmail());
				userGenerated = true;
				Logger.debug(AuthorizeAction.class, "approveByUser: " + approveByUser.getLastName() + " " + approveByUser.getEmail() + " generated: " + userGenerated);
			}

			//	skus ziskat ugd pre dane ppids
			UserGroupDetails userGroupDetails = null;
			UserGroupDetails userGroupDetails2 = null;
			try
			{
				StringTokenizer st = new StringTokenizer(uform.getUserGroupsIds(), ",");
				int ppid;
				UserGroupsDB ugDB = UserGroupsDB.getInstance();
				while (st.hasMoreTokens() && userGroupDetails==null)
				{
					ppid = Tools.getIntValue(st.nextToken(), -1);
					if (ppid > 0)
					{
						userGroupDetails2 = ugDB.getUserGroup(ppid);
						if (userGroupDetails2!=null && userGroupDetails2.getEmailDocId()>0)
						{
							//ak ma grupa zadany emailDocId, pouzi
							userGroupDetails = userGroupDetails2;
							break;
						}
					}
				}
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}

			if (uform.getEmail().length() > 5 && uform.getEmail().indexOf('@') > 1)
			{

				String url = Tools.getBaseHref(request);

				Prop prop = Prop.getInstance(Constants.getServletContext(), request);

				String subject = Constants.getString("approveEmailSubject");
				if (Tools.isEmpty(subject))
				{
					subject = prop.getText("iwcm.users.authorize_action.pristup_do_neverejnej_casti", url);
				}

				String body = Constants.getString("approveEmailText");

				if (Tools.isEmpty(body))
				{
					body = prop.getText("iwcm.users.authorize_action.vasa_ziadost")+"\n";

					body += prop.getText("iwcm.users.authorize_action.pre_pristup_pouzite")+"\n\n";

					body += "   "+prop.getText("iwcm.users.authorize_action.prihlasovacie_meno")+": " + uform.getLogin() + "\n";
					if (!Constants.getBoolean("passwordUseHash"))
					{
						body += "   "+prop.getText("iwcm.users.authorize_action.heslo")+": " + uform.getPassword() + "\n";
					}
					else if(Tools.isNotEmpty(password))
					{
						body += "   "+prop.getText("iwcm.users.authorize_action.heslo")+": " + password + "\n";
					}
					body += "\n\n\n" + url;
				}
				else
				{
					if (body.startsWith("docid="))
					{
						int docid = Integer.parseInt(body.substring(6));
						if (userGroupDetails == null)
						{
							userGroupDetails = new UserGroupDetails();
						}
						if (userGroupDetails.getEmailDocId() < 1)
						{
							userGroupDetails.setEmailDocId(docid);
						}
					}
				}
				//moznost specifikovat odosielatela a text mailu per formular
				if(request.getAttribute("approveEmailDocId")!=null)
				{
					int docid = Integer.parseInt((String)request.getAttribute("approveEmailDocId"),10);
					Logger.debug(AuthorizeAction.class, "sendInfoemail, approveEmailDocId: " + docid);
					if(docid > 0)
					{
						if (userGroupDetails!=null) userGroupDetails.setEmailDocId(docid);
						userGenerated =true;
					}
				}

				if (userGroupDetails!=null && userGroupDetails.getEmailDocId()>0)
				{
					try
					{

						DocDB docDB = DocDB.getInstance(); //servlet.getServletContext(), false, DBPool.getDBName(request));
						DocDetails docDetails = docDB.getDoc(userGroupDetails.getEmailDocId());
						body = docDetails.getData();
						subject = docDetails.getTitle();

						if (userGenerated)
						{
							if(request.getAttribute("approveEmailDocId")!=null)
							{
								if(Tools.isNotEmpty(docDetails.getFieldA())) approveByUser.setLastName(docDetails.getFieldA());
								if(Tools.isNotEmpty(docDetails.getFieldB())) approveByUser.setEmail(docDetails.getFieldB());
							}
							else
							{
								approveByUser.setLastName(docDetails.getAuthorName());
								approveByUser.setEmail(docDetails.getAuthorEmail());
							}
						}
					}
					catch (Exception ex)
					{
						sk.iway.iwcm.Logger.error(ex);
					}
				}

				if (Tools.isEmpty(body) || body.length()<10) return false;

				body = updateEmailText(body, uform, password, request);

				if (body.length() > 10)
				{
					//String body2 = new String(body.getBytes("windows-1250"));
					emailSend = SendMail.send(approveByUser.getFullName(), approveByUser.getEmail(), uform.getEmail(), subject, body);
				}
				if (!emailSend)
				{
					request.setAttribute("emailSendFail", "true");
				}
				else
				{
					request.setAttribute("from", approveByUser.getFullName()+"<"+approveByUser.getEmail()+">");
					request.setAttribute("to", uform.getEmail());
					request.setAttribute("subject", subject);
					body = Tools.replace(body, "\n", "<br>");
					request.setAttribute("body", body);
				}

			}
		}
		catch (Exception ex)
		{
			emailSend = false;
			sk.iway.iwcm.Logger.error(ex);
		}
		return(emailSend);
	}

	/**
	 * Update codes !LOGGED_USER_xxx with special codes like:
	 * !LOGGED_USER_PASSWORD!
	 * !APPROVER_USER_NAME!
	 * !APPROVER_USER_PHONE!
	 * !APPROVER_USER_EMAIL!
	 * in email text
	 * @param body
	 * @param uform
	 * @param password - user password or null
	 * @param request
	 * @return
	 */
	public static String updateEmailText(String body, UserDetails uform, String password, HttpServletRequest request) {
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

		/* Vyuzite v SIAF - do textu emailu sa doplni tel. cislo "admina" ktory spravil import - kedze adminov je viac, vzdy chcu konkretne t.c. */
		UserDetails loggedUser = UsersDB.getCurrentUser(request);
		if (loggedUser != null)
		{
			body = Tools.replace(body, "!TASKED_USER_PHONE!", loggedUser.getPhone());
			body = Tools.replace(body, "!TASKED_USER_EMAIL!", loggedUser.getEmailAddress());
			body = Tools.replace(body, "!APPROVER_USER_NAME!", loggedUser.getFullName());
			body = Tools.replace(body, "!APPROVER_USER_PHONE!", loggedUser.getPhone());
			body = Tools.replace(body, "!APPROVER_USER_EMAIL!", loggedUser.getEmailAddress());
		}
		/*	***  */

		body = Tools.replace(body, "!TITLE!", uform.getTitle());
		body = Tools.replace(body, "!NAME!", uform.getFullName());
		body = Tools.replace(body, "!name!", uform.getFullName());
		body = Tools.replace(body, "!FIRST_NAME!", uform.getFirstName());
		body = Tools.replace(body, "!LAST_NAME!", uform.getLastName());

		body = DocTools.updateUserCodes(new Identity(uform), new StringBuilder(body)).toString();

		//for safety reason
		body = Tools.replace(body, "!PASSWORD!", "*****");
		body = Tools.replace(body, "!LOGGED_USER_PASSWORD!", "*****");

		//uprav relativne cesty
		body = SendMail.createAbsolutePath(body, request);

		return body;
	}


	/**
	 * autorizovanie pouzivatela + vygenerovanie nahodneho hesla + poslanie emailu
	 * @param request
	 * @param user - pouzivatel, ktory autorizuje
	 * @param user_id - pouzivatel, ktoreho chceme autorizovat
	 */
	public static void authorize(HttpServletRequest request, Identity user, int user_id)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			UserDetails uform = UsersDB.getUser(user_id);

			//mame ho ;-0
			if (uform.getUserId() == user_id)
			{
				Adminlog.add(Adminlog.TYPE_USER_AUTHORIZE, "Authorize user :"
							+ "id= " + uform.getUserId()
							+ " login " + uform.getLogin()
							+ " name= " + uform.getFullName(), -1, -1);

				String password = null;
				if (uform.getFieldE().startsWith(RegUserAction.REQUIRE_AUTHORIZATION_AFTER_VERIFICATION) || Constants.getBoolean("authorizeRegeneratePassword"))
				{
					//vygeneruje nahodne heslo o dlzke 5 znakov, ak nevyhovuje bezpecnostnej politike, po prihlaseni sa vynuti zmena hesla
					password = RandomStringUtils.secure().nextAlphanumeric(5);
					//nakolko tu nemam normaenho usera ale len nejaky form, ziskam si ho znova ako trulko
					UserDetails u = UsersDB.getUser(user_id);
					if (u!=null)
					{
						u.setPassword(password);
						u.setFieldE("");
						UsersDB.saveUser(u);
					}
				}
				//posli mu mail
				boolean emailSend = sendInfoEmail(uform.getUserId(), password, user, request);

				if (emailSend)
				{
					db_conn = DBPool.getConnection();
					ps = db_conn.prepareStatement("UPDATE users SET authorized=? WHERE user_id=?"+UsersDB.getDomainIdSqlWhere(true));
					ps.setBoolean(1, true);
					ps.setInt(2, uform.getUserId());
					ps.execute();
					ps.close();
					ps = null;

					db_conn.close();
					db_conn = null;
				}

			}


		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}
	}
}
