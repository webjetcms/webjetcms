package sk.iway.iwcm.users;

import net.sourceforge.stripes.action.ActionBeanContext;
import sk.iway.Html2Text;
import sk.iway.Password;
import sk.iway.iwcm.*;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.FileBrowserTools;
import sk.iway.iwcm.common.LogonTools;
import sk.iway.iwcm.common.UserTools;
import sk.iway.iwcm.components.users.userdetail.UserDetailsService;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.dmail.EmailDB;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.helpers.BeanDiff;
import sk.iway.iwcm.helpers.BeanDiffPrinter;
import sk.iway.iwcm.helpers.MailHelper;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.stat.StatNewDB;
import sk.iway.iwcm.system.Modules;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.sql.*;
import java.util.*;
import java.util.Map.Entry;

import static sk.iway.iwcm.Tools.isEmpty;


/**
 *  Databaza registrovanych pouzivatelov
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.8 $
 *@created      Sobota, 2004, január 10
 *@modified     $Date: 2004/03/08 14:53:59 $
 */
public class UsersDB
{
	public static final int APPROVE_APPROVE = 0;
	public static final int APPROVE_NOTIFY = 1;
	public static final int APPROVE_NONE = 2;
	public static final int APPROVE_LEVEL2 = 3;

	private static final String CACHE_KEY = "UsersDB.users";

	private static Random rand = new Random();

	protected UsersDB() {
		//utility class
	}

	/**
	 * Specialna verzia UsersDB.getDomainIdSqlWhere pre USERS tabulku, kde pre NIE CLOUD WJ nerozlisujeme userov podla domen
	 * @param addAnd
	 * @return
	 */
	public static String getDomainIdSqlWhere(boolean addAnd) {
		if (InitServlet.isTypeCloud()) return CloudToolsForCore.getDomainIdSqlWhere(addAnd);

		//ak sa jedna o standardny (NIE CLOUD) WJ tak vratime prazdnu podmienku, pouzivatelia su medzi domenami zhodny
		if (addAnd)
		{
				return " "; //toto je zbytocne, netreba dat ziadnu podmienku: AND 1=1
		}
		else
		{
				return " 1=1 ";
		}
	}

	public static void fillUserDetails(UserDetails usr, ResultSet rs) throws Exception
	{
		usr.setUserId(rs.getInt("user_id"));
		usr.setTitle(DB.getDbString(rs, "title"));
		usr.setFirstName(DB.getDbString(rs, "first_name"));
		usr.setLastName(DB.getDbString(rs, "last_name"));
		usr.setLogin(DB.getDbString(rs, "login"));

		usr.setPassword(UserTools.PASS_UNCHANGED);

		usr.setAdmin(rs.getBoolean("is_admin"));
		usr.setUserGroupsIds(DB.getDbString(rs, "user_groups"));

		if (InitServlet.isWebjetInitialized())
		{
			UserGroupsDB userGroupsDB = UserGroupsDB.getInstance();
			usr.setUserGroupsNames(userGroupsDB.convertIdsToNames(usr.getUserGroupsIds()));
		}

		usr.setCompany(DB.getDbString(rs, "company"));
		usr.setAdress(DB.getDbString(rs, "adress"));
		usr.setCity(DB.getDbString(rs, "city"));
		usr.setEmail(DB.getDbString(rs, "email"));
		usr.setPSC(DB.getDbString(rs, "PSC"));
		usr.setCountry(DB.getDbString(rs, "country"));
		usr.setPhone(DB.getDbString(rs, "phone"));

		usr.setAuthorized(rs.getBoolean("authorized"));

		usr.setEditableGroups(DB.getDbString(rs, "editable_groups"));
		usr.setEditablePages(DB.getDbString(rs, "editable_pages"));
		usr.setWritableFolders(DB.getDbString(rs, "writable_folders"));

		usr.setLastLogon(DB.getDbDateTime(rs, "last_logon"));
		usr.setLastLogonAsDate(rs.getTimestamp("last_logon"));
		if(rs.getTimestamp("reg_date")!=null)
			usr.setRegDate(rs.getTimestamp("reg_date").getTime());

		usr.setFieldA(DB.getDbString(rs, "field_a"));
		usr.setFieldB(DB.getDbString(rs, "field_b"));
		usr.setFieldC(DB.getDbString(rs, "field_c"));
		usr.setFieldD(DB.getDbString(rs, "field_d"));
		usr.setFieldE(DB.getDbString(rs, "field_e"));

		usr.setDateOfBirth(DB.getDbDate(rs, "date_of_birth"));
		usr.setSexMale(rs.getBoolean("sex_male"));
		usr.setPhoto(DB.getDbString(rs, "photo"));
		usr.setSignature(DB.getDbString(rs, "signature"));

		usr.setForumRank(rs.getInt("forum_rank"));
		usr.setRatingRank(rs.getInt("rating_rank"));

		usr.setAllowLoginStart(DB.getDbDate(rs, "allow_login_start"));
		usr.setAllowLoginEnd(DB.getDbDate(rs, "allow_login_end"));

		usr.setAllowDateLogin(LogonTools.checkAllowLoginDates(rs));

		usr.setFax(DB.getDbString(rs, "fax"));
		usr.setDeliveryCity(DB.getDbString(rs, "delivery_city"));
		usr.setDeliveryCompany(DB.getDbString(rs, "delivery_company"));
		usr.setDeliveryCountry(DB.getDbString(rs, "delivery_country"));
		usr.setDeliveryFirstName(DB.getDbString(rs, "delivery_first_name"));
		usr.setDeliveryLastName(DB.getDbString(rs, "delivery_last_name"));
		usr.setDeliveryPhone(DB.getDbString(rs, "delivery_phone"));
		usr.setDeliveryPsc(DB.getDbString(rs, "delivery_psc"));
		usr.setDeliveryAdress(DB.getDbString(rs, "delivery_adress"));

		usr.setPosition(DB.getDbString(rs, "position"));
		usr.setParentId(rs.getInt("parent_id"));
	}

	/**
	 * Vrati zoznam pouzivatelov v danej skupine
	 * @param userGroupId - id skupiny, ak je -1 vrati zoznam vsetkych
	 * @return
	 */
	public static List<UserDetails> getUsersByGroup(int userGroupId)
	{
		int[] userGroupIds = new int[1];
		userGroupIds[0] = userGroupId;
		return getUsersByGroups(userGroupIds);
	}

    /**
     * Vrati zoznam pouzivatelov v danej skupine
     * @param groupName - nazov skupiny
     * @return
     */
    public static List<UserDetails> getUsersByGroup(String groupName)
    {
        UserGroupDetails educationAdminGroup = UserGroupsDB.getInstance().getUserGroup(groupName);
        if (educationAdminGroup == null) {
            return new ArrayList<>();
        }
        return getUsersByGroup(educationAdminGroup.getUserGroupId());
    }

	/**
	 * Vrati zoznam vsetkych pouzivatelov zacinajucich retazcom
	 * @return zoznam vsetkych userov podla mena
	 */
	public static List<UserDetails> getUsersByName(String startsWith)
	{
		List<UserDetails> users = new ArrayList<>();
		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{

			db_conn = DBPool.getConnection();

			String statement = "SELECT * FROM  users WHERE CONCAT(CONCAT(first_name, ' '), last_name) LIKE ?";
			//if (Constants.DB_TYPE == Constants.DB_MSSQL) statement = "SELECT * FROM  users WHERE (first_name + ' ' + last_name) Like ?";

			ps = db_conn.prepareStatement(statement);
			ps.setString(1, "%" + startsWith + "%");
			rs = ps.executeQuery();
			UserDetails usr;

			while (rs.next())
			{
				usr = new UserDetails(rs);
				users.add(usr);
			}
			rs.close();
			ps.close();
			db_conn.close();
			db_conn = null;
			rs = null;
			ps = null;
		}
		catch (Exception ex){sk.iway.iwcm.Logger.error(ex);}
		finally {
			try {
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			} catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
		return users;
	}

	/**
	 * Ziska zoznam pouzivatelov v zadanych skupinach
	 * @param userGroupIds
	 * @return
	 */
	public static List<UserDetails> getUsersByGroups(int[] userGroupIds)
	{
		List<UserDetails> users = new ArrayList<>();
		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{

			db_conn = DBPool.getConnectionReadUncommited();

			StringBuilder sql = new StringBuilder("SELECT * FROM users");
			sql.append(" WHERE ");
			sql.append(UsersDB.getDomainIdSqlWhere(false));

			if (userGroupIds.length>0 && (userGroupIds.length!=1 || userGroupIds[0]!=-1))
			{
				sql.append(" AND (");
				for (int i=0; i<userGroupIds.length; i++)
				{
					if (i==0) sql.append(" user_groups LIKE '%").append(userGroupIds[i]).append("%'");
					else if (i==userGroupIds.length-1) sql.append(" OR user_groups LIKE '%").append(userGroupIds[i]).append("%' ");
					else sql.append(" OR user_groups LIKE '%").append(userGroupIds[i]).append("%'");
				}
				sql.append(" )");
			}

			sql.append(" ORDER BY last_name, first_name");

			Logger.debug(UsersDB.class, "sql="+sql);

			ps = db_conn.prepareStatement(sql.toString());
			rs = ps.executeQuery();
			UserDetails usr;
			while (rs.next())
			{
				if (userGroupIds.length == 0 || userGroupIds[0]==-1)
				{
					usr = new UserDetails(rs);
					users.add(usr);
				}
				else
				{
					StringTokenizer st = new StringTokenizer(DB.getDbString(rs, "user_groups"), ",");
					usr = null;
					while (usr==null && st.hasMoreTokens())
					{
						int userGroupId = Tools.getIntValue(st.nextToken(), 0);
						for (int i=0; i<userGroupIds.length; i++)
						{
							if (userGroupId == userGroupIds[i])
							{
								usr = new UserDetails(rs);
								users.add(usr);
								i = userGroupIds.length+1; //NOSONAR
							}
						}
					}
				}

			}
			rs.close();
			ps.close();
			db_conn.close();
			db_conn = null;
			rs = null;
			ps = null;
		}
		catch (Exception ex){sk.iway.iwcm.Logger.error(ex);}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
		return (users);
	}

	/**
	 * Vrati pozadovaneho usera
	 * @param userId
	 * @return
	 */
	public static UserDetails getUser(int userId)
	{
		UserDetails usr = null;
		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{

			db_conn = DBPool.getConnectionReadUncommited();
			ps = db_conn.prepareStatement("SELECT * FROM  users WHERE user_id=?"+UsersDB.getDomainIdSqlWhere(true));
			ps.setInt(1, userId);
			rs = ps.executeQuery();

			if (rs.next())
			{
				usr = new UserDetails();
				fillUserDetails(usr, rs);
			}
			rs.close();
			ps.close();
			db_conn.close();
			db_conn = null;
			rs = null;
			ps = null;
		}
		catch (Exception ex){sk.iway.iwcm.Logger.error(ex);}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
		return (usr);
	}

	/**
	 * Vrati pouzivatela so zadanym loginName
	 * @param loginName
	 * @return
	 */
	public static UserDetails getUser(String loginName)
	{
		UserDetails usr = null;
		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{

			db_conn = DBPool.getConnectionReadUncommited();
			ps = db_conn.prepareStatement("SELECT * FROM  users WHERE login=?"+UsersDB.getDomainIdSqlWhere(true));
			ps.setString(1, loginName);
			rs = ps.executeQuery();

			if (rs.next())
			{
				usr = new UserDetails();
				fillUserDetails(usr, rs);
			}

			rs.close();
			ps.close();
			db_conn.close();
			db_conn = null;
			rs = null;
			ps = null;
		}
		catch (Exception ex){sk.iway.iwcm.Logger.error(ex);}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
		return (usr);
	}

	/**
	 * Vrati pouzivatela so zadanym emailom (prveho co najde)
	 * @param email
	 * @return
	 */
	public static UserDetails getUserByEmail(String email) {
		return getUserByEmail(email, 0);
	}

	/**
	 * Vrati pouzivatela so zadanym emailom (prveho co najde)
	 * @param email
	 * @return
	 */
	public static UserDetails getUserByEmail(String email, int cacheInSeconds)
	{
		Cache cache = Cache.getInstance();
		if (cacheInSeconds > 0) {
			UserDetails user = (UserDetails) cache.getObject("user-email-" + email);

			if (user != null) {
				return user;
			}
		}

		UserDetails usr = null;
		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{

			db_conn = DBPool.getConnectionReadUncommited();
			ps = db_conn.prepareStatement("SELECT * FROM  users WHERE email=?"+UsersDB.getDomainIdSqlWhere(true));
			ps.setString(1, email);
			rs = ps.executeQuery();

			if (rs.next())
			{
				usr = new UserDetails();
				fillUserDetails(usr, rs);
			}

			rs.close();
			ps.close();
			db_conn.close();
			db_conn = null;
			rs = null;
			ps = null;
		}
		catch (Exception ex){sk.iway.iwcm.Logger.error(ex);}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}

		if (cacheInSeconds > 0) {
			cache.setObjectSeconds("user-email-" + email, usr, cacheInSeconds);
		}

		return (usr);
	}

	/**
	 * Ziska ID uzivatela z emailu, ak uzivatel s takym emailom neexistuje vrati <code>null</code>
	 * @param email
	 * @return
	 */
	public static Integer getUserIdByEmail(String email)
	{
		UserDetails user = getUserByEmail(email);
		return (user!=null ? user.getUserId() : null);
	}

	/**
	 * Ziska ID uzivatela z loginu, ak uzivatel s takym loginom neexistuje vrati <code>null</code>
	 * @param login
	 * @return
	 */
	public static Integer getUserIdByLogin(String login)
	{
		UserDetails user = getUser(login);
		return (user!=null ? user.getUserId() : null);
	}

	public static int subscribeEmail(UserDetails user, HttpServletRequest request, String emailBody, String emailSenderEmail, String emailSenderName, String emailSubject)
	{
		return(subscribeUnsubscribeEmail(user, request, emailBody, emailSenderEmail, emailSenderName, emailSubject, false));
	}

	public static int unsubscribeEmail(UserDetails user, HttpServletRequest request, String emailBody, String emailSenderEmail, String emailSenderName, String emailSubject)
	{
		return(subscribeUnsubscribeEmail(user, request, emailBody, emailSenderEmail, emailSenderName, emailSubject, true));
	}

	/**
	 * Prihlasenie / odhlasenie odberu distribucneho zoznamu
	 * @param user - informacie o prijemcovi
	 * @param request - request
	 * @param emailBody - text emailu, ktory sa posle
	 * @param emailSenderEmail - email odosielatela emailu
	 * @param emailSenderName - meno odosielatela emailu
	 * @param emailSubject - predmet emailu
	 * @param unsubscribe - ak je true, jedna sa o odhlasenie
	 * @return - >0 = OK
	 *           -1 = chyba pri praci s DB
	 *           -2 = chyba pri odosielani emailu
	 *           ine = neznama chyba
	 */
	@SuppressWarnings("java:S1643")
	private static int subscribeUnsubscribeEmail(UserDetails user, HttpServletRequest request, String emailBody, String emailSenderEmail, String emailSenderName, String emailSubject, boolean unsubscribe)
	{
		if (SpamProtection.canPost("dmail", emailBody, request)==false) return -1;

		int createdUserId = user.getUserId();

		Identity loggedUser = UsersDB.getCurrentUser(request);
		//ak nesedi email, neberme prihlaseneho cloveka
		if (loggedUser!=null && loggedUser.getEmail().equals(user.getEmail())==false) loggedUser = null;

		//nacitaj udaje z request

		String sql;

		//kontrola udajov
		if (Tools.isEmail(user.getEmail())==false)
		{
			return(-1);
		}
		if (Tools.isEmpty(user.getFirstName()))
		{
			user.setFirstName(user.getEmail().substring(0, user.getEmail().indexOf('@')));
		}
		if (Tools.isEmpty(user.getLastName()))
		{
			user.setLastName(user.getEmail().substring(user.getEmail().indexOf('@')));
		}
		if (Tools.isEmpty(user.getLogin()))
		{
			//user.setLogin(user.getEmail());
			user.setLogin(UsersDB.getRandomLogin());
		}

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			String userGroups = null;

			sql = "SELECT * FROM  users WHERE email=?"+UsersDB.getDomainIdSqlWhere(true);
			if (loggedUser != null) sql = "SELECT * FROM  users WHERE user_id="+loggedUser.getUserId()+UsersDB.getDomainIdSqlWhere(true);

			String fullName = "";
			db_conn = DBPool.getConnection(request);
			ps = db_conn.prepareStatement(sql);
			if (loggedUser == null) ps.setString(1, user.getEmail());
			rs = ps.executeQuery();
			//fullName = (title + " " + firstName + " " + lastName).trim();

			if (user.getTitle() != null && user.getTitle().length()>0)
			{
				fullName = user.getTitle() + " ";
			}
			if (user.getFirstName() != null && user.getFirstName().length()>0)
			{
				fullName += user.getFirstName() + " ";
			}
			if (user.getLastName() != null && user.getLastName().length()>0)
			{
				fullName += user.getLastName() + " ";
			}
			fullName = fullName.trim();

			if (rs.next())
			{
				if (user == null || user.getUserId() < 1)
				{
					createdUserId = rs.getInt("user_id");
				}
				userGroups = DB.getDbString(rs, "user_groups");
				if (unsubscribe)
				{
					fullName = DB.getFullName(rs, "title");
				}
			}
			rs.close();
			ps.close();

			emailBody = Tools.replace(emailBody, "!name!", fullName);

			if (userGroups!=null && userGroups.length()>0)
			{
				//ponechame len grupy, ktore nie su emailove
				try
				{
					StringTokenizer st = new StringTokenizer(userGroups, ",");
					UserGroupsDB userGroupsDB = UserGroupsDB.getInstance(Constants.getServletContext(), false, DBPool.getDBName("request"));
					List<UserGroupDetails> permsGroups = userGroupsDB.getUserGroupsByTypeId(UserGroupDetails.TYPE_PERMS);
					//konvertni to na Map
					Map<String, UserGroupDetails> htPermsGroups = new Hashtable<>(permsGroups.size());
					int len = permsGroups.size();
					int i = 0;
					UserGroupDetails ugd;
					for (i=0; i<len;i++)
					{
						ugd = permsGroups.get(i);
						htPermsGroups.put(Integer.toString(ugd.getUserGroupId()), ugd);
					}

					String unsubscribeGroups = null;
					if (unsubscribe)
					{
						String[] unsubscribeGroupsParams = null;
						unsubscribeGroupsParams = request.getParameterValues("unsubscribe_group_id");
						if (unsubscribeGroupsParams != null)
						{
							for (i = 0; i < unsubscribeGroupsParams.length; i++)
							{
								if (Tools.isEmpty(unsubscribeGroupsParams[i])) continue;
								if (unsubscribeGroups == null) unsubscribeGroups = ","+unsubscribeGroupsParams[i]+",";
								else unsubscribeGroups += unsubscribeGroupsParams[i]+",";
							}
						}
					}

					len = st.countTokens();
					i = 0;
					int groupId;
					userGroups = null;
					while (st.hasMoreTokens())
					{
						groupId = Integer.parseInt(st.nextToken());
						//testni ci je to perms grupa
						if (htPermsGroups.get(Integer.toString(groupId))!=null)
						{
							if (userGroups == null) userGroups = Integer.toString(groupId);
							else userGroups = userGroups + "," + groupId;
						}
						else if (unsubscribeGroups!=null)
						{
							//ak mame nejake unsubscribe groups, tak vyhodime iba tie
							if (unsubscribeGroups.indexOf(","+groupId+",")==-1)
							{
								if (userGroups == null) userGroups = Integer.toString(groupId);
								else userGroups = userGroups + "," + groupId;
							}
						}
					}
				}
				catch (Exception ex)
				{
					sk.iway.iwcm.Logger.error(ex);
				}
			}

			//	vytvor zoznam skupin ktore ma registrovane

			String[] groups = request.getParameterValues("user_group_id");
			int i;
			if (groups != null)
			{
				for (i = 0; i < groups.length; i++)
				{
					if (Tools.isEmpty(groups[i])) continue;

					if (userGroups == null)
					{
						userGroups = groups[i];
					}
					else
					{
						if ((","+userGroups+",").indexOf(","+groups[i]+",")==-1)
						{
							userGroups = userGroups + "," + groups[i];
						}
					}
				}
			}

			if (userGroups == null) userGroups = "";

			//ak user so zadanym emailom este neexistuje, vytvorime ho
			if (createdUserId == -1)
			{
				if (unsubscribe)
				{
					return(-3);
				}
				else
				{
					user.setPassword(Password.generateStringHash(10));
					saveUser(user);
					createdUserId = user.getUserId();

					Adminlog.add(Adminlog.TYPE_USER_INSERT, "subscribeUnsubscribe create new user, email="+user.getEmail(), -1, -1);
				}
			}

			//netreba posielat konfirmacky email, rovno to userovi updatneme
			if (emailBody == null)
			{
				UserDetails logUser = UsersDB.getUser(createdUserId);

				if (Tools.isEmpty(userGroups) && unsubscribe)
				{
					String mode = Constants.getString("dmailUnsubscribeMode");

					if ("delete".equals(mode))
					{
						//nema ziadne skupiny - mazem z DB
						ps = db_conn.prepareStatement("DELETE FROM  users WHERE user_id=? AND is_admin=?"+UsersDB.getDomainIdSqlWhere(true));
						ps.setInt(1, createdUserId);
						ps.setBoolean(2, false);
						ps.execute();
						ps.close();
						//#23471 - Password security
                        PasswordsHistoryDB.getInstance().deleteAllByUserId(createdUserId);

						//toto tu je keby sa nepodarilo vymazanie (je to napr. admin)
						ps = db_conn.prepareStatement("UPDATE  users SET user_groups=?, authorized=? WHERE user_id=? AND is_admin=?"+UsersDB.getDomainIdSqlWhere(true));
						ps.setString(1, "");
						ps.setBoolean(2, true);
						ps.setInt(3, createdUserId);
						ps.setBoolean(4, true);
						ps.execute();
						ps.close();

						if (logUser != null)
						{
							Adminlog.add(Adminlog.TYPE_USER_DELETE, "subscribeUnsubscribe delete user, login="+logUser.getLogin()+" email="+logUser.getEmail(), logUser.getUserId(), -1);
						}
					}
					else if ("removeGroups".equals(mode))
					{
						//toto tu je keby sa nepodarilo vymazanie (je to napr. admin)
						ps = db_conn.prepareStatement("UPDATE  users SET user_groups=?, authorized=? WHERE user_id=?"+UsersDB.getDomainIdSqlWhere(true));
						ps.setString(1, "");
						ps.setBoolean(2, false);
						ps.setInt(3, createdUserId);
						ps.execute();
						ps.close();

						if (logUser != null)
						{
							Adminlog.add(Adminlog.TYPE_USER_SAVE, "subscribeUnsubscribe removeGroups, login="+logUser.getLogin()+" email="+logUser.getEmail(), -1, -1);
						}
					}
					else //disable
					{
						//	toto tu je keby sa nepodarilo vymazanie (je to napr. admin)
						ps = db_conn.prepareStatement("UPDATE  users SET authorized=? WHERE user_id=?"+UsersDB.getDomainIdSqlWhere(true));
						ps.setBoolean(1, false);
						ps.setInt(2, createdUserId);
						ps.execute();
						ps.close();

						if (logUser != null)
						{
							Adminlog.add(Adminlog.TYPE_USER_SAVE, "subscribeUnsubscribe disable user, login="+logUser.getLogin()+" email="+logUser.getEmail(), -1, -1);
						}
					}
				}
				else
				{
					//toto tu je keby sa nepodarilo vymazanie (je to napr. admin)
					ps = db_conn.prepareStatement("UPDATE  users SET user_groups=?, authorized=? WHERE user_id=?"+UsersDB.getDomainIdSqlWhere(true));
					ps.setString(1, userGroups);
					//TODO: toto je potencionalne bezpecnostne riziko, ale ked reaktivujem skupiny, musim ho povolit
					ps.setBoolean(2, true);
					ps.setInt(3, createdUserId);
					ps.execute();
					ps.close();

					if (logUser != null)
					{
						Adminlog.add(Adminlog.TYPE_USER_SAVE, "subscribeUnsubscribe removeGroups2 user, login="+logUser.getLogin()+" email="+logUser.getEmail(), -1, -1);
					}
				}

				if (loggedUser != null)
				{
					loggedUser.setUserGroupsIds(userGroups);
				}
			}
			else
			{
				//zapiseme to do temp databazy s nejakym hashom a posleme authorize linku
				StringBuilder hash = new StringBuilder();
				for (i = 0; i < 4; i++)
				{
					hash.append(rand.nextInt(9));
				}

				long now = (new java.util.Date()).getTime();
				hash.insert(0, '-').insert(0, now);

				sql = "INSERT INTO user_group_verify (user_id, user_groups, hash, create_date, email, hostname) VALUES (?, ?, ?, ?, ?, ?)";
				ps = db_conn.prepareStatement(sql);
				ps.setInt(1, createdUserId);
				ps.setString(2, userGroups);
				ps.setString(3, hash.toString());
				ps.setTimestamp(4, new Timestamp(now));
				ps.setString(5, user.getEmail());
				ps.setString(6, Tools.getRemoteHost(request));
				ps.execute();
				ps.close();

				//posli autorizacny email
				emailBody = Tools.replace(emailBody, "!HASH!", hash.toString());
				boolean success = SendMail.send(emailSenderName, emailSenderEmail, user.getEmail(), emailSubject, emailBody);
				if (success==false)
				{
					createdUserId = -2;
				}

				Adminlog.add(Adminlog.TYPE_USER_SAVE, "subscribeUnsubscribe insert user_group_verify user, login="+user.getLogin()+" email="+user.getEmail(), -1, -1);

			}

			rs = null;
			ps = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (db_conn!=null) db_conn.close();
				if (rs!=null) rs.close();
				if (ps!=null) ps.close();
			}
			catch (Exception ex2)
			{

			}
		}

		return(createdUserId);
	}

	public static UserDetails authorizeEmail(HttpServletRequest request, String hash)
	{
		UserDetails user = null;
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection(request);
			ps = db_conn.prepareStatement("SELECT * FROM user_group_verify WHERE hash=?");
			ps.setString(1, hash);
			rs = ps.executeQuery();
			int verifyId = -1;
			int userId = -1;
			String userGroups = null;
			if (rs.next())
			{
				verifyId = rs.getInt("verify_id");
				userId = rs.getInt("user_id");
				userGroups = DB.getDbString(rs, "user_groups");
			}
			rs.close();
			ps.close();



			if (verifyId > 0)
			{
				if (Tools.isEmpty(userGroups))
				{
					user = getUser(userId);

					//pridaj ho do databazy zakazanych emailov
					if (user != null) EmailDB.addUnsubscribedEmail(user.getEmail());

					String mode = Constants.getString("dmailUnsubscribeMode");
					if ("delete".equals(mode))
					{
						//nema ziadne skupiny - mazem z DB
						ps = db_conn.prepareStatement("DELETE FROM  users WHERE user_id=? AND is_admin=?"+UsersDB.getDomainIdSqlWhere(true));
						ps.setInt(1, userId);
						ps.setBoolean(2, false);
						ps.execute();
						ps.close();

						//toto tu je keby sa nepodarilo vymazanie (je to napr. admin)
						ps = db_conn.prepareStatement("UPDATE  users SET user_groups=?, authorized=? WHERE user_id=? AND is_admin=?"+UsersDB.getDomainIdSqlWhere(true));
						ps.setString(1, "");
						ps.setBoolean(2, true);
						ps.setInt(3, userId);
						ps.setBoolean(4, true);
						ps.execute();
						ps.close();
					}
					else if ("removeGroups".equals(mode))
					{
						//toto tu je keby sa nepodarilo vymazanie (je to napr. admin)
						ps = db_conn.prepareStatement("UPDATE  users SET user_groups=?, authorized=? WHERE user_id=?"+UsersDB.getDomainIdSqlWhere(true));
						ps.setString(1, "");
						ps.setBoolean(2, false);
						ps.setInt(3, userId);
						ps.execute();
						ps.close();
					}
					else //disable
					{
						//	toto tu je keby sa nepodarilo vymazanie (je to napr. admin)
						ps = db_conn.prepareStatement("UPDATE  users SET authorized=? WHERE user_id=?"+UsersDB.getDomainIdSqlWhere(true));
						ps.setBoolean(1, false);
						ps.setInt(2, userId);
						ps.execute();
						ps.close();
					}

					//updatni datum verifikacie
					ps = db_conn.prepareStatement("UPDATE user_group_verify SET verify_date=? WHERE verify_id=?");
					ps.setTimestamp(1, new Timestamp((new java.util.Date()).getTime()));
					ps.setInt(2, verifyId);
					ps.execute();
					ps.close();

					//updatni hostname verifikacie
					ps = db_conn.prepareStatement("UPDATE user_group_verify SET hostname=? WHERE verify_id=?");
					ps.setString(1, Tools.getRemoteHost(request));
					ps.setInt(2, verifyId);
					ps.execute();
					ps.close();
				}
				else
				{
					//updatni tabulku users
					ps = db_conn.prepareStatement("UPDATE  users SET user_groups=?, authorized=? WHERE user_id=?"+UsersDB.getDomainIdSqlWhere(true));
					ps.setString(1, userGroups);
					ps.setBoolean(2, true);
					ps.setInt(3, userId);
					ps.execute();
					ps.close();

					//updatni datum verifikacie
					ps = db_conn.prepareStatement("UPDATE user_group_verify SET verify_date=? WHERE verify_id=?");
					ps.setTimestamp(1, new Timestamp((new java.util.Date()).getTime()));
					ps.setInt(2, verifyId);
					ps.execute();
					ps.close();

					//updatni hostname verifikacie
					ps = db_conn.prepareStatement("UPDATE user_group_verify SET hostname=? WHERE verify_id=?");
					ps.setString(1, Tools.getRemoteHost(request));
					ps.setInt(2, verifyId);
					ps.execute();
					ps.close();

					//	nacitaj usera
					user = getUser(userId);

					//vymaz ho zo zakazanych emailov
					EmailDB.deleteUnsubscribedEmail(user.getEmail());
				}


			}

			rs = null;
			ps = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
			user = null;
		}
		finally
		{
			try
			{
				if (db_conn!=null) db_conn.close();
				if (rs!=null) rs.close();
				if (ps!=null) ps.close();
			}
			catch (Exception ex2)
			{

			}
		}

		return(user);
	}

	public static boolean sendPassword(HttpServletRequest request, String login)
	{
		String emailParam = null;
		if (login != null && login.contains("@")) emailParam = login;

		return(sendPassword(request, login, emailParam));
	}

	/**
	 * Odoslanie hesla na email adresu
	 * @param request
	 * @param login - prihlasovacie meno
	 * @param emailParam - email (ak je zadany email, hlada sa podla emailu, login moze byt vtedy null), alebo null
	 * @return
	 */
	public static boolean sendPassword(HttpServletRequest request, String login, String emailParams)
	{
		String emailParam = emailParams;
		if (SpamProtection.canPost("passwordSend", null, request)==false)
		{
			Prop prop = Prop.getInstance(Constants.getServletContext(), request);

			Logger.error(UsersDB.class, prop.getText("logon.error.blocked"));
			request.setAttribute("errors", prop.getText("logon.error.blocked"));
			request.setAttribute("error.logon.user.blocked", "true");
			return false;
		}

		String method = Constants.getString("sendPasswordMethod");
		if (Tools.isNotEmpty(method))
		{
			int i = method.lastIndexOf('.');
			String clazz = method.substring(0, i);
			method = method.substring(i+1);
			//String
			try
			{
				Class<?> c = Class.forName(clazz);
				Object o = c.getDeclaredConstructor().newInstance();
				Method m;
				Class<?>[] parameterTypes = new Class<?>[] {HttpServletRequest.class, String.class, String.class};
				Object[] arguments = new Object[] {request, login, emailParam};
				m = c.getMethod(method, parameterTypes);
				return((boolean)m.invoke(o, arguments));
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
				return false;
			}
		}

		UserDetails user = new UserDetails();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{

			db_conn = DBPool.getConnection(request);
			String sql;

			//asi zadal email namiesto loginu, hladajme podla emailu
			if (login != null && emailParam == null && login.contains("@"))
				emailParam = login;

			sql = "SELECT * FROM  users WHERE login=?"+UsersDB.getDomainIdSqlWhere(true);
			String param = login;
			if (Tools.isNotEmpty(emailParam))
			{
				sql = "SELECT * FROM  users WHERE email=?"+UsersDB.getDomainIdSqlWhere(true);
				param = emailParam;
			}
			sql += " ORDER BY user_id ASC, is_admin DESC";
			ps = db_conn.prepareStatement(sql);
			ps.setString(1, param);
			rs = ps.executeQuery();
			if(rs.next())
			{
				fillUserDetails(user, rs);
			}
			rs.close();
			ps.close();
			db_conn.close();
			db_conn = null;
			rs = null;
			ps = null;

			if(isEmpty(user.getEmail()) || user.getEmail().indexOf('@')==-1)
			{
				if (Constants.getBoolean("formLoginProtect"))
				{
					//nastavime na uspech aj ked user neexistuje aby sa to nedalo rozlisit
					request.setAttribute("passResultEmail", "@");
				}
				else
				{
					Logger.println(UsersDB.class,"K zadanemu uzivatelovi neexistuje email");
					request.setAttribute("passResultEmailFail","true");
				}
			}
			else
			{
				sendPasswordEmail(request, user);
				return (true);
			}
		}
		catch (Exception ex){sk.iway.iwcm.Logger.error(ex);}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
		return (false);
	}

	public static void sendPasswordEmail(HttpServletRequest request, UserDetails user) throws Exception
	{
		Prop prop = Prop.getInstance(Constants.getServletContext(), request);
		//if we are able to decrypt his/her original password
		String subject = prop.getText("logon.mail.lost_password") + " " + Tools.getBaseHref(request);
		if (!Constants.getBoolean("passwordUseHash"))
		{
			String text;
			text = prop.getText("logon.mail.message") + "\n";
			text += prop.getText("logon.mail.login_name") + ": " + user.getLogin() + "\n";
			text += prop.getText("logon.mail.password") + ": " + user.getPassword() + "\n";
			// from fromEmail toEmail subject text
			SendMail.send(user.getFullName(), user.getEmail(), user.getEmail(), subject, text);
		}else{
			int randomNumber = new SecureRandom().nextInt();
			String loginHash = new Password().encrypt(user.getLogin());
			String auth = new Password().encrypt(Integer.toString(randomNumber));
			Adminlog.add(Adminlog.TYPE_USER_CHANGE_PASSWORD, user.getUserId(), "Vyžiadanie zmeny hesla", randomNumber, APPROVE_APPROVE);
			//String text = prop.getText("logon.password.change_at")+"\n";

			String pageUrl = Constants.getString("changePasswordPageUrl");
			if (request !=null && request.getAttribute("sendPasswordUrl")!=null) pageUrl = (String)request.getAttribute("sendPasswordUrl");

			pageUrl = Tools.getBaseHref(request) + pageUrl + "?login="+loginHash+"&auth="+auth;

			String propKey = Tools.getRequestAttribute(request,  "sendPasswordTextKey", "logon.password.changeEmailText");
			String subjectKey = Tools.getRequestAttribute(request,  "sendPasswordSubjectKey", null);
			if (subjectKey != null)
			{
				subject = prop.getText(subjectKey, Tools.getBaseHref(request), DocDB.getDomain(request));
			}

			String fromName = Tools.getRequestAttribute(request,  "sendPasswordFromName", user.getFullName());
			String fromEmail = Tools.getRequestAttribute(request,  "sendPasswordFromEmail", user.getEmail());

			String text = prop.getText(propKey, pageUrl, String.valueOf(Constants.getInt("passwordResetValidityInMinutes")));

			new MailHelper().
				setFromEmail(fromEmail).
				setFromName(fromName).
				addRecipient(user.getEmail()).
				setSubject(subject).
				setMessage(text).
				send();
		}
		if (request!=null) request.setAttribute("passResultEmail", user.getEmail());
	}

	/**
	 * Vymaze z DB pozadovaneho usera
	 * @param userId
	 * @return
	 */
    public static String deleteUser(int userId)
    {
        return deleteUser(userId,null);
    }

	public static String deleteUser(int userId, String descriptionPrefix)
	{
		UserDetails user = null;
		try
		{
		    if(descriptionPrefix == null)
                descriptionPrefix = "";
			user = UsersDB.getUser(userId);
			if (user != null)	Adminlog.add(Adminlog.TYPE_USER_DELETE, descriptionPrefix+"Delete user login="+user.getLogin()+" name="+user.getFullName()+" email="+user.getEmail()+" groups="+user.getUserGroupsIds(), userId, -1);
			else Adminlog.add(Adminlog.TYPE_USER_DELETE, descriptionPrefix+"Delete user "+userId, userId, -1);

			Connection con = DBPool.getConnection();
			String sql = "DELETE FROM  users WHERE user_id=?"+UsersDB.getDomainIdSqlWhere(true);
			PreparedStatement ps = con.prepareStatement(sql);
			ps.setInt(1, userId);
			ps.execute();
			ps.close();

			ps = con.prepareStatement("DELETE FROM groups_approve WHERE user_id=?");
			ps.setInt(1, userId);
			ps.execute();
			ps.close();

			ps = con.prepareStatement("DELETE FROM user_disabled_items WHERE user_id=?");
			ps.setInt(1, userId);
			ps.execute();
			ps.close();

			ps = con.prepareStatement("DELETE FROM users_in_perm_groups WHERE user_id=?");
			ps.setInt(1, userId);
			ps.execute();
			ps.close();

			con.close();

				PasswordsHistoryDB.getInstance().deleteAllByUserId(userId);

				UsersDB.removeUserFromCache(userId);
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		//trigger po zmene udajov pouzivatela - podobne ako u usrLogon
		if ( Tools.isNotEmpty(Constants.getString("userAfterDeleteMethod") ))
		{
			try
			{
				String saveMethod = Constants.getString("userAfterDeleteMethod");
				String clazzName = saveMethod.substring( 0, saveMethod.lastIndexOf('.'));
				String methodName = saveMethod.substring( clazzName.length() +1 );
				Class<?> clazz = Class.forName(clazzName);
				boolean skipWithoutRequest = false;
				try
				{
					Method method = clazz.getMethod(methodName, HttpServletRequest.class, UserDetails.class, UserDetails.class);
					method.invoke(null, null/*request nemam*/, user,null);
					skipWithoutRequest=true;
				}
				catch (NoSuchMethodException nsme) {/*do nothing*/}
				if (!skipWithoutRequest)
				{
					Method method = clazz.getMethod(methodName, UserDetails.class, UserDetails.class);
					method.invoke(null, user,null);
				}

			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}
		return ("success");
	}

	/**
	 * Ulozenie pouzivatela do DB
	 * @param user
	 * @return
	 */
	public static boolean saveUser(UserDetails user)
	{
		UserDetails oldUser = UsersDB.getUser(user.getLogin());
		boolean saveOK = false;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		if (oldUser != null)
		{
			logChanges(oldUser, user);
		}

		try
		{
			db_conn = DBPool.getConnection();

			String sql = "INSERT INTO  users (title, first_name, last_name, login, is_admin, user_groups, authorized," +
							" company, adress, PSC, country, email, phone, editable_groups, " +
							" city, editable_pages, writable_folders, reg_date,"+
							" field_a, field_b, field_c, field_d, field_e, " +
							" date_of_birth, sex_male, photo, signature, forum_rank, rating_rank, fax, "+
							" delivery_city, delivery_company, delivery_country, delivery_first_name, delivery_last_name," +
							" delivery_phone, delivery_psc, delivery_adress, position, parent_id, allow_login_start, allow_login_end, domain_id) " +
							" VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

			if (user.getUserId() > 0)
			{
				sql = "UPDATE  users SET title=?, first_name=?, last_name=?, login=?, is_admin=?, user_groups=?, authorized=?," +
				" company=?, adress=?, PSC=?, country=?, email=?, phone=?, editable_groups=?, " +
				" city=?, editable_pages=?, writable_folders=?, reg_date=?,"+
				" field_a=?, field_b=?, field_c=?, field_d=?, field_e=?, " +
				" date_of_birth=?, sex_male=?, photo=?, signature=?, fax=?, " +
				" delivery_city=?, delivery_company=?, delivery_country=?, delivery_first_name=?, delivery_last_name=?, "+
				" delivery_phone=?, delivery_psc=?, delivery_adress=?, position=?, parent_id=?, allow_login_start=?, allow_login_end=?" +
				" WHERE user_id=?"+UsersDB.getDomainIdSqlWhere(true);
			}

			int i=1;
			ps = db_conn.prepareStatement(sql);
			ps.setString(i++, DB.prepareString(user.getTitle(), 16));
			ps.setString(i++, user.getFirstName());
			ps.setString(i++, user.getLastName());
			ps.setString(i++, user.getLogin());
			ps.setBoolean(i++, user.isAdmin());
			String userGroups = user.getUserGroupsIds();
			if (userGroups==null)
			{
				ps.setNull(i++, java.sql.Types.VARCHAR);
			}
			else
			{
				//ochrana pred duplicitami
				String newUserGroups = null;
				String[] ugArr = userGroups.split(",");
				for (String ug : ugArr)
				{
					if (newUserGroups == null) newUserGroups = ug;
					else if ((","+newUserGroups+",").indexOf(","+ug+",")==-1) newUserGroups = newUserGroups + "," + ug; //NOSONAR
				}
				ps.setString(i++, newUserGroups);
			}

			ps.setBoolean(i++, user.isAuthorized());
			ps.setString(i++, user.getCompany());
			ps.setString(i++, user.getAdress());
			ps.setString(i++, user.getPSC());
			ps.setString(i++, user.getCountry());
			ps.setString(i++, user.getEmail());
			ps.setString(i++, user.getPhone());

			String editableGroups = user.getEditableGroups();
			//ochrana pred duplicitami
			String newEditableGroups = null;
			if (Tools.isNotEmpty(editableGroups))
			{
				String[] ugArr = editableGroups.split(",");
				for (String ug : ugArr)
				{
					if (newEditableGroups == null) newEditableGroups = ug;
					else if ((","+newEditableGroups+",").indexOf(","+ug+",")==-1) newEditableGroups = newEditableGroups + "," + ug; //NOSONAR
				}
			}

			ps.setString(i++, newEditableGroups);

			ps.setString(i++, user.getCity());
			ps.setString(i++, user.getEditablePages());
			ps.setString(i++, user.getWritableFolders());

			if (user.getRegDate() > 1000 && user.getUserId() > 0)
			{
				ps.setTimestamp(i++, new Timestamp(user.getRegDate()));
			}
			else
			{
				ps.setTimestamp(i++, new Timestamp(Tools.getNow()));
			}

			ps.setString(i++, user.getFieldA());
			ps.setString(i++, user.getFieldB());
			ps.setString(i++, user.getFieldC());
			ps.setString(i++, user.getFieldD());
			ps.setString(i++, user.getFieldE());

			//	ps.setTimestamp(i++, new Timestamp(DB.getTimestamp(user.getDateOfBirth(), null)));
			long t = DB.getTimestamp(user.getDateOfBirth(), null);
			if (Tools.isEmpty(user.getDateOfBirth()) || t == 0)
			{
				ps.setNull(i++, Types.TIMESTAMP);
			}
			else
			{
				ps.setTimestamp(i++, new Timestamp(t));
			}

			ps.setBoolean(i++, user.isSexMale());
			ps.setString(i++, user.getPhoto());
			ps.setString(i++,  Html2Text.html2text(user.getSignature()));

			ps.setString(i++, user.getFaxId());
			ps.setString(i++, user.getDeliveryCity());
			ps.setString(i++, user.getDeliveryCompany());
			ps.setString(i++, user.getDeliveryCountry());
			ps.setString(i++, user.getDeliveryFirstName());
			ps.setString(i++, user.getDeliveryLastName());
			ps.setString(i++, user.getDeliveryPhone());
			ps.setString(i++, user.getDeliveryPsc());
			ps.setString(i++, user.getDeliveryAdress());
			ps.setString(i++, user.getPositionId());
			ps.setInt(i++, user.getParentId());

			if (Tools.isNotEmpty(user.getAllowLoginStart())) ps.setTimestamp(i++, new Timestamp(DB.getTimestamp(user.getAllowLoginStart())));
			else ps.setNull(i++, Types.TIMESTAMP);
			if (Tools.isNotEmpty(user.getAllowLoginEnd())) ps.setTimestamp(i++, new Timestamp(DB.getTimestamp(user.getAllowLoginEnd())));
			else ps.setNull(i++, Types.TIMESTAMP);

			if (user.getUserId()<1) ps.setInt(i++, CloudToolsForCore.getDomainId());

			if (user.getUserId() > 0)
			{
				ps.setInt(i++, user.getUserId());
			}

			try
			{
				ps.execute();
			}
			catch (Exception ex2)
			{
				//zamedzime tym chybe Cannot insert duplicate key row in object 'dbo.users' with unique index 'IX_login_name'.
				//a podari sa nam vratit user_id spravne
				sk.iway.iwcm.Logger.error(ex2);
			}
			ps.close();

			if (user.getUserId() < 1)
			{
				Logger.debug(UsersDB.class, "Getting new userId form login " + user.getLogin());

				ps = db_conn.prepareStatement("SELECT max(user_id) AS user_id FROM  users WHERE login=?"+UsersDB.getDomainIdSqlWhere(true));
				ps.setString(1, user.getLogin());
				rs = ps.executeQuery();
				if (rs.next())
				{
					user.setUserId(rs.getInt("user_id"));
				}
				rs.close();
				ps.close();

				Logger.debug(UsersDB.class, "userId="+user.getUserId());
			}
			rs = null;
			ps = null;

			db_conn.close();
			db_conn = null;

			if (user.isSettingsDontSave()==false && user.getSettings()!=null && user.getSettingsNotLoad().size()>0)
			{
				setSettings(user.getUserId(), user.getSettings());
			}
			//reset hodnoty
			user.setSettingsDontSave(false);

			saveOK = true;

			//save password if it's changed
			if (Tools.isNotEmpty(user.getPassword()) && UserTools.PASS_UNCHANGED.equals(user.getPassword())==false) {
				UserDetailsService.savePassword(user.getUserId(), user.getPassword());
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
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}

		//trigger po zmene udajov pouzivatela - podobne ako u usrLogon
		if ( Tools.isNotEmpty(Constants.getString("userAfterSaveMethod") ))
		{
			try
			{
				String saveMethod = Constants.getString("userAfterSaveMethod");
				String clazzName = saveMethod.substring( 0, saveMethod.lastIndexOf('.'));
				String methodName = saveMethod.substring( clazzName.length() +1 );
				Class<?> clazz = Class.forName(clazzName);
				boolean skipWithoutRequest = false;
				try
				{
					Method method = clazz.getMethod(methodName, HttpServletRequest.class, UserDetails.class, UserDetails.class);
					method.invoke(null, null/*request nemam*/, oldUser,UsersDB.getUser( user.getLogin() ));
					skipWithoutRequest=true;
				}
				catch (NoSuchMethodException nsme) {/*do nothing*/}
				if (!skipWithoutRequest)
				{
					Method method = clazz.getMethod(methodName, UserDetails.class, UserDetails.class);
					method.invoke(null, oldUser,UsersDB.getUser( user.getLogin() ));
				}

			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}

		UsersDB.removeUserFromCache(user.getUserId());

		return(saveOK);
	}

	static void logChanges(UserDetails oldUser, UserDetails user)
	{
		BeanDiff diff = new BeanDiff().setNew(user).setOriginal(oldUser).blacklist("password", "salt", "settingsDontSave");
		if (diff.diff().size() == 0)
			return;
		StringBuilder message = new StringBuilder().append(user.getLogin()).append('\n').append(new BeanDiffPrinter(diff));
		Adminlog.add(Adminlog.TYPE_USER_UPDATE, message.toString(), user.getUserId(), -1);
	}

	/**
	 * Vygeneruje a nastavi do DB authorize_hash
	 * @param userId
	 * @return
	 */
	public static String getGenerateAuthorizeHash(int userId)
	{
		String hash = null;

		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			hash = Password.generatePassword(32);

			Logger.debug(UsersDB.class, "getGenerateAuthorizeHash - userId="+userId+" hash="+hash);


			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("UPDATE  users SET authorize_hash=? WHERE user_id=?"+UsersDB.getDomainIdSqlWhere(true));
			ps.setString(1, hash);
			ps.setInt(2, userId);
			ps.execute();
			ps.close();
			db_conn.close();
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			hash = null;
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

		return hash;
	}

	public static boolean checkHast(int userId, String hash)
	{
		boolean ok = false;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{

			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT * FROM  users WHERE authorize_hash=? AND user_id=?"+UsersDB.getDomainIdSqlWhere(true));
			ps.setString(1, hash);
			ps.setInt(2, userId);
			rs = ps.executeQuery();
			if (rs.next())
			{
				ok = true;
			}
			rs.close();
			ps.close();

			if (ok)
			{
				//vymaz hash (aby sa nemohol znovaautorizovat, ked ho admin zadisabluje)
				ps = db_conn.prepareStatement("UPDATE  users SET authorized=?, authorize_hash=? WHERE user_id=?"+UsersDB.getDomainIdSqlWhere(true));
				ps.setBoolean(1, true);
				ps.setNull(2, Types.VARCHAR);
				ps.setInt(3, userId);
				ps.execute();
				ps.close();
			}

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
			}
		}

		return(ok);
	}

	public static boolean checkHash(int userId, String hash) {
			boolean result = false;

			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try {

				db_conn = DBPool.getConnection();
				ps = db_conn.prepareStatement("SELECT * FROM  users WHERE authorize_hash=? AND user_id=?" + UsersDB.getDomainIdSqlWhere(true));
				ps.setString(1, hash);
				ps.setInt(2, userId);
				rs = ps.executeQuery();
				if (rs.next()) {
					result = true;
				}
			} catch (Exception e) {
				sk.iway.iwcm.Logger.error(e);
			} finally {
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
				}
			}
			return result;
	}

	public static List<UserDetails> getUsers(SelectionFilter<UserDetails> filter)
	{
		List<UserDetails> users = getUsers();
		List<UserDetails> passingUsers = new ArrayList<>();

		for (UserDetails userDetails : users)
			if( filter.fullfilsConditions(userDetails) )
				passingUsers.add(userDetails);
		return passingUsers;
	}

	/**
	 * Vrati zoznam vsetkych pouzivatelov v DB
	 * @return zoznam vsetkych uzivatelov
	 */
	public static List<UserDetails> getUsers()
	{
		List<UserDetails> users=new ArrayList<>();
		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{

			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT * FROM  users WHERE "+UsersDB.getDomainIdSqlWhere(false)+" ORDER BY last_name, first_name");
			rs = ps.executeQuery();
			UserDetails usr;

			while (rs.next())
			{
				usr = new UserDetails(rs);
				users.add(usr);
			}
			rs.close();
			ps.close();
			db_conn.close();
			db_conn = null;
			rs = null;
			ps = null;
		}
		catch (Exception ex){sk.iway.iwcm.Logger.error(ex);}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
		return users;
	}

	/** autorizuje uzivatela
	 * @param userId
	 */
	public static void authorizeUser(int userId)
	{

		try
		{

			java.sql.Connection db_conn = DBPool.getConnection();
			java.sql.PreparedStatement ps = db_conn.prepareStatement("UPDATE  users SET authorized = 1 WHERE user_id = ?"+UsersDB.getDomainIdSqlWhere(true));
			ps.setInt(1, userId);
			ps.executeUpdate();
			ps.close();
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
	}

	/**
	 * Vrati zoznam vsetkych adminov
	 * @return zoznam vsetkych adminov
	 */
	public static List<UserDetails> getAdmins()
	{
		List<UserDetails> admins=new ArrayList<>();
		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{

			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT * FROM  users WHERE is_admin = 1 "+UsersDB.getDomainIdSqlWhere(true)+" ORDER BY last_name, first_name");
			rs = ps.executeQuery();
			UserDetails usr;

			while (rs.next())
			{
				usr = new UserDetails(rs);
				admins.add(usr);
			}
			rs.close();
			ps.close();
			db_conn.close();
			db_conn = null;
			rs = null;
			ps = null;
		}
		catch (Exception ex){sk.iway.iwcm.Logger.error(ex);}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
		return admins;
	}

	/**
	 * @return UserGroupVerify pre usera
	 */
	public static UserGroupVerify getUserGroupVerify(int userId)
	{
		UserGroupVerify ugv=null;
		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT * FROM user_group_verify WHERE user_id = ? AND verify_date IS NOT NULL ORDER BY verify_date DESC");
			ps.setInt(1, userId);
			rs = ps.executeQuery();

			if (rs.next())
			{
				ugv = new UserGroupVerify();
				ugv.setCreateDate(rs.getTimestamp("create_date"));
				ugv.setEmail(rs.getString("user_id"));
				ugv.setHash(rs.getString("hash"));
				ugv.setHostname(rs.getString("hostname"));
				ugv.setId(rs.getInt("user_id"));
				ugv.setUserGroups(rs.getString("user_groups"));
				ugv.setUserId(rs.getInt("user_id"));
				ugv.setVerifyDate(rs.getTimestamp("verify_date"));
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
		return ugv;
	}

	/**
	 * Prida zadanemu pouzivatelovi nove user skupiny
	 * @param usr
	 * @param groupsParams
	 */
	public static void addUserGroups(UserDetails usr, String[] groupsParams)
	{
		if (groupsParams==null || groupsParams.length<1) return;

		String newUserGroups = usr.getUserGroupsIds();
		if (newUserGroups==null) newUserGroups = "";
		for (int i=0; i<groupsParams.length; i++)
		{
			int groupId = Tools.getIntValue(groupsParams[i], -1);
			if (groupId < 1) continue;
			if ((","+newUserGroups+",").indexOf(","+groupId+",")==-1)
			{
				if (Tools.isEmpty(newUserGroups)) newUserGroups = Integer.toString(groupId);
				else newUserGroups += ","+groupId; //NOSONAR
			}
		}

		if (Tools.isNotEmpty(usr.getUserGroupsIds()) && usr.getUserGroupsIds().equals(newUserGroups)) return;

		if (Tools.isEmpty(newUserGroups)) newUserGroups = null;

		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{

			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("UPDATE  users SET user_groups=? WHERE user_id=?"+UsersDB.getDomainIdSqlWhere(true));
			ps.setString(1, newUserGroups);
			ps.setInt(2, usr.getUserId());
			ps.execute();
			ps.close();
			db_conn.close();
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

	/**
	 * Ziska objekt aktualne prihlaseneho pouzivatela, alebo null
	 * @param session
	 * @return
	 */
	public static Identity getCurrentUser(HttpSession session)
	{
		if (session == null) return null;
		Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
		return user;
	}

	/**
	 * Ziska objekt aktualne prihlaseneho pouzivatela, alebo null
	 * @param request
	 * @return
	 */
	public static Identity getCurrentUser(HttpServletRequest request)
	{
		if (request == null) return null;
		return getCurrentUser(request.getSession());
	}

	/**
	 * Ziska objekt aktualne prihlaseneho pouzivatela, alebo null
	 * @param context
	 * @return
	 */
	public static Identity getCurrentUser(ActionBeanContext context)
	{
		return getCurrentUser(context.getRequest().getSession());
	}

	/**
	 * Vrati nahodny 16 znakovy retazec pouzitelny pre vygenerovanie prihlasovacieho mena
	 * @return
	 */
	public static String getRandomLogin()
	{
		Calendar cal = Calendar.getInstance();
		String login = cal.get(Calendar.YEAR)+cal.get(Calendar.DAY_OF_YEAR)+cal.get(Calendar.HOUR_OF_DAY)+cal.get(Calendar.MINUTE)+Password.generatePassword(5);
		return login;
	}

	/**
	 * Vytvori login z mena a priezviska.
	 * Pre meno: Maros Urbanec
	 * Primarne vytvori login ako priezvisko.toLowerCase() - urbanec
	 * Ak existuje, skusi pridat prve pismeno z mena - urbanecm
	 * Ak existuje, skuska postupne loginy urbanecm1,urbanecm2,...
	 * az pokym nenajde cislo, pre ktore login este neexistuje.
	 *
	 * Vyhodi {@link IllegalArgumentException} ak su mu dodane prazdne parametre
	 *
	 */
	public static String generateNameLogin(String firstName,String lastName)
	{
		if(Tools.isEmpty(firstName) || Tools.isEmpty(lastName))
			throw new IllegalArgumentException("Nemozem vytvorit login - jeden z argumentov je null");

		String login = lastName.toLowerCase();
		boolean loginExists = getUser(login) != null;
		if (!loginExists)
			return login;
		login = login+lastName.toLowerCase().charAt(0);
		loginExists = getUser(login) != null;
		if (!loginExists)
			return login;
		String baseLogin = login;
		int loginCounter = 1;
		do
		{
			login = baseLogin+loginCounter;
			loginExists = getUser(login) != null;
			loginCounter++;
		}while(loginExists);
		return login;
	}

	/**
	 * Naplni SettingsBean z rs objektu
	 * @param settings
	 * @param rs
	 * @throws Exception
	 */
	public static void fillSettingsBean(SettingsBean settings, ResultSet rs) throws Exception
	{
		settings.setUserSettingsId(rs.getInt("user_settings_id"));
		settings.setUserId(rs.getInt("user_id"));
		settings.setSkey(DB.getDbString(rs, "skey"));
		settings.setSvalue1(DB.getDbString(rs, "svalue1"));
		settings.setSvalue2(DB.getDbString(rs, "svalue2"));
		settings.setSvalue3(DB.getDbString(rs, "svalue3"));
		settings.setSvalue4(DB.getDbString(rs, "svalue4"));
		settings.setSint1(rs.getInt("sint1"));
		settings.setSint2(rs.getInt("sint2"));
		settings.setSint3(rs.getInt("sint3"));
		settings.setSint4(rs.getInt("sint4"));
		settings.setSdate(rs.getTimestamp("sdate"));
	}

	/**
	 * Vrati nastavenia pouzivatela z tabulky user_settings
	 * @param userId
	 * @param countDown max pocet rekurzivnych volani pri neuspesnom nacitani settingsov
	 *
	 * @return
	 */
	public static Map<String, SettingsBean> getSettings(int userId, int countDown)
	{
		Map<String, SettingsBean> settings = new TreeMap<>();
		boolean success = false;
		countDown--;
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			// MSSQL Deadlock repair
			//ps = db_conn.prepareStatement("SELECT * FROM user_settings WHERE user_id=?");
			ps = StatNewDB.prepareStatement(db_conn, "SELECT * FROM user_settings WHERE user_id=?");
			ps.setInt(1, userId);
			rs = ps.executeQuery();
			while (rs.next())
			{
				SettingsBean sb = new SettingsBean();
				fillSettingsBean(sb, rs);
				if (sb.getSkey()==null) continue;
				settings.put(sb.getSkey(), sb);
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
			success = true;
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
			}
		}
		while (!success && countDown>0)
		{
			try
			{
				Thread.sleep(200);
			} catch (InterruptedException e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
			settings = getSettings(userId, countDown);
		}
		return settings;
	}

	public static Map<String, SettingsBean> getSettings(int userId)
	{
		return getSettings(userId, 5);
	}
	/**
	 * Odfiltruje nastavenia podla nejakeho prefixu a usporiada podla nazvov
	 * @param prefix - mojeOdkazy
	 * @param allSettings
	 * @return vrati hodnoty ako mojeOdkazy@1, mojeOdkazy@2.. usporiadane podla cisla za znakom @
	 */
	public static List<SettingsBean> filterSettingsByPrefix(String prefix, Map<String, SettingsBean> allSettings)
	{
		List<SettingsBean> settings =new ArrayList<>();

		for (Entry<String, SettingsBean> entry : allSettings.entrySet())
		{
			SettingsBean sb = entry.getValue();
			if (sb==null || prefix==null || sb.getSkey()==null) continue;

			if (sb.getSkey().startsWith(prefix)) settings.add(sb);
		}

		//usporiadaj podla cisla za znakom @
		Collections.sort(settings,
			new Comparator<SettingsBean>()
			{
			@Override
				public int compare(SettingsBean o1, SettingsBean o2)
				{
					try
					{
						int i1 = o1.getSkey().indexOf('@');
						int i2 = o2.getSkey().indexOf('@');
						if (i1 > 0 && i2>0)
						{
							i1 = Tools.getIntValue(o1.getSkey().substring(i1+1), -1);
							i2 = Tools.getIntValue(o2.getSkey().substring(i2+1), -1);
							if (i1!=-1 && i2!=-1)
							{
								return i1-i2;
							}
						}
					}
					catch (Exception e)
					{
						sk.iway.iwcm.Logger.error(e);
					}

					return o1.getSkey().compareTo(o2.getSkey());
				}
			});

		return settings;
	}

	/**
	 * Ulozi nastavenie pouzivatela do databazy
	 * @param userId
	 * @param settings
	 * @return
	 */
	public static boolean setSettings(int userId, Map<String, SettingsBean> settings)
	{
		boolean saveOK = false;

		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("DELETE FROM user_settings WHERE user_id=?");
			ps.setInt(1, userId);
			ps.execute();
			ps.close();

			ps = db_conn.prepareStatement("INSERT INTO user_settings (user_id, skey, svalue1, svalue2, svalue3, svalue4, sint1, sint2, sint3, sint4, sdate) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			int psIndex = 1;
			ps.setInt(psIndex++, userId);

			for (Entry<String, SettingsBean> entry : settings.entrySet())
			{
				try
				{
					SettingsBean sb = entry.getValue();
					if (sb == null || "-".equals(entry.getKey()) || "__delete".equals(sb.getSvalue1())) continue;
					sb.setSkey(entry.getKey());

					psIndex = 2;
					ps.setString(psIndex++, DB.prepareString(sb.getSkey(), 64));
					ps.setString(psIndex++, DB.prepareString(sb.getSvalue1(), 3000));
					ps.setString(psIndex++, DB.prepareString(sb.getSvalue2(), 255));
					ps.setString(psIndex++, DB.prepareString(sb.getSvalue3(), 255));
					ps.setString(psIndex++, DB.prepareString(sb.getSvalue4(), 255));
					ps.setInt(psIndex++, sb.getSint1());
					ps.setInt(psIndex++, sb.getSint2());
					ps.setInt(psIndex++, sb.getSint3());
					ps.setInt(psIndex++, sb.getSint4());
					ps.setTimestamp(psIndex++, sb.getSdate());
					ps.execute();
				}
				catch (Exception ex)
				{
					sk.iway.iwcm.Logger.error(ex);
				}
			}
			ps.close();
			ps.close();
			db_conn.close();
			ps = null;
			db_conn = null;

			saveOK = true;
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

		return saveOK;
	}

	/**
	 * Ak mame indexovane nastavenia, napr. mojeOdkazy, tak prida novu hodnotu na zaciatok zoznamu a vsetky ostatne posunie o 1
	 * @param settings
	 * @param prefix
	 * @param sb
	 * @return
	 */
	public static boolean settingsInsertFirst(Map<String, SettingsBean> settings, String prefix, SettingsBean sb)
	{
		//najskor si ziskaj podla nastaveni
		List<SettingsBean> hodnoty = filterSettingsByPrefix(prefix, settings);
		//bhric: najprv vymazem povodne hodnoty zo settings, inak nastavali duplicity
		for(SettingsBean s: hodnoty)
			settings.remove(s.getSkey());
		hodnoty.add(0, sb);
		for (int i=0; i<hodnoty.size(); i++)
		{
			sb.setSkey(prefix+"@"+i);
			settings.put(sb.getSkey(), hodnoty.get(i));
		}

		return true;
	}

	public static void deleteApproveGroup(int userId,int groupId)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("DELETE FROM groups_approve WHERE user_id = ? AND group_id = ?");
			ps.setInt(1, userId);
			ps.setInt(2, groupId);
			ps.executeUpdate();
			ps.close();
			db_conn.close();
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

	public static void addApproveGroup(int userId, int groupId, int mode)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			db_conn = DBPool.getConnection();

			//ochrana pred duplicitou
			ps = db_conn.prepareStatement("DELETE FROM groups_approve WHERE group_id=? AND user_id=? AND approve_mode=?");
			int parameterIndex = 1;
			ps.setInt(parameterIndex++, groupId);
			ps.setInt(parameterIndex++, userId);
			ps.setInt(parameterIndex++, mode);
			ps.execute();
			ps.close();

			ps = db_conn.prepareStatement("INSERT INTO groups_approve(group_id,user_id,approve_mode) VALUES" +
					"(?,?,?) ");
			parameterIndex = 1;
			ps.setInt(parameterIndex++, groupId);
			ps.setInt(parameterIndex++, userId);
			ps.setInt(parameterIndex++, mode);
			ps.execute();
			ps.close();
			db_conn.close();
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

	public static int getApproveMode(int userId, int groupId)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT approve_mode FROM groups_approve WHERE group_id=? AND user_id=?");
			int parameterIndex = 1;
			ps.setInt(parameterIndex++, groupId);
			ps.setInt(parameterIndex++, userId);
			rs = ps.executeQuery();
			if (rs.next())
				return rs.getInt(1);
			ps.close();
			db_conn.close();
			rs.close();
			ps = null;
			db_conn = null;
			return -1;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
			return -1;
		}
		finally
		{
			try
			{
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
				if (rs != null)
					rs.close();
			}
			catch (Exception ex2)
			{
			}
		}
	}


	/**
	 * Nastavi pouzivatelovi zakazane polozky/moduly
	 * @param user
	 */
	public static void setDisabledItems(Identity user)
	{
		if (InitServlet.isTypeCloud())
		{
			RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
			CloudToolsForCore.setPermissions(user, rb);
			return;
		}

		loadDisabledItemsFromDB(user);
	}

	/**
	 * Prakticky private metoda pre nahratie dat z DB, je to takto spravene kvoli CloudTools a multiwebu a donahratiu prav
	 * @param user
	 */
	public static void loadDisabledItemsFromDB(Identity user)
	{
		loadDisabledItemsFromDB(user, true);
	}

	/**
	 * Read disabled items for user
	 * @param user
	 * @param alsoGroups - true to load perms from groups
	 */
	public static void loadDisabledItemsFromDB(Identity user, boolean alsoGroups)
	{
		//najskor nacitame zoznam prav podla skupin (tie nasledne implicitne povolime)
		Set<String> enabledItemsFromGroups = new HashSet<>();
		if (alsoGroups) {
			try
			{
				List<PermissionGroupBean> permissionGroups = UserGroupsDB.getPermissionGroupsFor(user.getUserId());
				for (PermissionGroupBean permGroup : permissionGroups)
				{
					for (String permission : permGroup.getPermissionNames())
					{
						enabledItemsFromGroups.add(permission);
					}
				}
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}

		//nacitame prava pre usera
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();

			//user.setDisabledItems(DB.getDbString(db_result, "disabled_items"));
			ps = db_conn.prepareStatement("SELECT * FROM user_disabled_items WHERE user_id=?");
			ps.setInt(1, user.getUserId());
			rs = ps.executeQuery();
			while (rs.next())
			{
				String perm = DB.getDbString(rs, "item_name");
				if (enabledItemsFromGroups.contains(perm))
				{
					Logger.debug(UsersDB.class, "Enabling perm "+perm+" by group for user "+user.getLogin());
					continue;
				}

				user.addDisabledItem(perm);
			}
			rs.close();
			ps.close();

			//skontroluj moduly, ak sa nenachadzaju na disku, zadisabluj
			Modules.getInstance().disableModules(user);

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
			}
		}

	}


	@SuppressWarnings("unchecked")
	public static List<Number> getApproveGroups(int userId)
	{
		return DB.queryForList("SELECT group_id FROM groups_approve WHERE user_id=?", userId);
	}

	public static void clearApproveGroups(int userId)
	{
		for (Number groupId : getApproveGroups(userId))
			deleteApproveGroup(userId, groupId.intValue());
	}

	public static void disableItem(int userId,String whichItemToDisable)
	{
		DB.execute("INSERT INTO user_disabled_items(user_id,item_name) VALUES (?,?)", userId, whichItemToDisable);
	}

	public static void enableItem(int userId,String whichItemToEnable)
	{
		DB.execute("DELETE FROM user_disabled_items WHERE user_id = ? AND item_name = ?", userId, whichItemToEnable);
	}

	public static void addUserToPermissionGroup(int userId, int permId)
	{
		DB.execute("INSERT INTO users_in_perm_groups(user_id,perm_group_id) VALUES (?,?)", userId, permId);
	}

	public static void deleteUserFromPermissionGroup(int userId, int permId)
	{
		DB.execute("DELETE FROM users_in_perm_groups WHERE user_id = ? AND perm_group_id = ?", userId, permId);
	}

	public static boolean isFolderWritable(String writableFoldersParam,String folderParam)
	{
		String folder = folderParam;

		//jedna sa o IwcmDocGroupFsVolume kde su takto prenasane ID adresara/stranky, length<20 kvoli bezpecnosti
		if (folder != null && folder.length()<20 && (folder.startsWith("/group:") || folder.startsWith("/doc:"))) return true;

		if (FileBrowserTools.hasForbiddenSymbol(folder)) return false;

		String writableFolders = writableFoldersParam;
        if (Tools.isEmpty(writableFolders) && Constants.getBoolean("fbrowserShowOnlyWritableFolders"))
        {
            writableFolders = Constants.getStringExecuteMacro("fbrowserDefaultWritableFolders");
        }

		if (folder == null || Tools.isEmpty(writableFolders))
		{
			return(!Constants.getBoolean("defaultDisableUpload"));
		}
		if ("*".equals(writableFolders)) return true;

		if (folder.endsWith("/")==false) folder = folder+"/";

		folder = Tools.replace(folder, "//", "/");

		StringTokenizer st = new StringTokenizer(writableFolders, "\n");
		String dir;
		while (st.hasMoreTokens())
		{
			dir = st.nextToken().trim();
			if (dir.length()>1 && (dir.endsWith("+") || dir.endsWith("*")))
			{
				if (dir.endsWith("/+") || dir.endsWith("/*")) dir = dir.substring(0, dir.length()-2);
				else dir = dir.substring(0, dir.length()-1);

				if (folder.startsWith(dir))
				{
					return(true);
				}
			}
			else
			{
				if (dir.equals(folder))
				{
					return(true);
				}
			}
		}
		return(false);
	}

	/**
	 * Vrati zoznam userov ktori maju ako parent_id nastavene userId
	 * z parametra
	 * @param userId
	 * @return zoznam userov ktorum je userId parent, ak taky nie je tak vrati prazdny list
	 */
	public static List<UserDetails> getUsersByParentId(int userId)
	{
		List<UserDetails> users = new LinkedList<>();
		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{

			db_conn = DBPool.getConnectionReadUncommited();

			StringBuilder sql = new StringBuilder("SELECT * FROM users");
			sql.append(" WHERE parent_id = ? AND authorized = ? "+UsersDB.getDomainIdSqlWhere(true)+" order by last_name ASC, first_name ASC");
			Logger.debug(UsersDB.class, "sql="+sql);

			ps = db_conn.prepareStatement(sql.toString());
			int parameterIndex = 1;
			ps.setInt(parameterIndex++, userId);
			ps.setBoolean(parameterIndex++, true);

			rs = ps.executeQuery();
			UserDetails usr;
			while (rs.next())
			{
				usr = new UserDetails(rs);
				users.add(usr);
			}
			rs.close();
			ps.close();
			db_conn.close();
			db_conn = null;
			rs = null;
			ps = null;
		}
		catch (Exception ex){sk.iway.iwcm.Logger.error(ex);}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
		return (users);
	}

	/**
	 * Prefiltruje zoznam pouzivatelov cez meno skupiny
	 * @param users
	 * @param groupName
	 * @return zoznam pouzivatelv ktori patria do danej skupiny
	 */
	public static List<UserDetails> filterUserByGroupName(List<UserDetails> users, String groupName){
		List<UserDetails> filtered = new ArrayList<>(users.size());
		for(UserDetails usr : users){
			if(usr.isInUserGroup(groupName))
				filtered.add(usr);
		}
		return filtered;
	}

	/**
	 * Vrati zoznam userov ktori maju ako parent_id nastavene 0
	 * @return zoznam userov ktorum je 0 parent a maju prava na intranet
	 *  ak taky nie je tak vrati prazdny list
	 */
	public static List<UserDetails> getUsersWithoutParent()
	{
		return getUsersByParentId(0);
	}

	/**
	 * Kontroluje prava pouzivatela na admin cast a moduly, medzi perms je pouzity OR mod, zapisuje sa ako
	 * admin|editableGroupsNotEmpty
	 * Pozna aj specialne prava admin (kontrola na isAdmin) a editableGroupsNotEmpty (editableGroups nie je prazdne)
	 * @param user
	 * @param perms
	 * @return
	 */
	public static boolean checkUserPerms(Identity user, String perms)
	{
		if (user==null || user.getUserId()<1 || Tools.isEmpty(perms)) return false;

		String[] permsArray = Tools.getTokens(perms, "|", true);
		for (String perm : permsArray)
		{
			if ("admin".equalsIgnoreCase(perm) && user.isAdmin()) return true;
			if ("editableGroupsNotEmpty".equalsIgnoreCase(perm) &&  Tools.isNotEmpty(user.getEditableGroups())) return true;

			//standardne moduly
			if (user.isEnabledItem(perm)) return true;
		}

		return false;
	}

	/**
	 * Ziska zoznam pouzivatelov, ktori splnaju zadane podmienky
	 * @return
	 */
	public static List<UserDetails> getUsersByWhereSql(String whereSql)
	{
		List<UserDetails> users = new ArrayList<>();
		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{

			db_conn = DBPool.getConnectionReadUncommited();

			StringBuilder sql = new StringBuilder("SELECT * FROM users");
			sql.append(" WHERE ");
			sql.append(UsersDB.getDomainIdSqlWhere(false));

			sql.append(" ").append(whereSql);


			sql.append(" ORDER BY last_name, first_name");

			Logger.debug(UsersDB.class, "sql="+sql);

			ps = db_conn.prepareStatement(sql.toString());
			rs = ps.executeQuery();
			UserDetails usr;
			while (rs.next())
			{

					usr = new UserDetails(rs);
					users.add(usr);

			}
			rs.close();
			ps.close();
			db_conn.close();
			db_conn = null;
			rs = null;
			ps = null;
		}
		catch (Exception ex){sk.iway.iwcm.Logger.error(ex);}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
		return (users);
	}

	/**
	 * Ulozi schvalovanie pre webove adresare.
	 * @param userId
	 * @param approveGroups
	 * @return true ak sa vsetko podari ulozit
	 */
	public static boolean saveApproveGroups(int userId, Map<String, LabelValueDetails> approveGroups) {
		Connection con = DBPool.getConnection();
		PreparedStatement ps = null;
		try {
			ps = con.prepareStatement("DELETE FROM groups_approve WHERE user_id=?");
			ps.setInt(1, userId);
			ps.execute();
			ps.close();

			for (Map.Entry<String, LabelValueDetails> entry : approveGroups.entrySet()) {
				//String str = entry.getKey();
				LabelValueDetails lvd = entry.getValue();
				ps = con.prepareStatement("INSERT INTO groups_approve (group_id, user_id, approve_mode) VALUES (?, ?, ?)");
				ps.setInt(1, Tools.getIntValue(lvd.getValue(),-1));
				ps.setInt(2, userId);
				ps.setInt(3, Tools.getIntValue(lvd.getValue2(),-1));
				ps.execute();
				ps.close();
			}

			return true;
		} catch (Exception ex) {
			sk.iway.iwcm.Logger.error(ex);
		} finally {
			try {
				if (ps!=null) ps.close();
			} catch (Exception e) {
				sk.iway.iwcm.Logger.error(e);
			}
			try {
				con.close();
			} catch (Exception e) {
				sk.iway.iwcm.Logger.error(e);
			}
		}
		return false;
	}

    /**
     * Ulozi zakazane pravomoci do DB
     * @param userId
     * @param disabledItems retazec pravomoci oddeleny čiarkami (bez medzier)
     * @return true ak sa vsetko podari ulozit
     */
	public static boolean saveDisabledItems(int userId, String disabledItems) {
		Connection con = DBPool.getConnection();
		Logger.println(null,"setDisabledItems userid=" + userId);
		PreparedStatement ps = null;

		if (userId<1)
			return false;

		try {
			ps = con.prepareStatement("DELETE FROM user_disabled_items WHERE user_id=?");
			ps.setInt(1, userId);
			ps.execute();
			ps.close();
			for (String disabledItem : disabledItems.split(",")) {
				Logger.println(null, "setting: " + disabledItem);
				ps = con.prepareStatement("INSERT INTO user_disabled_items (user_id, item_name) VALUES (?, ?)");
				ps.setInt(1, userId);
				ps.setString(2, disabledItem);
				ps.execute();
				ps.close();
			}
		} catch (Exception ex) {
			sk.iway.iwcm.Logger.error(ex);
			return false;
		} finally {
			try {
				if (ps!=null) ps.close();
			} catch (Exception e) {
				sk.iway.iwcm.Logger.error(e);
			}
		}
		if (disabledItems!=null)
			Adminlog.add(Adminlog.TYPE_USER_UPDATE, "Current disabled items: "+disabledItems, userId, -1);
		return true;
	}

	private static Map<Integer, UserDetails> getUserCache()
	{
		Cache cache = Cache.getInstance();
		@SuppressWarnings("unchecked")
		Map<Integer, UserDetails> cachedUsers = (Map<Integer, UserDetails>)cache.getObject(CACHE_KEY);
		if (cachedUsers == null)
		{
			cachedUsers = new Hashtable<>();
			cache.setObject(CACHE_KEY, cachedUsers, 10*60);
		}

		return cachedUsers;
	}

	public static UserDetails getUserCached(int userId)
	{
		Map<Integer, UserDetails> cachedUsers = UsersDB.getUserCache();

		UserDetails user = cachedUsers.get(userId);
		if (user == null)
		{
			user = UsersDB.getUser(userId);
			if (user != null) cachedUsers.put(userId, user);
		}

		return user;
	}

	public static void removeUserFromCache(int userId)
	{
		Map<Integer, UserDetails> cachedUsers = UsersDB.getUserCache();
		cachedUsers.remove(userId);
	}

	/**
	 * Get password_salt for user
	 * @param userId
	 * @return
	 */
	public static String getSalt(int userId) {
		String salt = (new SimpleQuery()).forString("SELECT password_salt FROM users WHERE user_id=?", userId);
		return salt;
	}

}