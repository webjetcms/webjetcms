package sk.iway.iwcm.components.adresar;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageParams;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.enumerations.EnumerationTypeDB;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

/**
 *  AdresarDB.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: bhric mhruby $
 *@version      $Revision: 1.1 $
 *@created      Date: 1.10.2009 16:28:40
 *@modified     $Date: 2009/10/08 08:06:38 $
 */
public class AdresarDB
{
	private AdresarDB() {

	}

	/**
	 * @param filterParam nepouziva sa vyhladava sa nad stlpcami ktore su definovane v searchcolums
	 * @param searchString vyraz pre hladanie
	 * @param request
	 * @return zoznam registrovanych pouzivatelov vyhovujucim hladaniu
	 */
	public static List<UserDetails> listUsers(@Deprecated String filterParam, String searchString, HttpServletRequest request) {

		int enumerationTypeId = EnumerationTypeDB.getEnumerationIdFromString(Constants.getString("usersFaxList"));
		int enumerationTypeId2 = EnumerationTypeDB.getEnumerationIdFromString(Constants.getString("usersPositionList"));
	    List<Integer> integers = null;
	    if (!Tools.isEmpty(searchString))
	    	integers = new SimpleQuery().forListInteger("SELECT enumeration_data_id FROM enumeration_data WHERE hidden != true AND enumeration_type_id IN (" + enumerationTypeId + ","+enumerationTypeId2 +") AND string1 LIKE '%"+ DB.removeSlashes(searchString) + "%'");
		return listUsers(integers,searchString,request);
	}

	/**
	 * @param oddelenieIds ids enumerationDataCiselnika
	 * @param searchString vyraz pre hladanie
	 * @param request
	 * @return zoznam registrovanych pouzivatelov vyhovujucim hladaniu
	 */
	@SuppressWarnings("unused")
	public static List<UserDetails> listUsers(List<Integer> oddelenieIds, String searchString, HttpServletRequest request) {
		List<String> searchColumns = new ArrayList<>();
		searchColumns.add("last_name");
		searchColumns.add("first_name");
		searchColumns.add("email");
		searchColumns.add("phone");
		searchColumns.add("company");

		List<UserDetails> users = new ArrayList<>();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			PageParams pageParams = new PageParams(request);
			StringBuilder userGroupsSQL = new StringBuilder();
			String groupIds = pageParams.getValue("groupIds","");
			if(Tools.isNotEmpty(groupIds) && groupIds.indexOf(',') != -1)
				groupIds = Tools.replace(groupIds, ",", "+");
			String[] groupIdsList = Tools.getTokens(groupIds, "+");
			for(String groupId : groupIdsList)
			{
				int groupIdInt = Tools.getIntValue(groupId, -1);
				if(groupIdInt != -1)
				{
					if(Tools.isEmpty(userGroupsSQL))
						userGroupsSQL.append( "(");
					else
						userGroupsSQL.append( " OR ");
					userGroupsSQL.append("(user_groups='").append(groupId).append("' OR user_groups LIKE '%,").append(groupId).append("' OR user_groups LIKE '").append(groupId).append(",%' OR user_groups LIKE '%,").append(groupId).append(",%')");
				}
			}
			if(Tools.isNotEmpty(userGroupsSQL))
				userGroupsSQL.append( ')');
			StringBuilder sql = new StringBuilder("SELECT * FROM users WHERE authorized = 1 AND ").append(userGroupsSQL);

			if (Tools.isNotEmpty(searchString) ) {
				sql.append(" AND (");
				Iterator<String> iterator = searchColumns.iterator();
				while (iterator.hasNext()) {
					String filter = iterator.next();
					if (Constants.DB_TYPE == Constants.DB_MSSQL)
						filter += " COLLATE Latin1_general_CI_AI";
					sql.append(filter).append(" LIKE ?");
					if (iterator.hasNext())
						sql.append(" OR ");
					}
				sql.append(")");
				}

			boolean useless = true;
            if (oddelenieIds != null && !oddelenieIds.isEmpty()) {
                sql.append(useless ? " OR " : " AND ");
                sql.append(" fax IN (" + StringUtils.join(oddelenieIds, ",") + ") ");
                if (useless)
                	sql.append(" OR position IN (" + StringUtils.join(oddelenieIds,",")+") ");
            }

			//sortovanie
			StringBuilder orderSql = new StringBuilder();
			String orderBy = pageParams.getValue("orderBy","");
			if(Tools.isNotEmpty(orderBy)) {
				String[] orderByList = Tools.getTokens(orderBy, "+");
				for(String orderByTmp : orderByList) {
					String orderType = orderByTmp;
					String orderVar = "ASC";
					int ind = orderByTmp.lastIndexOf('-');
					if(ind != -1) {
						orderType = orderByTmp.substring(0, ind).trim();
						String orderVarTmp = orderByTmp.substring(ind+1, orderByTmp.length()).trim();
						if("desc".equalsIgnoreCase(orderVarTmp))
							orderVar = "DESC";
					}
					if(Tools.isEmpty(orderSql.toString())) {
						orderSql.append(" ORDER BY ").append(orderType).append(' ').append(orderVar);
					} else {
						orderSql.append(", ").append(orderType).append(' ').append(orderVar);
					}
				}
			} else {
				orderSql = new StringBuilder(" ORDER BY last_name,first_name");
			}
			sql.append(orderSql);

			Logger.println(AdresarDB.class, "ADRESAR SQL = "+sql);
			ps = db_conn.prepareStatement(sql.toString());
			int ind = 1;
			if (Tools.isNotEmpty(searchString)) {
				for (String str : searchColumns) {
					ps.setString(ind++, "%" + searchString + "%");
				}
			}

			rs = ps.executeQuery();
			while (rs.next()) {
				UserDetails usr = new UserDetails();
				UsersDB.fillUserDetails(usr, rs);

				if (Tools.isNotEmpty(usr.getFirstName()) && Tools.isNotEmpty(usr.getLastName()))
				{
					users.add(usr);
				}
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		} catch (Exception ex) {
			users = null;
			sk.iway.iwcm.Logger.error(ex);
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			} catch (Exception ex2) {
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
		return users;
	}
}
