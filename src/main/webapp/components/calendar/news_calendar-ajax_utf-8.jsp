<%@page import="java.util.List"%><%@page import="sk.iway.iwcm.users.UsersDB"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*, java.util.*,sk.iway.iwcm.calendar.*, java.text.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page import="java.sql.Timestamp"%>
<%@page import="java.sql.ResultSet"%>
<%@page import="java.sql.PreparedStatement"%>
<%@page import="java.sql.Connection"%>
<%@page import="sk.iway.iwcm.doc.DocDetails"%>
<%@page import="sk.iway.iwcm.doc.DocDB"%>
<%@page import="sk.iway.iwcm.doc.GroupDetails"%>
<%@page import="sk.iway.iwcm.doc.GroupsDB"%>
<%!
public static final Object LOCK = new Object();

public final String D_DOCUMENT_FIELDS = "d.doc_id, d.data, d.date_created, d.publish_start, d.publish_end, d.author_id, d.searchable, d.group_id, d.available, d.show_in_menu, d.password_protected, d.cacheable, d.external_link, d.virtual_path, d. temp_id, d.title, d.navbar, d.file_name, d.sort_priority, d.header_doc_id, d.footer_doc_id, d.menu_doc_id, d.right_menu_doc_id, d.html_head, d.html_data, d.perex_place, d.perex_image, d.perex_group, d.event_date, d.sync_id, d.sync_status, d.field_a, d.field_b, d.field_c, d.field_d, d.field_e, d.field_f, d.field_g, d.field_h, d.field_i, d.field_j, d.field_k, d.field_l, d.disable_after_end, d.forum_count, d.views_total, d.field_m, d.field_n, d.field_o, d.field_p, d.field_q, d.field_r, d.field_s, d.field_t, d.require_ssl";

public List<DocDetails> getEventsForMonth(String groupIds, String perexGroupIds, boolean expandGroupIds, long dateFrom, long dateTo)
{
	if(Tools.isEmpty(groupIds))
		return null;

	if (expandGroupIds)
	{
		StringBuilder searchGroups = null;
		StringTokenizer st = new StringTokenizer(groupIds, ",+; ");
		GroupsDB groupsDB = GroupsDB.getInstance();
		List searchGroupsArray;
		GroupDetails group;
		Iterator iter;
		int searchRootGroupId;
		while (st.hasMoreTokens())
		{
			try
			{
				String sid = st.nextToken();
				sid = sid.replace('(', ' ');
				sid = sid.replace(')', ')');
				sid = sid.trim();
				searchRootGroupId = Integer.parseInt(sid);
			}
			catch (Exception ex)
			{
				Logger.printlnError(DocDB.class, "groupIds="+groupIds);
				sk.iway.iwcm.Logger.error(ex);
				continue;
			}
			//najdi child grupy
			searchGroupsArray = groupsDB.getGroupsTree(searchRootGroupId, true, false);
			iter = searchGroupsArray.iterator();
			while (iter.hasNext())
			{
				group = (GroupDetails) iter.next();
				if (group != null)
				{
					if (searchGroups == null)
					{
						searchGroups = new StringBuilder(String.valueOf(group.getGroupId()));
					}
					else
					{
						searchGroups.append(',').append(group.getGroupId());
					}
				}
			}
		}
		if (searchGroups!=null) groupIds = searchGroups.toString();
	}

	List<DocDetails> result = new ArrayList<DocDetails>();

	Connection db_conn = null;
	PreparedStatement ps = null;
	ResultSet rs = null;
	try
	{
		db_conn = DBPool.getConnection();
		StringBuffer sql = new StringBuffer("SELECT u.title as u_title, u.first_name, u.last_name, u.email, "+D_DOCUMENT_FIELDS+" FROM documents d LEFT JOIN users u ON d.author_id=u.user_id WHERE available=1  AND group_id IN ("+groupIds+") AND html_data IS NOT NULL AND (publish_start IS NOT null AND publish_start >= ? AND publish_start <= ?) AND (password_protected IS null OR password_protected='')");
		if(Tools.isNotEmpty(perexGroupIds))
		{
			String[] perexGroupIdsArray = Tools.getTokens(perexGroupIds, ",");
			if(perexGroupIdsArray != null)
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
		sql.append(" ORDER BY publish_start DESC, sort_priority ASC LIMIT 0, 200");
		System.out.println(">>> SQL: "+sql.toString());
		ps = db_conn.prepareStatement(sql.toString());
		int ind = 1;
		ps.setTimestamp(ind++, new Timestamp(dateFrom));
		ps.setTimestamp(ind++, new Timestamp(dateTo));
		if(Tools.isNotEmpty(perexGroupIds))
		{
			String[] perexGroupIdsArray = Tools.getTokens(perexGroupIds, ",");
			if(perexGroupIdsArray != null)
			{
				for(String pg : perexGroupIdsArray)
				{
					ps.setString(ind++, "%,"+pg+",%");
					ps.setString(ind++, "%,"+pg);
					ps.setString(ind++, pg+",%");
				}
			}

		}
		rs = ps.executeQuery();
		DocDetails docDet = null;
		while (rs.next())
		{
			docDet = DocDB.getDocDetails(rs, false, true);
			if(docDet != null)
				result.add(docDet);
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

public static List<DocDetails> getEventsForDay(List<DocDetails> events, long datum)
{
	List<DocDetails> result = new ArrayList<DocDetails>();
	datum = DB.getTimestamp(Tools.formatDate(datum));
	for (DocDetails event : events)
	{
		long publishStart = -1;
		if(Tools.isNotEmpty(event.getPublishStartString()))
			publishStart =  event.getPublishStart();

		if(publishStart != -1 && Tools.formatDate(publishStart).equals(Tools.formatDate(datum)))
			result.add(event);
	}

	return result;
}
%>
<%
  int mesiacActual = Tools.getIntValue(request.getParameter("mesiac"), -1);
  int rokActual = Tools.getIntValue(request.getParameter("rok"), -1);
  if(mesiacActual == -1 || rokActual == -1)
    return;
  Calendar calFirstDay = Calendar.getInstance();
  calFirstDay.set(Calendar.MONTH, mesiacActual);
  calFirstDay.set(Calendar.YEAR, rokActual);
  calFirstDay.set(Calendar.DATE, 1);
  calFirstDay.set(Calendar.HOUR_OF_DAY, 0);
  calFirstDay.set(Calendar.MINUTE, 0);
  calFirstDay.set(Calendar.SECOND, 0);
  calFirstDay.set(Calendar.MILLISECOND, 1);
  Calendar calLastDay = Calendar.getInstance();
  calLastDay.set(Calendar.MONTH, mesiacActual);
  calLastDay.set(Calendar.YEAR, rokActual);
  calLastDay.set(Calendar.DATE, 1);
  calLastDay.set(Calendar.DATE, calLastDay.getActualMaximum(Calendar.DATE));
  calLastDay.set(Calendar.HOUR_OF_DAY, 23);
  calLastDay.set(Calendar.MINUTE, 59);
  calLastDay.set(Calendar.SECOND, 59);
  calLastDay.set(Calendar.MILLISECOND, 999);

  String groupIds = Tools.getStringValue(request.getParameter("groupIds"), "");
  groupIds = groupIds.replace('+', ',');
  String perexGroup =  Tools.getStringValue(request.getParameter("perexGroup"), "");
  perexGroup = perexGroup.replace('+', ',');
  //ak true, zahrnie aj podadersare
  String expandGroupIds = Tools.getStringValue(request.getParameter("expandGroupIds"), "false");

  List<DocDetails> events = null;
  Cache c = Cache.getInstance();
  String cacheKey = "components.calendar.calendar_news.mesiac."+mesiacActual+".rok."+rokActual+".groupIds."+groupIds+".perexGroup."+perexGroup+".expandGroupIds."+expandGroupIds;
  int cacheInMinutes = Tools.getIntValue(request.getParameter("cacheInMinutes"), 10);
  Object o = c.getObject(cacheKey);
  Identity user = UsersDB.getCurrentUser(request);
  boolean isAdmin = false;
  if (user != null) isAdmin = true;

  System.out.println("========== c="+o);

  if(isAdmin == false && o != null)
  {
	  events = (List<DocDetails>)o;
  }
  else
  {
    // doublecheck ochrana pred duplicitnym volanim DB (v kombinacii s nizsie volanym nasetovanim prazdneho ArrayListu)
    synchronized(LOCK)
    {
      o = c.getObject(cacheKey);

      if(isAdmin==false && o != null) //&& (user == null || user.isAdmin()==false))
      {
      	events = (List<DocDetails>)o;
      }
      else
      {
          //ochrana pred duplicitnym volanim DB
          if (c.getObject(cacheKey)==null)
          		c.setObjectSeconds(cacheKey, new ArrayList<DocDetails>() , cacheInMinutes*60, true);

          events = getEventsForMonth(groupIds, perexGroup, Boolean.valueOf(expandGroupIds).booleanValue(), calFirstDay.getTimeInMillis(), calLastDay.getTimeInMillis());
          c.setObjectSeconds(cacheKey, events , cacheInMinutes*60, true);
      }
    }
  }

  Calendar datum = Calendar.getInstance();
  datum.set(Calendar.DATE, 1);
  datum.set(Calendar.MONTH, mesiacActual);
  datum.set(Calendar.YEAR, rokActual);
  SimpleDateFormat sdf = new SimpleDateFormat("d_M_yyyy");
  for(int j = 0; j < calLastDay.get(Calendar.DATE); j++)
  {
    if(j != 0)
      datum.add(Calendar.DATE, 1);
    String datumStr = "#"+sdf.format(datum.getTimeInMillis());
    List<DocDetails> dayEvents = getEventsForDay(events, datum.getTimeInMillis());

    if(dayEvents.size() > 0)
    {
  		 %>$('<%=datumStr%>').addClass('akcia');<%
    }
  }
%>
