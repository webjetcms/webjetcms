package sk.iway.iwcm.system.ntlm;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.PropertyUtils;

import sk.iway.Password;
import sk.iway.iwcm.*;
import sk.iway.iwcm.common.LogonTools;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

/**
 *  BasicLdapLogon.java
 *
 *  Zakladne prihlasenie usera cez LDAP (bez pouzitia NTLM)
 *
 * Title        webjet8
 * Company      InterWay a. s. (www.interway.sk)
 * Copyright    InterWay a. s. (c) 2001-2017
 * @author      Author: jeeff
 * created      17. 7. 2017 16:09:29
 */
public class BasicLdapLogon
{
	public BasicLdapLogon() {
		//konstruktor aby sa dala trieda vytvorit v LogonTools
	}

	public static List<String> logon(String username, String password, HttpServletRequest request)
	{
		List<String> errors = new ArrayList<>();

		//aby sa mohol prihlasit aj s velkymi pismenami, ak nahodou to tak je v AD
		username = username.toLowerCase();

		UserDetails user = UsersDB.getUser(username);

		if (user == null)
		{
			//este neexistuje, treba vytvorit
			user = createNewUser(username);
		}

		BasicNtlmLogon.doLdapQuery(user, Constants.getString("ldapProviderUrl"), username, password);

		if (user.getLogin()==null)
		{
			//ma zle heslo, napis chybu
			errors.add("error.logon.wrong.pass");
			//nepiseme o aku chybu ide
			request.setAttribute("error.logon.wrong.pass", "true");
		}
		else
		{
			user.setAuthorized(true);

			if (Tools.isNotEmpty(Constants.getString("basicNtlmLogonAttrs"))) UsersDB.saveUser(user);

			if (user.getUserId()>0)
			{
				Identity loggedUser = new Identity(user);

				if (Constants.getBoolean("enableAdminInWebLogon") && user.isAdmin())
				{
					//kvoli blogom - nacitanie prav

					//nacitaj pristupove prava - pouziva sa napr. v module blog - nie je admin (isAdmin bude false, viz nizsie), mame ale jeho prava
					LogonTools.setUserPerms(loggedUser);

					UsersDB.setDisabledItems(loggedUser);
					loggedUser.setAdmin(true);
					loggedUser.setValid(true);
				}
				else
				{
					//aby mu nefungovalo pristup do adminu (bezpecnost)
					loggedUser.setAdmin(false);
				}

				LogonTools.setUserToSession(request.getSession(), loggedUser);
			}
			else
			{
				//ma zle heslo, napis chybu
				errors.add("error.logon.wrong.pass");
				//nepiseme o aku chybu ide
				request.setAttribute("error.logon.wrong.pass", "true");
			}
		}

		return errors;
	}

	/**
	 * Nastavujem cez adminLogonMethod=sk.iway.iwcm.system.ntlm.BasicLdapLogon.logonAdmin
	 */
	public static String logonAdmin(String username, String password, Identity user, Map<String, String> errors, HttpServletRequest request, sk.iway.iwcm.i18n.Prop prop)
	{
		if (username == null || password == null)
		{
			return ("");
		}

		//aby sa mohol prihlasit aj s velkymi pismenami, ak nahodou to tak je v AD
		username = username.toLowerCase();
		String forward = "logon_ok_admin";

		Logger.debug(BasicLdapLogon.class, " username = "+username);

		UserDetails actualUser = UsersDB.getUser(username);
		int newAdminTemplatePermsUserId = 0;
		//zistim, ci je povolene vytvorit automaticky noveho pouzivatela cez konf. premennu ldapNewAdminTemplatePermsUserLogin a ci dany pouzivatel vo WJ existuje
		String ldapNewAdminTemplatePermsUserLogin = Constants.getString("ldapNewAdminTemplatePermsUserLogin");
		if(Tools.isNotEmpty(ldapNewAdminTemplatePermsUserLogin) && UsersDB.getUser(ldapNewAdminTemplatePermsUserLogin) != null)
			newAdminTemplatePermsUserId=UsersDB.getUser(ldapNewAdminTemplatePermsUserLogin).getUserId();

		//nemam pouzivatela, mozem ale mat povolene automaticke vytvorenie admin pouzivatela
		if (actualUser == null)
		{
			if(newAdminTemplatePermsUserId > 0)
			{
				//este neexistuje, treba vytvorit
				actualUser = createNewUser(username);
			}
			else //rovno vraciam chybovu hlasku
			{
				if(Tools.isNotEmpty(ldapNewAdminTemplatePermsUserLogin))
					Logger.warn(BasicLdapLogon.class, "Pouzivatel "+ldapNewAdminTemplatePermsUserLogin+" neexistuje vo WJ");
				else
					Logger.warn(BasicLdapLogon.class, "Pouzivatel "+username+" vo WJ neexistuje a nie je povolene vytvorenie noveho admin pouzivatela!");

				errors.put("ERROR_KEY", prop.getText("logon.err.wrongPass"));
				request.setAttribute("error.logon.wrong.pass", "true");
				return forward;
			}
		}

		//poznacim si povodny stav ci pouzivatel bol admin
		boolean oldUserIsAdmin = actualUser.isAdmin();

		BasicNtlmLogon.doLdapQuery(actualUser, Constants.getString("ldapProviderUrl"), username, password);

		//asi zle heslo, alebo nejaka ina nutna podmienka v BasicNtlmLogon.doLdapQuery nebola splnena
		if (actualUser.getLogin()==null)
		{
			Logger.error(BasicLdapLogon.class, "after BasicNtlmLogon.doLdapQuery: LoginName je null, asi zle heslo");
			errors.put("ERROR_KEY", prop.getText("logon.err.wrongPass"));
			request.setAttribute("error.logon.wrong.pass", "true");
		}
		else
		{
			//autorizujem pouzivatela
			actualUser.setAuthorized(true);
			//ak mam zadefinovane atributy v basicNtlmLogonAttrs pre synchronizaciu dat o pouzivatelovi v AD, ulozim ho (setli sa uz v BasicNtlmLogon.doLdapQuery)
			if (Tools.isNotEmpty(Constants.getString("basicNtlmLogonAttrs"))) UsersDB.saveUser(actualUser);

			try
			{
				PropertyUtils.copyProperties(user, actualUser);
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}

			//ocakavam, ze som uz ulozil pouzivatela, inak pravdepodobne je basicNtlmLogonAttrs prazdne,
			//  co nemoze byt, musi tam byt minimalne memberOf pre synchronizaciu user skupin alebo skupin prav
			if(user.getUserId() > 0)
			{
				//ak som vytvoril pouzivatela, alebo prechadzam z bezneho pouzivatel na administratora a je povolene automaticke vytvorenie noveho pouzivatela,
				//  nastavim prava na moduly na zaklade uzivatela ldapNewAdminTemplatePermsUserLogin
				if(oldUserIsAdmin == false && user.isAdmin() && newAdminTemplatePermsUserId > 0)
					copyUserDisabledItems(newAdminTemplatePermsUserId, user.getUserId());

				//nacitaj pristupove prava
				LogonTools.setUserPerms(user);
				UsersDB.setDisabledItems(user);
			}
			else
			{
				Logger.error(BasicLdapLogon.class, "Nemam userID, nieco sa cestou pokazilo, skontroluj nastavene hodnoty v basicNtlmLogonAttrs");
				errors.put("ERROR_KEY", prop.getText("logon.err.wrongPass"));
				request.setAttribute("error.logon.wrong.pass", "true");
			}
		}

		return forward;
	}

	/**
	 * vytvorim noveho pouzivatela
	 * @param username loginName
	 * @return UserDetails
	 */
	public static UserDetails createNewUser(String username)
	{
		UserDetails actualUser = new UserDetails();
		actualUser.setUserId(-1);
		actualUser.setLogin(username);
		actualUser.setPassword(Password.generatePassword(10));
		actualUser.setEditablePages("");
		actualUser.setEditableGroups("");
		actualUser.setWritableFolders("");
		return actualUser;
	}

	/**
	 * skopiruje zakazane moduly  medzi pouzivatelmi
	 * @param originalUserId odkial kopirujem
	 * @param newUserId kam kopirujem
	 */
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

				Logger.debug(BasicLdapLogon.class, "copyUserDisabledItems to userId="+newUserId+" itemName="+itemName);
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
			catch (Exception ignored)
			{
			}
		}
	}
}
