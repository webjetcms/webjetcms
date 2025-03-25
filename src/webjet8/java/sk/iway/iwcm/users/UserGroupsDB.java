package sk.iway.iwcm.users;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.helpers.BeanDiff;
import sk.iway.iwcm.helpers.BeanDiffPrinter;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.system.multidomain.MultiDomainFilter;


/**
 *  Drzi zaznamy z tabulky user_groups, co je zoznam skupin pouzivatelov
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.6 $
 *@created      ďż˝tvrtok, 2002, august 15
 *@modified     $Date: 2004/02/12 16:57:09 $
 */
public class UserGroupsDB extends DB
{
	private String serverName = "";
	private List<UserGroupDetails> userGroups;

	public static UserGroupsDB getInstance()
	{
		return(getInstance(false));
	}

	public static UserGroupsDB getInstance(boolean forceRefresh)
	{
		return(getInstance(Constants.getServletContext(), forceRefresh, "iwcm"));
	}

	/**
	 *  Gets the instance attribute of the UserGroupsDB class
	 *
	 *@param  servletContext  Description of the Parameter
	 *@param  force_refresh   Description of the Parameter
	 *@param  serverName      Description of the Parameter
	 *@return                 The instance value
	 */
	public static UserGroupsDB getInstance(javax.servlet.ServletContext servletContext, boolean force_refresh, String serverName)
	{
		//try to get it from server space
		if (force_refresh == false)
		{
			if (servletContext.getAttribute(Constants.A_USER_GROUPS_DB + serverName) != null)
			{
				//Logger.println(this,"DocDB: getting from server space");
				return ((UserGroupsDB) servletContext.getAttribute(Constants.A_USER_GROUPS_DB + serverName));
			}
		}
		return (new UserGroupsDB(servletContext, serverName));
	}

	/**
	 *  Constructor for the UserGroupsDB object
	 *
	 *@param  servletContext  Description of the Parameter
	 *@param  serverName      Description of the Parameter
	 */
	private UserGroupsDB(javax.servlet.ServletContext servletContext, String serverName)
	{
		this.serverName = serverName;

		//save us to server space
		reload();

		servletContext.setAttribute(Constants.A_USER_GROUPS_DB + serverName, this);

		ClusterDB.addRefresh(UserGroupsDB.class);
	}

	/**
	 *  Description of the Method
	 */
	private void reload()
	{
		userGroups = new ArrayList<>();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{

			db_conn = DBPool.getConnection(serverName);
			ps = db_conn.prepareStatement("SELECT * FROM  user_groups ORDER BY user_group_name");
			rs = ps.executeQuery();
			UserGroupDetails usrGroupDetails;
			while (rs.next())
			{
				usrGroupDetails = new UserGroupDetails();
				usrGroupDetails.setUserGroupId(rs.getInt("user_group_id"));
				usrGroupDetails.setUserGroupName(getDbString(rs, "user_group_name"));
				usrGroupDetails.setUserGroupType(rs.getInt("user_group_type"));
				usrGroupDetails.setUserGroupComment(getDbString(rs, "user_group_comment"));
				usrGroupDetails.setRequireApprove(rs.getBoolean("require_approve"));
				usrGroupDetails.setEmailDocId(rs.getInt("email_doc_id"));
				usrGroupDetails.setAllowUserEdit(rs.getBoolean("allow_user_edit"));
				usrGroupDetails.setRequireEmailVerification(rs.getBoolean("require_email_verification"));
				usrGroupDetails.setPriceDiscount(rs.getInt("price_discount"));


				userGroups.add(usrGroupDetails);
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
			}
		}
	}

	/**
	 *  Gets the userGroups attribute of the UserGroupsDB object
	 *
	 *@return    The userGroups value
	 */
	public List<UserGroupDetails> getUserGroups()
	{
		return (filterMultidomainUserGroups(userGroups));
	}

	public static List<UserGroupDetails> filterMultidomainUserGroups(List<UserGroupDetails> userGroups)
	{
		String domainAlias = MultiDomainFilter.getDomainAlias(CloudToolsForCore.getDomainName());
		if (Tools.isEmpty(domainAlias)) return userGroups;

		List<UserGroupDetails> filtered = new ArrayList<>();
		for (UserGroupDetails ug : userGroups)
		{
			if (ug.getUserGroupName().indexOf("(")==-1 || ug.getUserGroupName().indexOf("("+domainAlias+")")!=-1)
			{
				filtered.add(ug);
			}
		}
		return filtered;
	}

	/**
	 *  Gets the userGroup attribute of the UserGroupsDB object
	 *
	 *@param  id  Description of the Parameter
	 *@return     The userGroup value
	 */
	public UserGroupDetails getUserGroup(int id)
	{
		for (UserGroupDetails ugd : userGroups)
		{
			if (ugd.getUserGroupId() == id)
			{
				return (ugd);
			}
		}
		return (null);
	}

	/**
	 * Vrati zoznam skupin pre dany typ skupiny
	 * @param userGroupTypeId
	 * @return
	 */
	public List<UserGroupDetails> getUserGroupsByTypeId(int userGroupTypeId)
	{
		List<UserGroupDetails> ret = new ArrayList<>();
		for (UserGroupDetails ugd : userGroups)
		{
			if (ugd.getUserGroupType() == userGroupTypeId)
			{
				ret.add(ugd);
			}
		}
		return (ret);
	}


	/**
	 *  Najde skupinu podla mena, ignoruje velkost pismen
	 *
	 *@param  groupName  Description of the Parameter
	 *@return            The userGroup value
	 */
	public UserGroupDetails getUserGroup(String groupName)
	{
		for (UserGroupDetails ugd : userGroups)
		{
			if (ugd.getUserGroupName().equalsIgnoreCase(groupName))
			{
				return (ugd);
			}
		}
		return (null);
	}

	/**
	 *  Najde skupiny podla mena, ignoruje velkost pismen aj diakritiku, konstrukcia LIKE
	 *
	 *@param  groupName  cast nazvu skupiny
	 *@return
	 */
	public List<UserGroupDetails> getUserGroups(String groupName)
	{
		List<UserGroupDetails> filterGroups = new ArrayList<>();

		for (UserGroupDetails ugd : userGroups)
		{
			if (DB.internationalToEnglish(ugd.getUserGroupName().toLowerCase()).indexOf(DB.internationalToEnglish(groupName.toLowerCase())) != -1)
				filterGroups.add(ugd);
		}
		return (filterGroups);
	}

	/**
	 * Vrati meno skupiny na zaklade ID alebo null ak neexistuje
	 * @param userGroupId
	 * @return
	 */
	public String getUserGroupName(int userGroupId)
	{
		for (UserGroupDetails ugd : userGroups)
		{
			if (ugd.getUserGroupId() == userGroupId)
			{
				return (ugd.getUserGroupName());
			}
		}
		return (null);
	}

	/**
	 * Nrati ID skupiny na zaklade mena
	 * @param groupName
	 * @return
	 */
	public int getUserGroupId(String groupName)
	{
		if (groupName.startsWith("gid:"))
		{
			try
			{
				//skus ziskat id
				int gid = Tools.getIntValue(groupName.substring(4, groupName.indexOf('-')).trim(), -1);
				if (gid > 0) return(gid);
			}
			catch (Exception e)
			{
			}
		}

		//TODO: upravit na nieco rychlejsie (Hashtabulku)
		for (UserGroupDetails ugd : userGroups)
		{
			if (ugd.getUserGroupName().equalsIgnoreCase(groupName))
			{
				return (ugd.getUserGroupId());
			}
		}
		return (-1);
	}

	/**
	 *  Najde skupinu podla mena, ignoruje velkost pismen, nazov skupiny prevadza na nazov adresara (odstranuje znaky)
	 * @param dirName
	 * @return
	 */
	public UserGroupDetails getUserGroupDirName(String dirName)
	{
		String groupName;
		for (UserGroupDetails ugd : userGroups)
		{
			groupName = ugd.getUserGroupName();
			groupName = DocTools.removeCharsDir(groupName, false).toLowerCase();
			groupName = DB.internationalToEnglish(groupName);
			if (groupName.equalsIgnoreCase(dirName))
			{
				return (ugd);
			}
		}
		return (null);
	}

	/**
	 *  Description of the Method
	 *
	 *@param  ids  Description of the Parameter
	 *@return      Description of the Return Value
	 */
	public String convertIdsToNames(String ids)
	{
		if (ids == null || ids.length() < 1)
		{
			return ("");
		}
		StringTokenizer st = new StringTokenizer(ids, ",");
		StringBuilder ret = null;
		int id;
		UserGroupDetails ugd;
		try
		{
			while (st.hasMoreTokens())
			{
				id = Tools.getIntValue(st.nextToken(), -1);
				if (id == -1) continue;
				ugd = getUserGroup(id);
				if (ugd != null)
				{
					if (ret == null)
					{
						ret = new StringBuilder(ugd.getUserGroupName());
					}
					else
					{
						ret.append(",").append(ugd.getUserGroupName());
					}
				}
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		if (ret == null)
		{
			return "???";
		}
		return (ret.toString());
	}

	/**
	 * Ulozi zadanu skupinu do DB - pozor po ulozeni treba zavolat refresh DB
	 * @param ugd
	 * @return
	 */
	public static boolean saveUserGroup(UserGroupDetails ugd)
	{
		boolean saveOK = false;
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();


			String sql = "INSERT INTO  user_groups (user_group_name, user_group_type, user_group_comment, require_approve, email_doc_id, allow_user_edit, price_discount) VALUES (?, ?, ?, ?, ?, ?, ?)";

			if (ugd.getUserGroupId()>0)
			{
				UserGroupDetails old = getInstance().getUserGroup(ugd.getUserGroupId());
				BeanDiff diff = new BeanDiff().setNew(ugd).setOriginal(old);

				sql = "UPDATE  user_groups SET user_group_name=?, user_group_type=?, user_group_comment=?, require_approve=?, email_doc_id=?, allow_user_edit=?, price_discount=? WHERE user_group_id=?";
				Adminlog.add(Adminlog.TYPE_USER_GROUP_UPDATE, "Update user groups name= : "+  ugd.getUserGroupName() + new BeanDiffPrinter(diff), ugd.getUserGroupId(), -1);
			}
			else
			{
				Adminlog.add(Adminlog.TYPE_USER_GROUP_INSERT, "Insert user groups name= : "+ ugd.getUserGroupName()+ " type= " + ugd.getUserGroupType(), -1, -1);
			}
			ps = db_conn.prepareStatement(sql);
			ps.setString(1, ugd.getUserGroupName());
			ps.setInt(2, ugd.getUserGroupType());
			ps.setString(3, ugd.getUserGroupComment());
			ps.setBoolean(4, ugd.isRequireApprove());
			ps.setInt(5, ugd.getEmailDocId());
			ps.setBoolean(6, ugd.isAllowUserEdit());
			ps.setInt(7, ugd.getPriceDiscount());
			if (ugd.getUserGroupId()>0)
			{
				ps.setInt(8, ugd.getUserGroupId());
			}
			ps.execute();
			ps.close();

			//nastav ID skupiny
			if (ugd.getUserGroupId()<1)
			{
				ps = db_conn.prepareStatement("SELECT max(user_group_id) AS user_group_id FROM  user_groups WHERE user_group_name=?");
				ps.setString(1, ugd.getUserGroupName());
				rs = ps.executeQuery();
				if (rs.next())
				{
					ugd.setUserGroupId(rs.getInt("user_group_id"));
				}
				rs.close();
				ps.close();

				rs = null;
			}

			db_conn.close();
			ps = null;
			db_conn = null;

			saveOK = true;

			UserGroupsDB.getInstance(true);
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
		return(saveOK);
	}

	public static List<PermissionGroupBean> getPermissionGroupsFor(int userId)
	{
		List<PermissionGroupBean> permGroups = new ArrayList<>();
		List<Number> ids = new SimpleQuery().forListNumber("SELECT perm_group_id FROM users_in_perm_groups WHERE user_id = ?", userId);

		for (Number id : ids)
		{
			PermissionGroupBean permGroup = (new PermissionGroupDB()).getById(id.intValue());
			if (permGroup != null)
				permGroups.add(permGroup);
		}

		return permGroups;
	}

    /**
     * Vrati list ID skupin z pouzivatela odfiltrovanych podla typu skupiny (userGroupTypeId)
     * @param ids - ciarkou oddeleny zoznam ID skupin (ziskanych z user.getUserGroupIds())
     * @param userGroupTypeId - filter skupin, alebo < 1 pre vsetky
     * @return
     */
    public List<Integer> getUserGroupIdsList(String ids, int userGroupTypeId)
    {
        List<Integer> idsList = new ArrayList<>();

        if (ids == null || ids.length() < 1)
        {
            return idsList;
        }
        StringTokenizer st = new StringTokenizer(ids, ",");
        int id;
        UserGroupDetails ugd;
        try
        {
            while (st.hasMoreTokens())
            {
                id = Tools.getIntValue(st.nextToken(), -1);
                if (id == -1) continue;
                ugd = getUserGroup(id);
                if (ugd != null && (userGroupTypeId<1 || ugd.getUserGroupType()==userGroupTypeId))
                {
                    idsList.add(ugd.getUserGroupId());
                }
            }
        }
        catch (Exception ex)
        {
            sk.iway.iwcm.Logger.error(ex);
        }
        return idsList;
    }

	/**
	 * Odstrani zadanu skupinu.
	 * @param userGroupId
	 * @return
	 */
	public boolean remove(int userGroupId) {
		Connection db_conn = null;
		PreparedStatement ps = null;
		try {
			db_conn = DBPool.getConnection();

			ps = db_conn.prepareStatement("DELETE FROM  user_groups WHERE user_group_id=?");
			ps.setInt(1, userGroupId);

			ps.execute();
			ps.close();

			return true;
		} catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			} catch (Exception ex) {
				sk.iway.iwcm.Logger.error(ex);
			}
		}
		return false;
	}

	public BigDecimal calculatePrice(BigDecimal price, UserDetails user) {
		BigDecimal newPrice = price;
		int maxPriceDiscount = 0;
		if(user != null) {
			for (int groupId : Tools.getTokensInt(user.getUserGroupsIds(), ",")) {
				UserGroupDetails group = getUserGroup(groupId);
				if (group != null && group.getPriceDiscount() > maxPriceDiscount) {
					maxPriceDiscount = group.getPriceDiscount();
				}
			}

			newPrice = getPriceByDiscount(price, maxPriceDiscount);
		}

		Logger.debug(UserGroupsDB.class, "calculatePrice: price=" + price + ", newPrice=" + newPrice + ", maxPriceDiscount=" + maxPriceDiscount + ", user=" + user);

		return newPrice;
	}

	public Map<String, BigDecimal> calculatePrices(Map<String, BigDecimal> prices, UserDetails user) {
		int maxPriceDiscount = 0;
		if(user != null) {
			for (int groupId : Tools.getTokensInt(user.getUserGroupsIds(), ",")) {
				UserGroupDetails group = getUserGroup(groupId);
				if (group != null && group.getPriceDiscount() > maxPriceDiscount) {
					maxPriceDiscount = group.getPriceDiscount();
				}
			}
		}
		Map<String, BigDecimal> discountedPrices = new java.util.HashMap<>();
		for (Map.Entry<String, BigDecimal> entry : prices.entrySet()) {
			discountedPrices.put(entry.getKey(), getPriceByDiscount(entry.getValue(), maxPriceDiscount));
		}
		return discountedPrices;
	}

	private BigDecimal getPriceByDiscount(BigDecimal price, int discount) {
		if(discount == 0) return price;
		if(discount == 100) return BigDecimal.ZERO;
		return price.multiply(new BigDecimal(100 - discount)).divide(new BigDecimal(100));
	}
}
