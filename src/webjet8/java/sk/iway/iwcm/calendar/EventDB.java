package sk.iway.iwcm.calendar;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.SearchTools;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
import sk.iway.iwcm.database.SimpleQuery;

/**
 *		EventDB.java - vykonava pracu s databazou s tabulkou calendar a calendar_types
 *
 *	@Title        webjet7
 *	@Company      Interway s.r.o. (www.interway.sk)
 *	@Copyright    Interway s.r.o. (c) 2001-2010
 *	@author       $Author: kmarton $
 *	@version      $Revision: 1.0 $
 *	@created      Date: 25.06.2010 14:55:51
 *	@modified     $Date: 2010/06/25 14:56:45 $
 */
public class EventDB
{
	/**
	 * Funkcia, ktora vrati zoznam udalosti reprezentovane objektom CalendarDetails podla filtracnych kriterii
	 *
	 * @param eventTypeId	identifikator udalosti
	 * @param searchText		text, ktory hladame v nazve udalosti
	 *
	 * @return Vrati zoznam udalosti, ktore vyhovovali filtracnym kriteriam
	 */
   public static List<CalendarDetails> getEvents(int eventTypeId, String searchText)
   {
   	List<Object> params = new ArrayList<Object>();

   	StringBuilder sql = new StringBuilder();
		sql.append("SELECT c.*, ct.name FROM calendar c, calendar_types ct WHERE c.type_id = ct.type_id "+CloudToolsForCore.getDomainIdSqlWhere(true, "c"));

   	if (eventTypeId > 0)
   	{
   		sql.append(" AND c.type_id = ?");
   		params.add(eventTypeId);
   	}

   	if (Tools.isNotEmpty(searchText))
   	{
   		sql.append(" AND c.title LIKE ?");
   		params.add("%" + searchText + "%");
   	}

   	sql.append(" ORDER BY date_from DESC");

      List<CalendarDetails> events = new ComplexQuery().setSql(sql.toString()).setParams(params.toArray()).list(new Mapper<CalendarDetails>()
      {
      	@Override
         public CalendarDetails map(ResultSet rs) throws SQLException
         {
         	int bgColor = 0;

         	CalendarDetails event = new CalendarDetails();
				event.setCalendarId(rs.getInt("calendar_id"));
             String title = SearchTools.htmlToPlain(DB.getDbString(rs, "title"));

				title = StringUtils.abbreviate(title, 60);

				event.setTitle(title);

				event.setDescription(DB.getDbString(rs, "description"));
				event.setFrom(rs.getTimestamp("date_from"));
				event.setTo(rs.getTimestamp("date_to"));
				event.setTypeId(rs.getInt("type_id"));
				event.setType(DB.getDbString(rs, "name"));
				event.setTimeRange(DB.getDbString(rs, "time_range"));
				event.setArea(DB.getDbString(rs, "area"));
				event.setCity(DB.getDbString(rs, "city"));
				event.setAddress(DB.getDbString(rs, "address"));
				event.setInfo1(DB.getDbString(rs, "info_1"));
				event.setInfo2(DB.getDbString(rs, "info_2"));
				event.setInfo3(DB.getDbString(rs, "info_3"));
				event.setInfo4(DB.getDbString(rs, "info_4"));
				event.setInfo5(DB.getDbString(rs, "info_5"));
				event.setNotifyHoursBefore(rs.getInt("notify_hours_before"));
				event.setNotifyEmails(DB.getDbString(rs, "notify_emails"));
				event.setNotifySender(DB.getDbString(rs, "notify_sender"));
				event.setNotifyIntrotext(DB.getDbString(rs, "notify_introtext"));
				event.setNotifySendSMS(rs.getBoolean("notify_sendsms"));
				event.setLng(DB.getDbString(rs, "lng"));
				event.setCreatorId(rs.getInt("creator_id"));
				event.setApprove(rs.getInt("approve"));
				event.setSuggest(rs.getBoolean("suggest"));
				event.setDomainId(rs.getInt("domain_id"));
				event.setBgColorIndex(bgColor);
				//bgColor = 1 - bgColor; //dead store

         	return event;
         }
      });

		return events;
	}

   /**
    * Funkcia, ktora vymaze danu udalost a zaznam o tom ulozi do auditu
    *
    * @param eventId identifikator udalosti
    */
   public static void deleteEvent(int eventId)
   {
	   if(CalendarDB.getEvent(eventId) != null)
	   {
	   	Adminlog.add(Adminlog.TYPE_CALENDAR_DELETE, "Zmazane podujatie: " + CalendarDB.getEvent(eventId).getTitle(), eventId, -1);
	   	new SimpleQuery().execute("DELETE FROM calendar WHERE calendar_id = ?", eventId);
	   }
   }

   public static void changeApproved(int eventId)
   {
	   int approve=-1;
	   CalendarDetails cd = CalendarDB.getEvent(eventId);
	   if(cd.getApprove()==-1)
		   approve=1;
	   if(cd != null)
	   {
	   	Adminlog.add(Adminlog.TYPE_CALENDAR_UPDATE, "Zmena schvalenia podujatia: " + CalendarDB.getEvent(eventId).getTitle(), eventId, -1);
	   	new SimpleQuery().execute("UPDATE calendar SET approve = ? WHERE calendar_id = ?", approve, eventId);
	   }
   }
}