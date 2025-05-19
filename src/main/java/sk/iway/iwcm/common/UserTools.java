package sk.iway.iwcm.common;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.StringTokenizer;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.LabelValueDetails;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.enumerations.EnumerationDataDB;
import sk.iway.iwcm.components.enumerations.EnumerationTypeDB;
import sk.iway.iwcm.components.enumerations.model.EnumerationDataBean;
import sk.iway.iwcm.components.enumerations.model.EnumerationTypeBean;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.users.PermissionGroupBean;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UserGroupsDB;
import sk.iway.iwcm.users.UsersDB;

public class UserTools {

    public static final String PASS_UNCHANGED = "Unch4ng3d.Pwd";
    private static final String DEPARTMENT_CONF_NAME = "usersFaxList";
    public static final String USE_EMAIL_AS_LOGIN = "useEmailAsLogin";

	private UserTools() {

	}

    public static String getDepartment(String department, String parentDepartment, int row) {
        EnumerationDataBean departmentEnumeration = null;
        EnumerationDataBean parentDepartmentEnumeration = null;
        if (Tools.isNotEmpty(department)) {
            departmentEnumeration = resolveOrCreate(DEPARTMENT_CONF_NAME, department);
        }

        if (Tools.isNotEmpty(parentDepartment)) {
            parentDepartmentEnumeration = resolveOrCreate(DEPARTMENT_CONF_NAME, parentDepartment);
        }
        if (parentDepartmentEnumeration != null && departmentEnumeration != null) {
            departmentEnumeration.setParentEnumerationData(parentDepartmentEnumeration);
            departmentEnumeration.save();
        }

        if (departmentEnumeration != null)
            return departmentEnumeration.getId()+"";
        if (parentDepartmentEnumeration != null)
            return parentDepartmentEnumeration.getId()+"";
        if (Tools.isNotEmpty(department))
            return department;
        if (Tools.isNotEmpty(parentDepartment))
            return parentDepartment;
        return "";
    }

    public static EnumerationDataBean resolveOrCreate(String constName, String value) {
        int enumerationId = Tools.getIntValue(Constants.getString(constName).substring(Constants.getString(constName).indexOf("_") + 1), -1);
        EnumerationTypeBean enumerationTypeBean = EnumerationTypeDB.getEnumerationById(enumerationId);
        List<EnumerationDataBean> enumerationDataBeans = EnumerationDataDB.getEnumerationDataBy(value,enumerationTypeBean.getEnumerationTypeId());
        if (enumerationDataBeans != null && enumerationDataBeans.size() == 1) {
            return enumerationDataBeans.get(0);
        } else {
            EnumerationDataBean enumerationDataBean = new EnumerationDataBean();
            enumerationDataBean.setType(enumerationTypeBean);
            enumerationDataBean.setString1(value);
            enumerationDataBean.save();
            return enumerationDataBean;
        }
    }

    public static void setSuperiorWorker(List<UserDetails> userDetails) {
        // naplnim pre kazde oddelenie jeho veducim aby som minimzalizoval zataz na databazu
        HashMap<Integer, Integer> cache = new HashMap<>();
        for (UserDetails userDetail : userDetails) {
            int departmentId = Tools.getIntValue(userDetail.getFaxId(),-1);
            if (userDetail.isInUserGroup(Constants.getInt("organisation_structure.holiday_approver",-1))) {
                EnumerationDataBean department = EnumerationDataDB.getInstance().getById(Tools.getIntValue(userDetail.getFaxId(),-1));
                if (department != null && department.getParentEnumerationData() != null)
                    departmentId = department.getParentEnumerationData().getEnumerationDataId();
            }
            Integer superior = cache.get(departmentId);

            if (superior == null) {
                superior = resolveSuperiorWorker(EnumerationDataDB.getEnumerationDataById(departmentId));
                if (superior == null)
                    superior = userDetail.getUserId(); // defaultne je sam sebe nadriadeny
                else
                    cache.put(departmentId,superior);
            }
            userDetail.setParentId(superior);
            UsersDB.saveUser(userDetail);
        }
    }

    /**
     *
     * @param department
     * @return
     */
    public static Integer resolveSuperiorWorker(EnumerationDataBean department) {
        if (department == null)
            return null;
        //todo dorobit checkovanie datumu do UsersDB
        List<UserDetails> userDetails = UsersDB.getUsersByWhereSql(" AND fax = " + department.getId() + " AND authorized = "+DB.getBooleanSql(true));
        for (UserDetails userDetail : userDetails) {
            if (userDetail.isInUserGroup(Constants.getInt("organisation_structure.holiday_approver",-1))) {
                return userDetail.getUserId();
            }
        }
        return resolveSuperiorWorker(department.getParentEnumerationData());
    }

    /**
	 * Vrati zoznam editovatelnych stranok pre dane skupiny
	 * @param editablePagesString
	 * @return
	 */
	public static List<DocDetails> getEditablePages(String editablePagesString)
	{
		List<DocDetails> editablePages = new ArrayList<>();
		StringTokenizer st = new StringTokenizer(editablePagesString, ",");
		int id;
		//LabelValueDetails lvb;
		DocDB docDB = DocDB.getInstance();
		DocDetails page;
		GroupsDB groupsDB = GroupsDB.getInstance();
		while (st.hasMoreTokens())
		{
			id = Integer.parseInt(st.nextToken());
			page = docDB.getDoc(id);
			if (page != null)
			{
				page.setNavbar(Tools.replace(groupsDB.getNavbarNoHref(page.getGroupId()), " > ", "/"));
				editablePages.add(page);
			}
		}
		return (editablePages);
	}

	/**
	 * Vrati zoznam editovatelnych adresarov ako LabelValueDetails List
	 * @param editableGroupsString
	 * @return
	 */
	public static List<LabelValueDetails> getEditableGroups(String editableGroupsString)
	{
		List<LabelValueDetails> editableGroups = new ArrayList<>();
		StringTokenizer st = new StringTokenizer(editableGroupsString, ",");
		int id;
		GroupDetails group;
		GroupsDB groupsDB = GroupsDB.getInstance();
		LabelValueDetails lvb;
		while (st.hasMoreTokens())
		{
			id = Integer.parseInt(st.nextToken());
			group = groupsDB.getGroup(id);
			if (group != null)
			{
				lvb = new LabelValueDetails(group.getFullPath(), Integer.toString(id));
				editableGroups.add(lvb);
			}
		}
		return(editableGroups);
	}

	/**
	 * Vrati zoznam schvalovani adresarov ako List LabelValueDetails - label=cesta, value=groupId, value2=mode
	 * @param userId
	 * @return
	 */
	public static List<LabelValueDetails> getApproveGroups(int userId)
	{
		List<LabelValueDetails> approveGroups = new ArrayList<>();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try
		{
			GroupsDB groupsDB = GroupsDB.getInstance();
			GroupDetails group;
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT * FROM groups_approve WHERE user_id=? ORDER BY approve_id");
			ps.setInt(1, userId);
			rs = ps.executeQuery();

			int groupId;
			int mode;
			String groupName;
			LabelValueDetails lvb;
			while (rs.next())
			{
				groupId = rs.getInt("group_id");
				mode = rs.getInt("approve_mode");
				group = groupsDB.getGroup(groupId);
				if (group!=null)
				{
					groupName = group.getFullPath(); //groupsDB.getNavbarNoHref(groupId);
					lvb = new LabelValueDetails(groupName, Integer.toString(groupId));
					lvb.setValue2(Integer.toString(mode));
					approveGroups.add(lvb);
				}
			}
			rs.close();
			ps.close();

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
				if (db_conn != null)
					db_conn.close();
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
			}
			catch (Exception ex2)
			{
			}
		}
		return(approveGroups);
	}

	/**
	 * Vrati posle so zoznamom zakazanych poloziek pouzivatela
	 * @param userId
	 * @return
	 */
	public static String[] getDisabledItems(int userId)
	{
		String[] disabledItems = new String[0];
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();

			//	nastav disabled items
			ps = db_conn.prepareStatement("SELECT count(user_id) FROM user_disabled_items WHERE user_id=?");
			ps.setInt(1, userId);
			rs = ps.executeQuery();
			int size = 0;
			if (rs.next())
			{
				size = rs.getInt(1);
			}
			rs.close();
			ps.close();

			disabledItems = new String[size];

			ps = db_conn.prepareStatement("SELECT * FROM user_disabled_items WHERE user_id=?");
			ps.setInt(1, userId);
			rs = ps.executeQuery();
			int counter = 0;
			while (rs.next())
			{
				disabledItems[counter++] = DB.getDbString(rs, "item_name");
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
				if (db_conn != null)
					db_conn.close();
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
			}
			catch (Exception ex2)
			{
			}
		}

		return(disabledItems);
	}

	public static int[] getPermGroups(int userId)
	{
		List<PermissionGroupBean> permGroups = UserGroupsDB.getPermissionGroupsFor(userId);
		int[] permGroupIds = new int[permGroups.size()];
		for (ListIterator<PermissionGroupBean> iterator = permGroups.listIterator(); iterator.hasNext();)
		{
			PermissionGroupBean permissionGroup = iterator.next();
			permGroupIds[iterator.nextIndex()-1] = permissionGroup.getUserPermGroupId();
		}

		return permGroupIds;
	}

    /**
	 * Vrati hash-tabulku s poctom pouzivatelov pre jednotlivu skupinu: key =
	 * group_id, value = pocet pouzivatelov
	 *
	 * @return Map&lt;Integer, Integer&gt;
	 */
	public static Map<Integer, Integer> numberOfUsersInGroups()
	{
		Map<Integer, Integer> table = new Hashtable<>();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{

			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT count(user_id) as pocet, user_groups FROM users WHERE "+UsersDB.getDomainIdSqlWhere(false)+" GROUP BY user_groups");
			rs = ps.executeQuery();
			int pocet;
			while (rs.next())
			{
				pocet = rs.getInt("pocet");
				String userGroups = rs.getString("user_groups");
				if (userGroups != null)
				{
					String[] gids = userGroups.split(",");
					for (int i = 0; i < gids.length; i++)
					{
						int key = Tools.getIntValue(gids[i], -1);
						if (key < 0)
							continue;
						if (table.containsKey(key))
						{
							int oldPocet = table.get(key);
							table.put(key, pocet + oldPocet);
						}
						else
						{
							table.put(key, pocet);
						}
					}
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
			}
		}
		return (table);
	}
}
