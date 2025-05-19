package sk.iway.iwcm.components.users;

import java.util.StringTokenizer;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;

import sk.iway.Password;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.users.userdetail.UserDetailsEntity;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.stripes.RegUserAction;
import sk.iway.iwcm.users.PasswordSecurity;
import sk.iway.iwcm.users.PasswordsHistoryBean;
import sk.iway.iwcm.users.UserGroupDetails;
import sk.iway.iwcm.users.UserGroupsDB;
import sk.iway.iwcm.users.UsersDB;

@Service
public class AuthorizeUserService {

	/**
	 * Authorize user and send him a email about authorization status. If wanted, generate new password for user.
	 *
	 * @param userToApprove entity of user to approve
	 * @param approver entity of user who is approving
	 * @param generatePassword true - generate new password for user or do nothing
	 * @param request - request
	 * @return true - authorization successful or return false
	 */
	public static boolean authUser(UserDetailsEntity userToApprove, Identity approver, boolean generatePassword, HttpServletRequest request) {
		boolean emailSend = false;
		String password = null;

		//Adminlog
		Adminlog.add(Adminlog.TYPE_USER_AUTHORIZE, "Authorize user :"
			+ "id= " + userToApprove.getId()
			+ " login " + userToApprove.getLogin()
			+ " name= " + userToApprove.getFullName(), -1, -1);

		//
		if (generatePassword || Constants.getBoolean("authorizeRegeneratePassword")
				|| userToApprove.getFieldE().startsWith(RegUserAction.REQUIRE_AUTHORIZATION_AFTER_VERIFICATION)) {

			//Generate new password
			password = Password.generateStringHash(8);

			//Send approve email, with new Password (NOT hashed)
			emailSend = sendInfoEmail(userToApprove, password, approver, request);

			if(!emailSend) return false;

			//NOW hash password
			try {
                String salt = "";
                String hash = "";
                sk.iway.Password pass = new sk.iway.Password();
                if (Constants.getBoolean("passwordUseHash")) {
                    salt = PasswordSecurity.generateSalt();
                    hash = PasswordSecurity.calculateHash(password, salt);
                } else {
                    hash = pass.encrypt(password);
                }

                PasswordsHistoryBean.insertAndSaveNew(userToApprove.getId().intValue(), hash, salt);

                //Save updated user, password / hash / fieldE
                (new SimpleQuery()).execute("UPDATE users SET password=?, password_salt=?, authorized=?, field_e=? WHERE user_id=?", hash, salt, true, "", userToApprove.getId());

                //Add auditlog about password chnage
                Adminlog.add(Adminlog.TYPE_USER_CHANGE_PASSWORD, userToApprove.getId().intValue(), "SaveUserAction - user (" + userToApprove.getLogin() + ") successfully changed password", -1, -1);
            } catch (Exception ex) {
                Logger.error(AuthorizeUserService.class, ex);
				return false;
            }
		} else {
			//Send approve email, without password
			emailSend = sendInfoEmail(userToApprove, password, approver, request);

			if(!emailSend) return false;

			//Update user approvement status
			String sql = "UPDATE users SET authorized=? WHERE user_id=?"+UsersDB.getDomainIdSqlWhere(true);
			(new SimpleQuery()).execute(sql, true, userToApprove.getId());
		}

		return true;
	}

	/**
	 * Send email about auth status with new generated password
	 * @param userToApprove - user that was approved
	 * @param password - approveed user new generated password
	 * @param approveByUser - who is approver
	 * @param request - request
	 * @return
	 */
	public static boolean sendInfoEmail(UserDetailsEntity userToApprove, String password, Identity approveByUser, HttpServletRequest request) {
		boolean emailSend = false;

		if(userToApprove == null || approveByUser == null) return false;

		try {
			Logger.debug(AuthorizeUserService.class, "sendInfoemail, userDetailsEntity: " + userToApprove);

			//	skus ziskat ugd pre dane ppids
			UserGroupDetails userGroupDetails = null;
			UserGroupDetails userGroupDetails2 = null;
			try {
				StringTokenizer st = new StringTokenizer(userToApprove.getUserGroupsIds(), ",");
				int ppid;
				UserGroupsDB ugDB = UserGroupsDB.getInstance();
				while (st.hasMoreTokens() && userGroupDetails == null) {
					ppid = Tools.getIntValue(st.nextToken(), -1);
					if (ppid > 0) {
						userGroupDetails2 = ugDB.getUserGroup(ppid);
						if (userGroupDetails2!=null && userGroupDetails2.getEmailDocId() > 0) {
							//ak ma grupa zadany emailDocId, pouzi
							userGroupDetails = userGroupDetails2;
							break;
						}
					}
				}
			} catch (Exception e) { sk.iway.iwcm.Logger.error(e); }

			if (Tools.isEmail(userToApprove.getEmail())) {
				String url = Tools.getBaseHref(request);
				Prop prop = Prop.getInstance(Constants.getServletContext(), request);

				String subject = Constants.getString("approveEmailSubject");
				if (Tools.isEmpty(subject))
					subject = prop.getText("iwcm.users.authorize_action.pristup_do_neverejnej_casti", url);

				String body = Constants.getString("approveEmailText");
				if (Tools.isEmpty(body)) {

					body = prop.getText("iwcm.users.authorize_action.vasa_ziadost")+"\n";
					body += prop.getText("iwcm.users.authorize_action.pre_pristup_pouzite")+"\n\n";
					body += "   " + prop.getText("iwcm.users.authorize_action.prihlasovacie_meno") + ": " + userToApprove.getLogin() + "\n";

					if (!Constants.getBoolean("passwordUseHash"))
						body += "   "+prop.getText("iwcm.users.authorize_action.heslo")+": " + userToApprove.getPassword() + "\n";
					else if(Tools.isNotEmpty(password))
						body += "   "+prop.getText("iwcm.users.authorize_action.heslo")+": " + password + "\n";

					body += "\n\n\n" + url;
				} else if (body.startsWith("docid=")) {
					int docid = Integer.parseInt(body.substring(6));
					if(userGroupDetails == null)
						userGroupDetails = new UserGroupDetails();

					if(userGroupDetails.getEmailDocId() < 1)
						userGroupDetails.setEmailDocId(docid);
				}

				if (userGroupDetails != null && userGroupDetails.getEmailDocId() > 0) {
					try {
						DocDB docDB = DocDB.getInstance(); //servlet.getServletContext(), false, DBPool.getDBName(request));
						DocDetails docDetails = docDB.getDoc(userGroupDetails.getEmailDocId());
						body = docDetails.getData();
						subject = docDetails.getTitle();
					} catch (Exception ex) { sk.iway.iwcm.Logger.error(ex); }
				}

				if (Tools.isEmpty(body) || body.length() < 10) return false;

				//	replacni !BR! za \n
				body = Tools.replace(body, "!BR!", "\n");
				body = Tools.replace(body, "!LOGIN_NAME!", userToApprove.getLogin());
				body = Tools.replace(body, "!LOGGED_USER_LOGIN!", userToApprove.getLogin());

				if(!Constants.getBoolean("passwordUseHash")) {
					body = Tools.replace(body, "!PASSWORD!", userToApprove.getPassword());
					body = Tools.replace(body, "!LOGGED_USER_PASSWORD!", userToApprove.getPassword());
				} else if(Tools.isNotEmpty(password)) {
					body = Tools.replace(body, "!PASSWORD!", password);
					body = Tools.replace(body, "!LOGGED_USER_PASSWORD!", password);
				}

				/* Vyuzite v SIAF - do textu emailu sa doplni tel. cislo "admina" ktory spravil import - kedze adminov je viac, vzdy chcu konkretne t.c. */
				if (approveByUser != null) {
                    body = Tools.replace(body, "!TASKED_USER_PHONE!", approveByUser.getPhone());
                    body = Tools.replace(body, "!TASKED_USER_EMAIL!", approveByUser.getEmailAddress());
                }
				/*	***  */

				body = Tools.replace(body, "!TITLE!", userToApprove.getTitle());
				body = Tools.replace(body, "!NAME!", userToApprove.getFullName());
				body = Tools.replace(body, "!name!", userToApprove.getFullName());
				body = Tools.replace(body, "!FIRST_NAME!", userToApprove.getFirstName());
				body = Tools.replace(body, "!LAST_NAME!", userToApprove.getLastName());

				//uprav relativne cesty
				body = SendMail.createAbsolutePath(body, request);

				if (body.length() > 10)
					emailSend = SendMail.send(approveByUser.getFullName(), approveByUser.getEmail(), userToApprove.getEmail(), subject, body);

				if (!emailSend)
					request.setAttribute("emailSendFail", "true");
				else {
					request.setAttribute("from", approveByUser.getFullName()+"<"+approveByUser.getEmail()+">");
					request.setAttribute("to", userToApprove.getEmail());
					request.setAttribute("subject", subject);
					body = Tools.replace(body, "\n", "<br>");
					request.setAttribute("body", body);
				}
			}
		} catch (Exception ex) {
			emailSend = false;
			sk.iway.iwcm.Logger.error(ex);
		}

		return(emailSend);
	}
}
