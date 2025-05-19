package sk.iway.iwcm.doc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.cron.CronFacade;

/**
 *  GroupPublisher.java
 *
 *  Encapsulates operations over groups_scheduler SQL table.
 *  Publishes scheduled changes made to the {@link GroupDetails} instance
 *
 *@Title        webjet4_repair
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: murbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 31.8.2009 17:53:21
 *@modified     $Date: 2009/09/07 10:52:39 $
 */
public class GroupPublisher
{
	static final SimpleQuery query = new SimpleQuery();

	static final long MINUTE = 60*1000L;

	public static boolean addRecord(GroupDetails group, Date publishDate, int userId)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			GroupDetails g = group;
			Timestamp pd = null;

			if(publishDate!=null)
				pd = new Timestamp(publishDate.getTime());

			String[] additionalFields = DataAccessHelper.getGroupFields();
			StringBuilder addFieldsInsert = new StringBuilder();
			StringBuilder addFieldsInsertParams = new StringBuilder();
			if (additionalFields != null && additionalFields.length>0) {
				for (String field : additionalFields) {
					addFieldsInsert.append(", ").append(field);
					addFieldsInsertParams.append(", ?");
				}
			}

			String sql = "INSERT INTO groups_scheduler (save_date, group_id, group_name, internal, parent_group_id, navbar, " +
					"default_doc_id, temp_id, sort_priority, password_protected, menu_type, url_dir_name," +
					" html_head, logon_page_doc_id, domain_name, new_page_docid_template, install_name, " +
					"field_a, field_b, field_c, field_d, logged_menu_type, link_group_id," +
					"when_to_publish, user_id, lng, hidden_in_admin, force_group_template" +
					addFieldsInsert.toString() +
					") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?"+addFieldsInsertParams.toString()+")";

			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql);

			Object[] parameters = {new Date(), g.getGroupId(), g.getGroupName(), g.isInternal(), g.getParentGroupId(), g.getNavbarName(), g.getDefaultDocId(),
				g.getTempId(), g.getSortPriority(), g.getPasswordProtected(), g.getMenuType(), g.getUrlDirName(),
				g.getHtmlHead(), g.getLogonPageDocId(), g.getDomainName(), g.getNewPageDocIdTemplate(), g.getInstallName(),
				g.getFieldA(), g.getFieldB(), g.getFieldC(), g.getFieldD(), g.getLoggedMenuType(), g.getLinkGroupId(), pd, userId, g.getLng(), g.isHiddenInAdmin(), g.isForceTheUseOfGroupTemplate()};

			SimpleQuery.bindParameters(ps, parameters);

			DataAccessHelper.setGroupPreparedStatement(ps, g, parameters.length+1);

			ps.execute();
			ps.close();
			db_conn.close();
			ps = null;
			db_conn = null;

			return true;
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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		return false;
	}

	public static boolean addRecord(GroupDetails group, Date publishDate)
	{
		return addRecord(group, publishDate, -1);
	}

	public static void deleteRecord(int scheduleId)
	{
		query.execute("DELETE FROM groups_scheduler WHERE schedule_id = ?", scheduleId);
	}

	public static void markAsPublished(int scheduleId)
	{
		query.execute("UPDATE groups_scheduler SET date_published=? WHERE schedule_id = ? AND date_published IS NULL", new Timestamp(Tools.getNow()), scheduleId);
	}

	/**
	 * Publishes scheduled changes in {@link GroupDetails} attributes
	 * Called main because of Cron's naming restrictions, @see {@link CronFacade}
	 */
	public static void main(String[] args)
	{
		try{
		@SuppressWarnings("unchecked")
		List<Number> rowIds = query.forList(
			"SELECT schedule_id FROM groups_scheduler WHERE when_to_publish IS NOT NULL AND date_published IS NULL AND when_to_publish <= ?", new Timestamp(System.currentTimeMillis())
		);

		if (rowIds.size() == 0)
			return;

		for (Number id : rowIds)
		{
			publish(id.intValue());
			markAsPublished(id.intValue());
		}

		GroupsDB.getInstance(true);
		//mohla sa zmenit URL linka adresara
		DocDB.getInstance(true);
		}
		catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
		}
	}

	private static void publish(int historyId)
	{
		GroupsDB groupsDB = GroupsDB.getInstance();
		GroupDetails group = getGroupFromGroupsHistory(historyId);

		GroupDetails origGroup = groupsDB.getGroup(group.getGroupId());

		if(origGroup != null)
		{
			// zisti ci sme v adresari /System/Trash (kos), ak ano, nepublikuj
	      String navbarNoHref = DB.internationalToEnglish(groupsDB.getURLPath(origGroup.getGroupId())).toLowerCase();
	      //tu sa vytvara adresar podla default jazyka, nie podla prihlaseneho pouzivatela!
	      Prop propSystem = Prop.getInstance(Constants.getString("defaultLanguage"));
	      String trashDirName = propSystem.getText("config.trash_dir");

	   	if (navbarNoHref.startsWith(DB.internationalToEnglish(trashDirName).toLowerCase())==false)
	      {
	   		GroupsDB.getInstance().setGroup(group);
	      }
		}
	}


	public static Map<Integer, Date> getScheduledChangesForGroupWithId(int groupId)
	{
		@SuppressWarnings("unchecked")
		List<Number> historyIds = query.forList(
			"SELECT schedule_id FROM groups_scheduler WHERE group_id = ? AND when_to_publish IS NOT NULL", groupId
		);

		Map<Integer, Date> scheduled = new HashMap<>();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT when_to_publish FROM groups_scheduler WHERE schedule_id = ? AND when_to_publish IS NOT NULL");
			for (Number id : historyIds)
			{
				ps.setInt(1, id.intValue());
				rs = ps.executeQuery();
				while (rs.next())
				{
					scheduled.put(id.intValue(), new java.util.Date(DB.getDbTimestamp(rs, "when_to_publish")));
				}
				rs.close();
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

		return scheduled;
	}

	public static List<GroupSchedulerDetails> getGroupsFromGroupsHistory(int groupId)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		List<GroupSchedulerDetails> listGroupsSchedulerDetails = new ArrayList<>();
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT * FROM groups_scheduler WHERE group_id = ?");
			ps.setInt(1, groupId);
			rs = ps.executeQuery();
			while (rs.next())
			{
				listGroupsSchedulerDetails.add(GroupsDB.fillFieldsByResultSetFromScheduler(rs));
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
		return listGroupsSchedulerDetails;
	}

	public static GroupDetails getGroupFromGroupsHistory(int scheduleId)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT * FROM groups_scheduler WHERE schedule_id = ?");
			ps.setInt(1, scheduleId);
			rs = ps.executeQuery();
			while (rs.next())
			{
				return GroupsDB.fillFieldsByResultSet(rs);
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
		throw new IllegalStateException("No row found for scheduleId: "+scheduleId);
	}

	/**
	 * Returns list of publicable directories
	 * @return list of publicable directories
	 */
	public static List<GroupDetails> getPublicableDirs()
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		GroupDetails gd = null;
		GroupDetails groupDet = null;
		List<GroupDetails> publicableGroups = new ArrayList<>();
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT * FROM groups_scheduler");
			rs = ps.executeQuery();
			while (rs.next())
			{
				groupDet = (GroupsDB.getInstance()).getGroup(rs.getInt("group_id"));
			  	if(groupDet == null) continue;

			  	String link = groupDet.getFullPath();	//cesta adresara

			  	gd = new GroupDetails();
				gd.setGroupId(rs.getInt("group_id"));
				gd.setGroupName(DB.getDbString(rs, "group_name"));
				SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
				gd.setFieldA(sdf.format(rs.getDate("when_to_publish")));
				gd.setTempId(rs.getInt("schedule_id"));
				gd.setFullPath(link);
				publicableGroups.add(gd);
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
		return publicableGroups;
	}
}