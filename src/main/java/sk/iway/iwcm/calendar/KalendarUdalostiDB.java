package sk.iway.iwcm.calendar;

import org.apache.commons.lang3.time.DateUtils;
import sk.iway.iwcm.*;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.users.UsersDB;

import jakarta.servlet.http.HttpSession;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.*;

public class KalendarUdalostiDB {

	public static final String D_DOCUMENT_FIELDS_NODATA = "d.doc_id, d.data, d.date_created, d.publish_start, d.publish_end, d.group_id, d.password_protected, d.external_link, d.virtual_path, d.title, d.navbar, d.sort_priority, d.html_data, d.perex_place, d.perex_image, d.perex_group, d.event_date, d.forum_count, d.field_a, d.field_b, d.field_c, d.field_d";

	public final static int SHOW_ALL = 0;
	public final static int SHOW_NOW = 1;
	public final static int SHOW_OLD = 2;
	public final static int SHOW_NEXT = 3;

	public static List<DocDetails> getEvents(String groupIds, boolean priorityViaGroupsIds, String perexGroupIds, int showType, String datumOd, String datumDo, int year, boolean desc, int limit, int cacheMinutes, HttpSession session) {
		return getEvents(false, groupIds, priorityViaGroupsIds, perexGroupIds, showType, datumOd, datumDo, year, desc, limit, cacheMinutes, session);
	}

	@SuppressWarnings("unchecked")
	public static List<DocDetails> getEvents(boolean includeSubdirectories, String groupIds, boolean priorityViaGroupsIds, String perexGroupIds, int showType, String datumOd, String datumDo, int year, boolean desc, int limit, int cacheMinutes, HttpSession session)
	{
		Cache c = Cache.getInstance();
		List<DocDetails> returnList = Collections.emptyList();
		String cacheName = "kalendar_udalosti-" + includeSubdirectories + "-"+groupIds+"-"+perexGroupIds+"-"+datumOd+"-"+datumDo+"-"+year+"-"+showType+"-"+desc+"-"+limit;
		Identity user = UsersDB.getCurrentUser(session);


		boolean notAdmin = user!=null && !user.isAdmin() || user == null;
		if(notAdmin && c.getObject(cacheName)!=null)
		{
			Object o = c.getObject(cacheName);
			if (o!=null && o instanceof List<?>)
			{
				Logger.debug(KalendarUdalostiDB.class, "citam z Cache List<DocDetails> pre groupIds: "+groupIds);
				returnList = (List<DocDetails>)o;
			}
		}
		else
		{
			Logger.debug(KalendarUdalostiDB.class, "nemam nic v cache alebo je to admin");
			returnList = getEventsFromDB(includeSubdirectories, groupIds, priorityViaGroupsIds, perexGroupIds, showType, datumOd, datumDo, year, desc, limit);
			Logger.debug(KalendarUdalostiDB.class, "pisem do Cache List<DocDetails> pre groupIds: "+groupIds+", perexGroupId: "+perexGroupIds+", datumOd: "+datumOd+", datumDo: "+datumDo+", year: "+year+", showType: "+showType);
			c.setObjectSeconds(cacheName, returnList, cacheMinutes*60,true);
		}

		//filter podla prav
		if(user != null)
		{
			List<DocDetails> filtered = new ArrayList<DocDetails>(returnList.size());
			for(DocDetails doc : returnList)
			{
				if(DocDB.canAccess(doc, user))
					filtered.add(doc);
			}
			returnList = filtered;
		}
		return returnList;
	}

	/**
	 * @param groupIds - adresare z kadial sa maju brat udalosti
	 * @param priorityViaGroupsIds - true ak bude priorita zobrazenych udalosti tak ako je poradie groupIds
	 * @param perexGroupId - filter podla perex skupin
	 * @param showType - SHOW_ALL = 0; SHOW_NOW = 1; SHOW_OLD = 2; SHOW_NEXT = 3;
	 * @param month - podla roku
	 * @param datumOd
	 * @param datumDo
	 * @param year
	 * @param desc
	 * @param limit
	 * @return
	 */
	private static List<DocDetails> getEventsFromDB(boolean includeSubdirectories, String groupIds, boolean priorityViaGroupsIds, String perexGroupIds, int showType, String datumOd, String datumDo, int year, boolean desc, int limit)
	{
		if(Tools.isEmpty(groupIds))
			return null;

		groupIds = groupIds.replace('+',',');

		if(Tools.isNotEmpty(perexGroupIds))
			perexGroupIds = perexGroupIds.replace('+',',');

		List<DocDetails> docList = new ArrayList<DocDetails>();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{

			String[] perexGroupIdsArray = null;
			if(Tools.isNotEmpty(perexGroupIds)) perexGroupIdsArray = Tools.getTokens(perexGroupIds, ",");
			boolean perexGroupUseJoin = Constants.getBoolean("perexGroupUseJoin") && perexGroupIdsArray != null;
			String groupNamesIn = "";
			db_conn = DBPool.getConnection();

			StringBuffer sql = new StringBuffer("SELECT "+D_DOCUMENT_FIELDS_NODATA+" FROM documents d WHERE d.group_id IN ("+groupIds+") AND d.available = "+DB.getBooleanSql(true)+" AND d.show_in_menu = "+DB.getBooleanSql(true));
			if(perexGroupIdsArray != null)
			{
				if(perexGroupUseJoin)
				{
					String gn;
					for (int j=0; j<perexGroupIdsArray.length; j++)
					{
						gn = perexGroupIdsArray[j];
						if(Tools.getIntValue(gn,0) > 0) groupNamesIn += gn+(j != perexGroupIdsArray.length-1 ? "," : "");
					}
					if(Tools.isNotEmpty(groupNamesIn)) sql.append(" AND p.perex_group_id IN (?)");
				}
				else
				{
					for(int i=0; i<perexGroupIdsArray.length; i++)
					{
						if(i == 0)
							sql.append(" AND (perex_group LIKE ? OR perex_group LIKE ? OR perex_group LIKE ?");
						else
							sql.append(" OR perex_group LIKE ? OR perex_group LIKE ? OR perex_group LIKE ?");
					}
					if(perexGroupIdsArray.length > 0)
						sql.append(")");
				}
			}

			if(year > 0 && showType == SHOW_ALL)
				sql.append(" AND ((publish_start IS null OR publish_end >= ?) AND (publish_end IS null OR publish_start <= ?))");
			if(Tools.isNotEmpty(datumOd) || Tools.isNotEmpty(datumDo))
			{
				if(Tools.isNotEmpty(datumOd) && Tools.isEmpty(datumDo))
					sql.append(" AND (publish_start IS null OR publish_end >= ?)");
				else if(Tools.isEmpty(datumOd) && Tools.isNotEmpty(datumDo))
					sql.append(" AND (publish_end IS null OR publish_start <= ?)");
				else
					sql.append(" AND ((publish_start IS null OR publish_end >= ?) AND (publish_end IS null OR publish_start <= ?))");
			}
			if (showType == SHOW_NOW)
				sql.append(" AND ((publish_start IS null OR publish_start <= ?) AND (publish_end IS null OR publish_end >= ?))");
			else if (showType == SHOW_OLD)
				sql.append(" AND (publish_end IS NOT null AND publish_end < ?)");
			else if (showType == SHOW_NEXT)
				sql.append(" AND (publish_end IS NOT null AND publish_end > ?)");
			sql.append(" ORDER BY d.publish_start ").append(desc ? "DESC" : "ASC");
			if(limit > 0)
				sql.append(" LIMIT ").append(limit);

			//System.out.println(">> getDocListFromDB SQL: "+sql.toString());
			Logger.debug(KalendarUdalostiDB.class, "getDocListFromDB SQL: "+sql.toString());

			ps = db_conn.prepareStatement(sql.toString());
			int ind = 1;
			if(perexGroupIdsArray != null)
			{
				if(perexGroupUseJoin) ps.setString(ind++, groupNamesIn);
				else
				{
					for(String pg : perexGroupIdsArray)
					{
						ps.setString(ind++, "%,"+pg+",%");
						ps.setString(ind++, "%,"+pg);
						ps.setString(ind++, pg+",%");
					}
				}
			}
			if(year > 0 && showType == SHOW_ALL)
			{
				Calendar firstDay = Calendar.getInstance();
				firstDay.set(Calendar.DATE, 1);
				firstDay.set(Calendar.MONTH, 0);
				firstDay.set(Calendar.YEAR, year);
				firstDay.set(Calendar.HOUR_OF_DAY, 0);
				firstDay.set(Calendar.MINUTE, 0);
				firstDay.set(Calendar.SECOND, 0);
				firstDay.set(Calendar.MILLISECOND, 0);
				Calendar lastDay = Calendar.getInstance();
				lastDay.set(Calendar.DATE, 31);
				lastDay.set(Calendar.MONTH, 11);
				lastDay.set(Calendar.YEAR, year);
				lastDay.set(Calendar.HOUR_OF_DAY, 23);
				lastDay.set(Calendar.MINUTE, 59);
				lastDay.set(Calendar.SECOND, 59);
				lastDay.set(Calendar.MILLISECOND, 999);
				ps.setTimestamp(ind++, new Timestamp(firstDay.getTimeInMillis()));
				ps.setTimestamp(ind++, new Timestamp(lastDay.getTimeInMillis()));
			}
			if(Tools.isNotEmpty(datumOd) || Tools.isNotEmpty(datumDo))
			{
				Calendar fromDate = Calendar.getInstance();
				fromDate.setTimeInMillis(DB.getTimestamp(datumOd, "0:00:00"));
				Calendar toDate = Calendar.getInstance();
				toDate.setTimeInMillis(DB.getTimestamp(datumDo, "23:59:59"));

				if(Tools.isNotEmpty(datumOd) && Tools.isEmpty(datumDo))
					ps.setTimestamp(ind++, new Timestamp(fromDate.getTimeInMillis()));
				else if(Tools.isEmpty(datumOd) && Tools.isNotEmpty(datumDo))
					ps.setTimestamp(ind++, new Timestamp(toDate.getTimeInMillis()));
				else
				{
					ps.setTimestamp(ind++, new Timestamp(fromDate.getTimeInMillis()));
					ps.setTimestamp(ind++, new Timestamp(toDate.getTimeInMillis()));
				}
			}
			if (showType == SHOW_NOW)
			{
				Calendar today = Calendar.getInstance();
				if(year > 0)
					today.set(Calendar.YEAR, year);
				today.set(Calendar.HOUR_OF_DAY, 23);
				today.set(Calendar.MINUTE, 59);
				today.set(Calendar.SECOND, 59);
				today.set(Calendar.MILLISECOND, 999);
				ps.setTimestamp(ind++, new Timestamp(today.getTimeInMillis()));
				today.set(Calendar.HOUR_OF_DAY, 0);
				today.set(Calendar.MINUTE, 0);
				today.set(Calendar.SECOND, 0);
				today.set(Calendar.MILLISECOND, 0);
				ps.setTimestamp(ind++, new Timestamp(today.getTimeInMillis()));
			}
			else if (showType == SHOW_OLD)
			{
				Calendar today = Calendar.getInstance();
				if(year > 0)
					today.set(Calendar.YEAR, year);
				today.set(Calendar.HOUR_OF_DAY, 0);
				today.set(Calendar.MINUTE, 0);
				today.set(Calendar.SECOND, 0);
				today.set(Calendar.MILLISECOND, 0);
				ps.setTimestamp(ind++, new Timestamp(today.getTimeInMillis()));
			}
			else if (showType == SHOW_NEXT)
			{
				Calendar today = Calendar.getInstance();
				if(year > 0)
					today.set(Calendar.YEAR, year);
				today.set(Calendar.HOUR_OF_DAY, 23);
				today.set(Calendar.MINUTE, 59);
				today.set(Calendar.SECOND, 59);
				today.set(Calendar.MILLISECOND, 999);
				ps.setTimestamp(ind++, new Timestamp(today.getTimeInMillis()));
			}
			rs = ps.executeQuery();
			while (rs.next())
			{
				DocDetails doc = new DocDetails();

				doc.setDocId(rs.getInt("doc_id"));
				doc.setData(rs.getString("data"));
				doc.setDateCreated(DB.getDbTimestamp(rs, "date_created"));
				doc.setPublishStart(DB.getDbTimestamp(rs, "publish_start"));
				doc.setPublishEnd(DB.getDbTimestamp(rs, "publish_end"));
				doc.setGroupId(rs.getInt("group_id"));
				doc.setPasswordProtected(DB.getDbString(rs, "password_protected"));
				doc.setExternalLink(DB.getDbString(rs, "external_link"));
				doc.setVirtualPath(DB.getDbString(rs, "virtual_path"));
				doc.setTitle(DB.getDbString(rs, "title"));
				doc.setNavbar(DB.getDbString(rs, "navbar"));
				doc.setSortPriority(rs.getInt("sort_priority"));
				doc.setHtmlData(DB.getDbString(rs, "html_data"));
				doc.setPerexPlace(DB.getDbString(rs, "perex_place"));
				doc.setPerexImage(DB.getDbString(rs, "perex_image"));
				doc.setPerexGroupString(DB.getDbString(rs, "perex_group"));
				doc.setEventDate(DB.getDbTimestamp(rs, "event_date"));
				doc.setForumCount(rs.getInt("forum_count"));
				doc.setFieldA(DB.getDbString(rs, "field_a"));
				doc.setFieldB(DB.getDbString(rs, "field_b"));
				doc.setFieldC(DB.getDbString(rs, "field_c"));
				doc.setFieldD(DB.getDbString(rs, "field_d"));

				docList.add(doc);
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

		List<String> groupIdsTokens = new ArrayList<>(Tools.getStringListValue(Tools.getTokens(groupIds,",")));

		if (includeSubdirectories) {
			GroupsDB groupsDB = GroupsDB.getInstance();
			Set<String> subgroupIds = getSubgroupIds(groupsDB, groupIdsTokens);
			if (!subgroupIds.isEmpty()) {
				for (String subgroupId : subgroupIds) {
					groupIdsTokens.add(subgroupId);
				}
			}
		}

		//usporiadam podla groupIds
		if(priorityViaGroupsIds && docList != null && docList.size() > 0)
		{
			List<DocDetails> newResult = new ArrayList<DocDetails>();

			for(String token : groupIdsTokens)
			{
				int groupId = Tools.getIntValue(token, 0);
				for(DocDetails doc : docList)
				{
					if(doc.getGroupId() == groupId)
						newResult.add(doc);
				}
			}

			if(newResult.size() > 0)
			{
				docList = null;
				docList = new ArrayList<DocDetails>(newResult);
			}
		}

		return docList;
	}

	private static Set<String> getSubgroupIds(GroupsDB groupsDB, List<String> groupIdsTokens) {
		Set<String> result = new HashSet<>();

		for (String groupId : groupIdsTokens) {
			int id = Tools.getIntValue(groupId, 0);
			if (id == 0) {
				break;
			}
			String subgroupsIds = groupsDB.getSubgroupsIds(id);
			if (subgroupsIds.equals("-1")) {
				break;
			}

			List<String> subgroupIds = new ArrayList<>(Tools.getStringListValue(Tools.getTokens(subgroupsIds, ",")));
			if (subgroupIds.contains("" + id)) {
				subgroupIds.remove(subgroupIds.indexOf("" + id));
			}
			result.addAll(subgroupIds);
		}

		return result;
	}

	/**
	 * vrati akcie pre zadany den z ArrayListu
	 * @param events
	 * @param datum
	 * @return
	 */
	public static List<DocDetails> getEventsForDay(List<DocDetails> events, long datum)
	{
		List<DocDetails> result = new ArrayList<DocDetails>();
		datum = DB.getTimestamp(Tools.formatDate(datum));
		for (DocDetails event : events)
		{
			long calFrom = -1;
			long calTo = -1;
			if(Tools.isNotEmpty(event.getPublishStartString()))
				calFrom =  DB.getTimestamp(event.getPublishStartString());
			if(Tools.isNotEmpty(event.getPublishEndString()))
				calTo =  DB.getTimestamp(event.getPublishEndString());

			if(calFrom != -1 && calTo == -1)
			{
				if(DateUtils.isSameDay(new Date(calFrom), new Date(datum)))
				//if(calFrom == datum)
					result.add(event);
			}
			else if(calFrom != -1 && calTo != -1)
			{
				if(calFrom <= datum && datum <= calTo)
					result.add(event);
			}
		}

		return result;
	}

	/**
	 * vrati nasledujucich x akcii pre zadany den, maximalne vsak do konca mesiaca
	 * neberie do uvahy udalosti zacinajuce v danom dni
	 * @param events eventy
	 * @param datum od kedy
	 * @param count pocet udalosti
	 * @param till datum dokedy sa ma maximalne hladat
	 * @return
	 */
	public static List<DocDetails> getNextEvents(List<DocDetails> events, long datum, int count, long till)
	{
		if(count < 1) throw new IllegalArgumentException("count must be positive number");
		if(events == null) throw new IllegalArgumentException("Events can't be null");
		List<DocDetails> result = new ArrayList<DocDetails>(count);
		datum = DB.getTimestamp(Tools.formatDate(datum));
		for (DocDetails event : events)
		{
			long calFrom = -1;
			if(Tools.isNotEmpty(event.getPublishStartString()))
				calFrom =  DB.getTimestamp(event.getPublishStartString());
			if(calFrom != -1)
			{
				if(calFrom > datum && calFrom < till)
					result.add(event);
			}
			if(result.size() == count) break;
		}

		return result;
	}

	private static CalendarDetails fillCalendarDetails(ResultSet rs)
	{
		CalendarDetails cal = null;
		try
		{
			cal = new CalendarDetails();
			cal.setCalendarId(rs.getInt("calendar_id"));
			cal.setTitle(DB.getDbString(rs, "title"));
			cal.setDescription(DB.getDbString(rs, "description"));

			try
			{
				//ak je tam len cislo, je to docId
				if (cal.getDescription().length() > 0 && cal.getDescription().length() < 6)
				{
					int docId = Integer.parseInt(cal.getDescription().trim());
					if (docId > 0)
					{
						DocDB docDB = DocDB.getInstance();
						DocDetails doc = docDB.getDoc(docId);
						if (doc != null)
						{
							cal.setDescription(doc.getData());
						}
					}
				}
			}
			catch (Exception ex)
			{
				//sk.iway.iwcm.Logger.error(ex);
			}

			cal.setFrom(rs.getTimestamp("date_from"));
			cal.setTo(rs.getTimestamp("date_to"));
			cal.setTypeId(rs.getInt("type_id"));
			cal.setType(DB.getDbString(rs, "name"));
			cal.setTimeRange(DB.getDbString(rs, "time_range"));
			cal.setArea(DB.getDbString(rs, "area"));
			cal.setCity(DB.getDbString(rs, "city"));
			cal.setAddress(DB.getDbString(rs, "address"));
			cal.setInfo1(DB.getDbString(rs, "info_1"));
			cal.setInfo2(DB.getDbString(rs, "info_2"));
			cal.setInfo3(DB.getDbString(rs, "info_3"));
			cal.setInfo4(DB.getDbString(rs, "info_4"));
			cal.setInfo5(DB.getDbString(rs, "info_5"));
			cal.setNotifyHoursBefore(rs.getInt("notify_hours_before"));
			cal.setNotifyEmails(DB.getDbString(rs, "notify_emails"));
			cal.setNotifySender(DB.getDbString(rs, "notify_sender"));
			cal.setNotifyIntrotext(DB.getDbString(rs, "notify_introtext"));
			cal.setNotifySendSMS(rs.getBoolean("notify_sendsms"));
			cal.setLng(DB.getDbString(rs, "lng"));
			cal.setCreatorId(rs.getInt("creator_id"));
			cal.setApprove(rs.getInt("approve"));
			cal.setSuggest(rs.getBoolean("suggest"));
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		return cal;
	}

	/**
	 * vrati udalosti na zaklade fakulty s prioritou rektorskych udalosti
	 * @param fakulta - _vsetky, _rektorat, faculta_prava, faculta_ekonomie_podnikania, faculta_masmedii, faculta_informatiky
	 * @param typUdalostiId
	 * @param now - prebiehajuce
	 * @param past - minule
	 * @param future - buduce
	 * @param year - podla roku
	 * @param offset
	 * @param count
	 * @param datumOd
	 * @param datumDo
	 * @return
	 */
	public static List<CalendarDetails> getUdalosti(String fakulta, int typUdalostiId,  boolean now, boolean past, boolean future, int year, int offset, int count, String datumOd, String datumDo)
	{
		List<CalendarDetails> result = null;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			StringBuffer sb = new StringBuffer("SELECT c.*, ct.name FROM calendar c, calendar_types ct WHERE c.type_id=ct.type_id");
			if(Tools.isNotEmpty(fakulta))
			{
				//ak zobrazujem akcie fakulty, tak musim hladat aj rektorske udalosti
				if(!"_vsetky".equals(fakulta) || !"_rektorat".equals(fakulta))
					sb.append(" AND (info_5 = ? OR info_5 = ?)");
				else
					sb.append(" AND info_5 = ?");
			}
			if(typUdalostiId > 0)
				sb.append(" AND c.type_id = ?");
			if(Tools.isNotEmpty(datumOd) && Tools.isNotEmpty(datumDo))
			{
				sb.append(" AND date_from >= ? AND date_from <= ?");
			}
			else
			{
				if(year > 0 && !now && !past && !future)
					sb.append(" AND date_from >= ? AND date_to <= ?");
				if(now)
					sb.append(" AND date_from <= ? AND  ? <= date_to");
				else if(past)
					sb.append(" AND date_to < ?");
				else if(future)
					sb.append(" AND ? < date_to");
			}
			sb.append(" ORDER BY info_5 DESC, date_from ASC");
			if(count > 0)
				sb.append(" LIMIT "+offset+", "+count);
			System.out.println(">> getUdalosti SQL: "+sb.toString());
			Logger.debug(KalendarUdalostiDB.class, "getUdalosti SQL: "+sb.toString());
			ps = db_conn.prepareStatement(sb.toString());
			int ind = 1;
			if(Tools.isNotEmpty(fakulta))
			{
				if(!"_vsetky".equals(fakulta) || !"_rektorat".equals(fakulta))
					ps.setString(ind++, "_rektorat");
				ps.setString(ind++, fakulta);
			}
			if(typUdalostiId > 0)
				ps.setInt(ind++, typUdalostiId);
			if(Tools.isNotEmpty(datumOd) && Tools.isNotEmpty(datumDo))
			{
				ps.setTimestamp(ind++, new Timestamp(DB.getTimestamp(datumOd, "0:00:00")));
				ps.setTimestamp(ind++, new Timestamp(DB.getTimestamp(datumDo, "0:00:00")));
			}
			else
			{
				if(year > 0 && !now && !past && !future)
				{
					Calendar firstDay = Calendar.getInstance();
					firstDay.set(Calendar.DATE, 1);
					firstDay.set(Calendar.MONTH, 0);
					firstDay.set(Calendar.YEAR, year);
					firstDay.set(Calendar.HOUR_OF_DAY, 0);
					firstDay.set(Calendar.MINUTE, 0);
					firstDay.set(Calendar.SECOND, 0);
					firstDay.set(Calendar.MILLISECOND, 0);
					Calendar lastDay = Calendar.getInstance();
					lastDay.set(Calendar.DATE, 31);
					lastDay.set(Calendar.MONTH, 11);
					lastDay.set(Calendar.YEAR, year);
					lastDay.set(Calendar.HOUR_OF_DAY, 0);
					lastDay.set(Calendar.MINUTE, 0);
					lastDay.set(Calendar.SECOND, 0);
					lastDay.set(Calendar.MILLISECOND, 0);
					ps.setTimestamp(ind++, new Timestamp(firstDay.getTimeInMillis()));
					ps.setTimestamp(ind++, new Timestamp(lastDay.getTimeInMillis()));
				}
				if(now)
				{
					Calendar today = Calendar.getInstance();
					if(year > 0)
						today.set(Calendar.YEAR, year);
					today.set(Calendar.HOUR_OF_DAY, 0);
					today.set(Calendar.MINUTE, 0);
					today.set(Calendar.SECOND, 0);
					today.set(Calendar.MILLISECOND, 0);
					ps.setTimestamp(ind++, new Timestamp(today.getTimeInMillis()));
					ps.setTimestamp(ind++, new Timestamp(today.getTimeInMillis()));
				}
				else if(past)
				{
					Calendar today = Calendar.getInstance();
					if(year > 0)
						today.set(Calendar.YEAR, year);
					today.set(Calendar.HOUR_OF_DAY, 0);
					today.set(Calendar.MINUTE, 0);
					today.set(Calendar.SECOND, 0);
					today.set(Calendar.MILLISECOND, 0);
					ps.setTimestamp(ind++, new Timestamp(today.getTimeInMillis()));
				}
				else if(future)
				{
					Calendar today = Calendar.getInstance();
					if(year > 0)
						today.set(Calendar.YEAR, year);
					today.set(Calendar.HOUR_OF_DAY, 0);
					today.set(Calendar.MINUTE, 0);
					today.set(Calendar.SECOND, 0);
					today.set(Calendar.MILLISECOND, 0);
					ps.setTimestamp(ind++, new Timestamp(today.getTimeInMillis()));
				}
			}
			rs = ps.executeQuery();
			while (rs.next())
			{
				if(result == null)
					result = new ArrayList<CalendarDetails>();

				result.add(fillCalendarDetails(rs));
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

		return result;
	}
}
