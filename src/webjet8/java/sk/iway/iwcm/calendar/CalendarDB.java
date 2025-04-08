package sk.iway.iwcm.calendar;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import sk.iway.Password;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageParams;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.users.PasswordSecurity;
import sk.iway.iwcm.users.SettingsBean;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UserGroupDetails;
import sk.iway.iwcm.users.UserGroupsDB;
import sk.iway.iwcm.users.UsersDB;

/**
 *  CalendarDB - kalendar podujati, meniny
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       not attributable
 *@version      1.0
 *@created      Štvrtok, 2004, február 12
 *@modified     $Date: 2004/03/18 15:45:04 $
 */
public class CalendarDB
{
	public static final int FORUM_OFFSET = 1000000;

	/**
	 *  vrati hodnotu parametra, alebo attributu (ak nie je parameter)
	 *
	 *@param  name
	 *@param  request
	 *@return
	 */
	private static String getParamAttribute(String name, HttpServletRequest request)
	{
		String ret = request.getParameter(name);
		if (ret == null)
		{
			ret = (String) request.getAttribute(name);
		}
		return (ret);
	}

	/**
	 *  vrati hodnotu parametra, alebo attributu (ak nie je parameter) ako cislo, alebo -1 ak sa o cislo nejedna
	 *
	 *@param  name
	 *@param  request
	 *@return
	 */
	private static int getParamAttributeInt(String name, HttpServletRequest request)
	{
		String tmp = getParamAttribute(name, request);
		int i = -1;

		try
		{
			if (tmp != null)
			{
				i = Integer.parseInt(tmp);
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return (i);
	}

	/**
	 *  vrati zoznam udalosti kalendara v zavislosti na parametroch v requeste
	 *
	 *@param  request
	 *@return
	 */
	public static List<CalendarDetails> getEvents(HttpServletRequest request)
	{
		List<CalendarDetails> events = new ArrayList<>();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			//ziskaj udaje z db
			db_conn = DBPool.getConnection(request);
			StringBuilder sql = new StringBuilder("SELECT c.*, ct.name FROM calendar c, calendar_types ct WHERE c.type_id=ct.type_id AND date_to >= ?"+CloudToolsForCore.getDomainIdSqlWhere(true, "c"));

			long now = (new java.util.Date()).getTime();
			now = now - 86400000;
			long end = 0;

			if (getParamAttribute("calAllEvents", request) != null)
			{
				//chce vsetky zaznamy uplne od zaciatku
				now = 1000;
			}
			if (getParamAttribute("calStart", request) != null)
			{
				//chce zaznamy od datumu
				now = DB.getTimestamp(getParamAttribute("calStart", request), "0:00", DBPool.getDBName(request));
			}
			if (getParamAttribute("calEnd", request) != null)
			{
				//chce zaznamy od datumu
				end = DB.getTimestamp(getParamAttribute("calEnd", request), "23:59", DBPool.getDBName(request));
				sql.append(" AND date_from <= ? ");
			}

			int i;
			i = getParamAttributeInt("calTypeId", request);
			if (i > 0)
			{
				sql.append(" AND c.type_id = ?");
			}
			String typNazvy = getParamAttribute("typyNazvy", request);
			StringBuilder typyInSQL = new StringBuilder(" AND ct.name IN (");
			List<String> typyIn = null;
			if (Tools.isNotEmpty(typNazvy))
			{
				StringTokenizer st = new StringTokenizer(typNazvy, ",+");
				while (st.hasMoreTokens())
				{
					String nazov = DB.removeSlashes(st.nextToken());
					if (Tools.isNotEmpty(nazov))
					{
						if (typyIn == null)
						{
							typyIn = new ArrayList<>();
							typyIn.add(nazov);
							typyInSQL.append("?");
						}
						else
						{
							typyIn.add(nazov);	//pripravim si hodnoty do IN
							typyInSQL.append(",?");
						}
					}
				}
				typyInSQL.append(")");
				if (typyIn!=null && typyIn.size() > 0)
				{
					sql.append(typyInSQL);	//pridam IN SQL
				}
			}
			if(request.getAttribute("showApprove") != null)
			{
				sql.append(" AND approve = ?");
			}

			if(request.getAttribute("suggest") != null)
			{
				sql.append(" AND suggest = ?");
			}
			sql.append(" ORDER BY date_from");
			Logger.debug(CalendarDB.class, "sql="+sql.toString());
			int psIndex = 1;
			ps = db_conn.prepareStatement(sql.toString());
			ps.setTimestamp(psIndex++, new Timestamp(now));
			if (end > 0) ps.setTimestamp(psIndex++, new Timestamp(end));
			Logger.debug(CalendarDB.class, "date_to: " + new Timestamp(end));
			if(i > 0)
			{
				ps.setInt(psIndex++, i);
			}
			if (typyIn != null && typyIn.size() > 0)
			{
				for(String s: typyIn)
				{
					ps.setString(psIndex++, s);	//postupne nastavim hodnoty do IN
				}
			}
			if(request.getAttribute("showApprove") != null)
			{
				int showApprove = "1".equals(getParamAttribute("showApprove", request)) ? 1 : 0;
				ps.setInt(psIndex++, showApprove);
			}
			if(request.getAttribute("suggest") != null)
				ps.setBoolean(psIndex++, (Boolean)request.getAttribute("suggest"));
			//if (Tools.isNotEmpty(typyIn)) ps.setString(psIndex++, typyIn);
			rs = ps.executeQuery();
			CalendarDetails cal;
			while (rs.next())
			{
				cal = new CalendarDetails();
				cal.setCalendarId(rs.getInt("calendar_id"));
				cal.setTitle(DB.getDbString(rs, "title"));
				cal.setDescription(DB.getDbString(rs, "description"));

				try
				{
					//ak je tam len cislo, je to docId
					if (cal.getDescription().length() > 0 && cal.getDescription().length() < 4)
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
				events.add(cal);
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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		return (events);
	}

	public static CalendarDetails getEvent(int calendarId, HttpServletRequest request)
	{
		return getEvent(calendarId);
	}

	/**
	 *  Ziska zaznam v kalendari
	 *
	 *@param  calendarId
	 *@return
	 */
	public static CalendarDetails getEvent(int calendarId)
	{
		CalendarDetails cal = null;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			//ziskaj udaje z db
			db_conn = DBPool.getConnection();
			String sql = "SELECT c.*, ct.name FROM calendar c, calendar_types ct WHERE c.type_id=ct.type_id AND c.calendar_id=?"+CloudToolsForCore.getDomainIdSqlWhere(true, "c");

			ps = db_conn.prepareStatement(sql);
			ps.setInt(1, calendarId);

			rs = ps.executeQuery();
			if (rs.next())
			{
				cal = new CalendarDetails();
				cal.setCalendarId(rs.getInt("calendar_id"));
				cal.setTitle(DB.getDbString(rs, "title"));
				cal.setDescription(DB.getDbString(rs, "description"));

				try
				{
					//ak je tam len cislo, je to docId
					if (cal.getDescription().length() < 6)
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
				cal.setHashString(DB.getDbString(rs, "hash_string"));
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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		return (cal);
	}

	/**
	 * Toto sa vola z crontabu kazdych 10 minut
	 * @param args
	 */
	public static void main(String[] args)
	{
		//Logger.println(CalendarDB.class,"CalendarDB.sendNotify()");
		if(args != null && args.length > 0 && Tools.getIntValue(args[0], 3) == 2)
		{
			String serverName = "";
			if(args.length > 1)
				serverName = args[1];
			sendListEventsNotify(serverName);
		}else
			CalendarDB.sendNotify();
	}

	/**
	 *  Posle notifikaciu emailom
	 */
	public static void sendNotify()
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		List<CalendarDetails> events = new ArrayList<>();
		Calendar calendar = Calendar.getInstance();

		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT c.*, ct.name FROM calendar c, calendar_types ct WHERE c.notify_hours_before<>0 AND c.type_id=ct.type_id AND notify_sent=?"+CloudToolsForCore.getDomainIdSqlWhere(true, "c"));
			ps.setBoolean(1, false);
			rs = ps.executeQuery();
			while (rs.next())
			{
				CalendarDetails cal = new CalendarDetails();
				cal.setCalendarId(rs.getInt("calendar_id"));
				cal.setTitle(DB.getDbString(rs, "title"));
				cal.setDescription(DB.getDbString(rs, "description"));

				try
				{
					//ak je tam len cislo, je to docId
					if (cal.getDescription().length() > 0 && cal.getDescription().length() < 4)
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

				Logger.debug(CalendarDB.class, "sendNotify: testing id="+cal.getCalendarId());

				if (cal.getNotifyHoursBefore() != 0 && cal.getNotifyEmails().length() > 1 && cal.getNotifySender().length() > 1)
				{
					calendar.setTime(new java.util.Date(cal.getFrom().getTime()));
					calendar.add(Calendar.HOUR_OF_DAY, -cal.getNotifyHoursBefore());
					//?? je to v minulosti?

					Logger.debug(CalendarDB.class, "sendNotify: time="+Tools.formatDateTime(calendar.getTime().getTime()));

					if (calendar.getTime().getTime() < Tools.getNow())
					{
						Logger.debug(CalendarDB.class, "adding: " + cal.getTitle());
						events.add(cal);
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

		//preiteruj zaznamy a pre kazdy posli notify
		for (CalendarDetails cal : events)
		{
			sendNotifyMail(cal);
		}
	}

	/**
	 *  Description of the Method
	 *
	 *@param  cal  Description of the Parameter
	 *@return      Description of the Return Value
	 */
	private static boolean sendNotifyMail(CalendarDetails cal)
	{
		Logger.debug(CalendarDB.class, "sendNotifyEmail: hb="+cal.getNotifyHoursBefore()+" ne="+cal.getNotifyEmails()+" se="+cal.getNotifySender());

		if (cal.getNotifyHoursBefore() != 0 && cal.getNotifyEmails().length() > 1 && cal.getNotifySender().length() > 1)
		{
			Prop prop = Prop.getInstance(Constants.getServletContext(), cal.getLng(), false);
			String subject = cal.getTitle();

			StringBuilder body= new StringBuilder("<html><head><style>");

			try
			{
				//nacitaj css styl
				InputStream is = Constants.getServletContext().getResourceAsStream("/css/email.css");
				if (is==null)
				{
					is = Constants.getServletContext().getResourceAsStream(Constants.getString("editorPageCss"));
				}
				if (is!=null)
				{
					BufferedReader br = new BufferedReader(new InputStreamReader(is, Constants.FILE_ENCODING));
					String line;
					while ((line=br.readLine())!=null)
					{
						body.append(line).append('\n');
					}
					br.close();
				}
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}

			body.append("</style></head><body>");

			body.append(cal.getNotifyIntrotext());

			body.append("<table>\n");

			body.append( "<tr>");
			body.append( "   <td valign='top' nowrap><b>").append(prop.getText("calendar_edit.title")).append(":&nbsp;&nbsp;&nbsp;</b></td>");
			body.append( "   <td>").append(cal.getTitle()).append("</td>");
			body.append( "</tr>\n");

			body.append( "<tr>");
			body.append( "   <td valign='top' nowrap><b>").append(prop.getText("calendar_edit.begin")).append(":&nbsp;&nbsp;&nbsp;</b></td>");
			body.append( "   <td>").append(cal.getFromString()).append(' ').append(cal.getTimeRange()).append("</td>");
			body.append( "</tr>\n");

			body.append( "<tr>");
			body.append( "   <td valign='top' nowrap><b>").append(prop.getText("calendar_edit.description")).append(":&nbsp;&nbsp;&nbsp;</b></td>");
			body.append( "<td>").append(cal.getDescription()).append( "</td>");
			body.append( "</tr>\n");

			body.append( "</table></body></html>");

			StringBuilder smsBody = new StringBuilder(cal.getNotifyIntrotext()).append(' ');
			smsBody.append(prop.getText("calendar_edit.begin")).append(": ").append(cal.getFromString());
			if (Tools.isEmpty(cal.getTimeRange())==false)
			{
				smsBody.append(" ").append(cal.getTimeRange());
			}
			smsBody.append(prop.getText("calendar_edit.title")).append(": ").append(cal.getTitle());

			StringTokenizer st = new StringTokenizer(cal.getNotifyEmails(), ",");
			String toEmail;
			String fromName = getName(cal.getNotifySender());
			String fromEmail = getEmail(cal.getNotifySender());
			UserGroupsDB userGroupsDB = UserGroupsDB.getInstance();
			UserGroupDetails userGroupDetails;
			Map<String, String> allreadySent = new Hashtable<>();
			while (st.hasMoreTokens())
			{
				toEmail = st.nextToken().trim();

				Logger.debug(CalendarDB.class, "sendNotifyEmail: sending to: "+toEmail);

				userGroupDetails = userGroupsDB.getUserGroup(toEmail);
				if (toEmail.indexOf('@') != -1 && userGroupDetails==null)
				{
					if (allreadySent.get(toEmail)==null)
					{
						SendMail.send(fromName, fromEmail, toEmail, subject, body.toString());

						allreadySent.put(toEmail, toEmail);
					}
				}
				else
				{
					//je to asi meno skupiny, ktorym to ma ist
					if (userGroupDetails != null)
					{
						//nacitaj zoznam ludi z danej skupiny
						for (UserDetails usr : UsersDB.getUsersByGroup(userGroupDetails.getUserGroupId()))
						{
							toEmail = usr.getEmail();
							if (toEmail.indexOf('@') != -1 && allreadySent.get(toEmail)==null)
							{
									Logger.println(CalendarDB.class,"SENDING TO BY GROUP: " + toEmail + " " + usr.getFullName());
									SendMail.send(fromName, fromEmail, toEmail, subject, body.toString());
									allreadySent.put(toEmail, toEmail);
							}
						}
					}
				}
			}

			//oznac zaznam za odoslany
			Connection db_conn = null;
			PreparedStatement ps = null;
			try
			{
				db_conn = DBPool.getConnection();
				ps = db_conn.prepareStatement("UPDATE calendar SET notify_sent=? WHERE calendar_id=? "+CloudToolsForCore.getDomainIdSqlWhere(true));
				ps.setBoolean(1, true);
				ps.setInt(2, cal.getCalendarId());
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
		return (false);
	}

	/**
	 *  Vrati meno zo stringu typu Peter Weber &lt;peter@domain.sk&gt;
	 *
	 *@param  from  Description of the Parameter
	 *@return       The email value
	 */
	public static String getEmail(String from)
	{
		//OK todo: checek ci from neobsahuje nieco ako: "aaa<b>4444</b>bbb" <miba@interway.sk>

		try
		{
			if (from == null)
			{
				return ("");
			}
			int begin = from.lastIndexOf('<');
			int end = from.lastIndexOf('>');

			if (begin < 0)
			{
				return (from);
			}
			if (end < begin)
			{
				return (from);
			}

			return (from.substring(begin + 1, end).trim());
		}
		catch (Exception ex)
		{
			return from;
		}
	}

	/**
	 *  Vrati email zo stringu typu Peter Weber &lt;peter@domain.sk&gt;
	 *
	 *@param  from  Description of the Parameter
	 *@return       The name value
	 */
	public static String getName(String from)
	{
		try
		{
			if (from == null)
			{
				return ("");
			}
			int begin = from.indexOf('<');
			if (begin < 0)
			{
				return (from);
			}

			return (from.substring(0, begin).trim());
		}
		catch (Exception ex)
		{
			return from;
		}
	}

	/**
	 * Vrati meniny pre dany jazyk a dnovy posun
	 * @param lng - jazyk
	 * @param offset - pocet dni posunu
	 * @return
	 */
	public static String getMeniny(String lng, int offset)
	{
		String name = "";

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			Calendar cal = Calendar.getInstance();
			if (offset != 0)
			{
				cal.add(Calendar.DAY_OF_YEAR, offset);
			}

			//Logger.println(this,"meniny: " + cal.get(Calendar.DAY_OF_MONTH) + "."+(cal.get(Calendar.MONTH) + 1)+" ["+lng);

			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT * FROM calendar_name_in_year WHERE lng=? AND day=? AND month=?");
			ps.setString(1, lng);
			ps.setInt(2, cal.get(Calendar.DAY_OF_MONTH));
			ps.setInt(3, cal.get(Calendar.MONTH) + 1);
			rs = ps.executeQuery();
			if (rs.next())
			{
				name = DB.getDbString(rs, "name");
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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		return(name);
	}

	/**
	 * Vrati zoznam pozvanok so stavmi pre zadany zaznam kalendara
	 * @param calendarId
	 * @return
	 */
	public static List<CalendarInvitationDetails> getInvitations(int calendarId)
	{
		List<CalendarInvitationDetails> invitations = new ArrayList<>();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			String sql = "SELECT u.*, i.calendar_invitation_id, i.calendar_id, i.sent_date, i.status_date, i.status FROM users u, calendar_invitation i WHERE u.user_id=i.user_id AND i.calendar_id=? "+CloudToolsForCore.getDomainIdSqlWhere(true)+" ORDER BY last_name";

			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql);
			ps.setInt(1, calendarId);
			rs = ps.executeQuery();
			while (rs.next())
			{
				CalendarInvitationDetails inv = new CalendarInvitationDetails();
				fillCalendarInvitation(rs, inv);
				invitations.add(inv);
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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		return(invitations);
	}

	/**
	 * Vrati pozvanku so zadanym ID
	 * @param calendarInvitationId
	 * @return
	 */
	public static CalendarInvitationDetails getInvitation(int calendarInvitationId)
	{
		CalendarInvitationDetails inv = null;
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			String sql = "SELECT u.*, i.calendar_invitation_id, i.calendar_id, i.sent_date, i.status_date, i.status FROM users u, calendar_invitation i WHERE u.user_id=i.user_id AND i.calendar_invitation_id=?"+CloudToolsForCore.getDomainIdSqlWhere(true);

			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql);
			ps.setInt(1, calendarInvitationId);
			rs = ps.executeQuery();
			while (rs.next())
			{
				inv = new CalendarInvitationDetails();
				fillCalendarInvitation(rs, inv);
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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		return(inv);
	}

	/**
	 * Naplni CalendarInvitationDetails z resultsetu
	 * @param rs
	 * @param inv
	 */
	private static void fillCalendarInvitation(ResultSet rs, CalendarInvitationDetails inv) throws SQLException
	{
		inv.setCalendarInvitationId(rs.getInt("calendar_invitation_id"));
		inv.setCalendarId(rs.getInt("calendar_id"));
		inv.setUserId(rs.getInt("user_id"));
		Timestamp t = rs.getTimestamp("sent_date");
		if (t!=null) inv.setSentDate(new java.util.Date(t.getTime()));
		t = rs.getTimestamp("status_date");
		if (t!=null) inv.setStatusDate(new java.util.Date(t.getTime()));
		inv.setStatus(DB.getDbString(rs, "status"));

		//napln user details
		try
		{
			inv.setUser(new UserDetails(rs));
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
	}

	/**
	 * Aktualizuje stav pozvanky, status je char(1) a mal by mat hodnoty:
	 * -=nenastavene
	 * A=accepted
	 * D=declined
	 * T=tentative
	 * @param calendarInvitationId
	 * @param status
	 */
	public static boolean setCalendarInvitationStatus(int calendarInvitationId, String status)
	{
		boolean saveOK = false;
		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("UPDATE calendar_invitation SET status_date=?, status=? WHERE calendar_invitation_id=? "+CloudToolsForCore.getDomainIdSqlWhere(true));
			ps.setTimestamp(1, new Timestamp(Tools.getNow()));
			ps.setString(2, status);
			ps.setInt(3, calendarInvitationId);
			ps.execute();
			ps.close();

			saveOK = true;

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
		return(saveOK);
	}

	/**
	 * vrati pozvanku podla uzivatela
	 * @param calendarId
	 * @param userId
	 * @return
	 */
	public static CalendarInvitationDetails getInvitationByUser(int calendarId, int userId)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		CalendarInvitationDetails inv = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT * FROM calendar_invitation WHERE calendar_id = ? AND user_id=?"+CloudToolsForCore.getDomainIdSqlWhere(true));
			ps.setInt(1, calendarId);
			ps.setInt(2, userId);
			rs = ps.executeQuery();
			if (rs.next())
			{
				inv = new CalendarInvitationDetails();
				inv.setCalendarInvitationId(rs.getInt("calendar_invitation_id"));
				inv.setCalendarId(rs.getInt("calendar_id"));
				inv.setUserId(rs.getInt("user_id"));
				Timestamp t = rs.getTimestamp("sent_date");
				if (t!=null) inv.setSentDate(new java.util.Date(t.getTime()));
				t = rs.getTimestamp("status_date");
				if (t!=null) inv.setStatusDate(new java.util.Date(t.getTime()));
				inv.setStatus(DB.getDbString(rs, "status"));
				inv.setUser(UsersDB.getUser(userId));
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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		return inv;
	}

	/**
	 * vrati vsetky akceptovane pozvanky na zaklade calendarId
	 * @param calendarId
	 * @return
	 */
	public static List<CalendarInvitationDetails> getAllAcceptedInvitations(int calendarId)
	{
		List<CalendarInvitationDetails> alCalNew = new ArrayList<>();
		List<CalendarInvitationDetails> alCal = getInvitations(calendarId);
		if(alCal.size() > 0)
		{
			for (int i = 0; i < alCal.size(); i++)
			{
				CalendarInvitationDetails inv = alCal.get(i);
				if(inv.getStatus().equals("A"))
					alCalNew.add(inv);
			}
		}

		return alCalNew;
	}

	/**
	 * ulozi pozvanku do DB (bez poslania mailu)
	 * @param calendarId
	 * @param userId
	 * @return
	 */
	public static boolean saveCalendarInvitation(int calendarId, int userId)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("INSERT INTO calendar_invitation (calendar_id, user_id, sent_date, status, domain_id) VALUES (?, ?, ?, ?, ?)");
			int psIndex = 1;
			ps.setInt(psIndex++, calendarId);
			ps.setInt(psIndex++, userId);
			ps.setTimestamp(psIndex++, new Timestamp(Tools.getNow()));
			ps.setString(psIndex++, "-");
			ps.setInt(psIndex++, CloudToolsForCore.getDomainId());
			ps.execute();
			ps.close();
			db_conn.close();
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
			return false;
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

		return true;
	}

	/**
	 * napni CalendarDetails
	 * @param rs
	 * @return
	 */
	private static CalendarDetails fillCalendarDetails(ResultSet rs)
	{
		CalendarDetails cal = new CalendarDetails();
		try
		{
			cal.setCalendarId(rs.getInt("calendar_id"));
			cal.setTitle(DB.getDbString(rs, "title"));
			cal.setDescription(DB.getDbString(rs, "description"));

			try
			{
				//ak je tam len cislo, je to docId
				if (cal.getDescription().length() > 0 && cal.getDescription().length() < 4)
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
	 * vrati vsetky neschvalene udalosti ktore ma user s userId schvalit
	 * @param userId
	 * @return
	 */
	public static List<CalendarDetails> getNotApprovedEvents(int userId)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		List<CalendarDetails> result = new ArrayList<>();
		try
		{
			Map<Integer, EventTypeDetails> eventTypes = EventTypeDB.getTypeBySchvalovatelId(userId);
			if(eventTypes.size() < 1)
				return result;
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT c.*, ct.name FROM calendar c, calendar_types ct WHERE c.type_id=ct.type_id AND (c.approve = ? OR c.approve = ?)"+CloudToolsForCore.getDomainIdSqlWhere(true, "c"));
			ps.setInt(1, -1);
			ps.setInt(2, 0);
			rs = ps.executeQuery();
			while (rs.next())
			{
				int typeId = rs.getInt("type_id");
				if(eventTypes.containsKey(Integer.valueOf(typeId)))
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

	/**
	 * vrati vsetky uzivatelove udalosti
	 * @param userId
	 * @return
	 */
	public static List<CalendarDetails> getEventsByUserId(int userId)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		List<CalendarDetails> result = new ArrayList<>();
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT c.*, ct.name FROM calendar c, calendar_types ct WHERE c.type_id=ct.type_id AND c.creator_id = ?"+CloudToolsForCore.getDomainIdSqlWhere(true, "c"));
			ps.setInt(1, userId);
			rs = ps.executeQuery();
			while (rs.next())
			{
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

	public static boolean saveEventToDB(EventForm event)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			Prop prop = Prop.getInstance(Constants.getServletContext(), event.getLng(), false);
			db_conn = DBPool.getConnection();
			String sql = "INSERT INTO calendar (title, date_from, date_to, type_id, description, time_range, area, city, address, info_1, info_2, info_3, info_4, info_5, notify_hours_before, notify_emails, notify_sender, notify_introtext, notify_sendsms, lng, creator_id, approve, suggest, domain_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			ps = db_conn.prepareStatement(sql);
			DB.setClob(ps, 1, event.getTitle());
			if (event.getDateFrom().length()>12)
			{
				ps.setTimestamp(2, new Timestamp(DB.getTimestamp(event.getDateFrom())));
			}
			else
			{
				ps.setDate(2, new java.sql.Date(DB.getTimestamp(event.getDateFrom(), "01:01")));
			}
			if (event.getDateTo().length()>12)
			{
				ps.setTimestamp(3, new Timestamp(DB.getTimestamp(event.getDateTo())));
			}
			else
			{
				ps.setDate(3, new java.sql.Date(DB.getTimestamp(event.getDateTo(), "23:59")));
			}
			ps.setInt(4, event.getTypeId());
			DB.setClob(ps, 5, event.getDescription());
			ps.setString(6, event.getTimeRange());
			ps.setString(7, event.getArea());
			ps.setString(8, event.getCity());
			ps.setString(9, event.getAddress());
			ps.setString(10, event.getInfo1());
			ps.setString(11, event.getInfo2());
			ps.setString(12, event.getInfo3());
			ps.setString(13, event.getInfo4());
			ps.setString(14, event.getInfo5());
			ps.setInt(15, event.getNotifyHoursBefore());
			ps.setString(16, event.getNotifyEmails());
			ps.setString(17, event.getNotifySender());
			DB.setClob(ps, 18, event.getNotifyIntrotext());
			ps.setBoolean(19, event.isNotifySendSMS());
			ps.setString(20, event.getLng());
			ps.setInt(21, event.getCreatorId());
			ps.setInt(22, event.getApprove());
			ps.setBoolean(23, false);
			ps.setInt(24, CloudToolsForCore.getDomainId());
			ps.execute();
			ps.close();

			if (event.getCalendarId()<1)
			{
				//ziskaj ID z db
				ps = db_conn.prepareStatement("SELECT max(calendar_id) as calendar_id FROM calendar WHERE title=?"+CloudToolsForCore.getDomainIdSqlWhere(true));
				ps.setString(1, event.getTitle());
				ResultSet rs = ps.executeQuery();
				if (rs.next())
				{
					event.setCalendarId(rs.getInt("calendar_id"));
				}
				rs.close();
				ps.close();
			}

			db_conn.close();
			ps = null;
			db_conn = null;


			// v pripade ze treba dat schvalit udalost posli mail schvalovatelovi
			UserDetails creator = UsersDB.getUser(event.getCreatorId());
			if(creator != null)
			{
				EventTypeDetails eventType = EventTypeDB.getTypeById(event.getTypeId());
				// posli iba v pripade, ze treba schvalit
				if(event.getApprove() == -1)
				{
					UserDetails schvalovatel = UsersDB.getUser(eventType.getSchvalovatelId());
					if(eventType == null || schvalovatel == null)
						return false;
					//pokial schvalovatel a tvorca akcie je ten isty, tak nastav akciu za schvalenu
					if(schvalovatel.getUserId() == creator.getUserId())
					{
						boolean saveOk = CalendarDB.saveApproveStatus(event.getCalendarId(), 1, false);
						if(saveOk == false)
							return false;
					}
					else
					{
						String datumAkcie = "";
						if(Tools.isNotEmpty(event.getDateFrom()))
							datumAkcie = event.getDateFrom();
						if(Tools.isNotEmpty(event.getDateTo()))
							datumAkcie = datumAkcie.concat(" - "+event.getDateTo());
						if(Tools.isNotEmpty(event.getTimeRange()))
							datumAkcie = datumAkcie.concat(" o "+event.getTimeRange());
						StringBuilder emailClientData=new StringBuilder();
						emailClientData.append("<html><head>");
						emailClientData.append("<style>");
						emailClientData.append("body{");
						emailClientData.append("font-family: Arial;");
						emailClientData.append("font-size: 11pt;");
						emailClientData.append('}');
						emailClientData.append("</style></head><body>");
						emailClientData.append(prop.getText("calendar.vytvoril_akciu", creator.getFullName(), event.getTitle(), datumAkcie));
						emailClientData.append("</body></html>");
						boolean saveOk = SendMail.send(creator.getFullName(), creator.getEmailAddress(), schvalovatel.getEmailAddress(), prop.getText("calendar.na_schvalenie"), emailClientData.toString());
						if(saveOk == false)
							return false;
					}
				}
			}else
				return false;
			//****************************************
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
			return false;
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

		return true;
	}

	/**
	 * nastavi status udalosti o jej schvaleni/neschvaleni
	 * @param eventId
	 * @param status
	 * @param sendEmail - ak je false, tak sa neposle mail o schvaleni/meschvaleni udalosti
	 * @return
	 */
	public static boolean saveApproveStatus(int eventId, int status, boolean sendEmail)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("UPDATE calendar SET approve = ? WHERE calendar_id = ? "+CloudToolsForCore.getDomainIdSqlWhere(true));
			ps.setInt(1, status);
			ps.setInt(2, eventId);
			ps.execute();
			ps.close();
			db_conn.close();
			ps = null;
			db_conn = null;

			//poslat mail tvorcovi udalosti o statuse
			CalendarDetails event = CalendarDB.getEvent(eventId);
			UserDetails creator = UsersDB.getUser(event.getCreatorId());
			if(event != null && creator != null)
			{
				EventTypeDetails eventType = EventTypeDB.getTypeById(event.getTypeId());
				UserDetails schvalovatel = UsersDB.getUser(eventType.getSchvalovatelId());
				Prop prop = Prop.getInstance(Constants.getServletContext(), event.getLng(), false);
				if(eventType == null || schvalovatel == null)
					return false;
				String statusString = "";
				if(event.getApprove() == 1)
					statusString = prop.getText("calendar.schvalena");
				else if(event.getApprove() == 0)
					statusString = prop.getText("calendar.neschvalena");

				String datumAkcie = "";
				if(Tools.isNotEmpty(event.getFromString()))
					datumAkcie = event.getFromString();
				if(Tools.isNotEmpty(event.getToString()) && event.getToString().trim().equals(event.getFromString().trim()) == false)
					datumAkcie = datumAkcie.concat(" - "+event.getToString());
				if(Tools.isNotEmpty(event.getTimeRange()))
					datumAkcie = datumAkcie.concat(" o "+event.getTimeRange());
				StringBuilder emailClientData=new StringBuilder("<html><head>");
				emailClientData.append("<style>");
				emailClientData.append("body{");
				emailClientData.append("font-family: Arial;");
				emailClientData.append("font-size: 11pt;");
				emailClientData.append('}');
				emailClientData.append("</style></head><body>");
				emailClientData.append(prop.getText("calendar.approve_email_body",event.getTitle(),datumAkcie,statusString)+" "+schvalovatel.getFullName());
				emailClientData.append("</body></html>");
				if(sendEmail)
				{
					boolean sendOK = SendMail.send(schvalovatel.getFullName(), schvalovatel.getEmailAddress(), creator.getEmailAddress(), prop.getText("calendar.approve_email_subject",statusString), emailClientData.toString());
					if(sendOK == false)
						return false;
				}
			}else
				return false;
			//****************************************
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
			return false;
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

		return true;
	}

	/**
	 * vrati vsetky udalosti od-do(dateFrom-dateTo) ktorych sa zucastni/nezucasti uzival userId
	 * @param userId
	 * @param dateTo
	 * @param dateFrom
	 * @param status - 'A'/'D'- vrati udalosti kde uzivatel potvrdil/zamietol ucast, '-' - vrati vsetky udalosti
	 * @return
	 */
	public static List<CalendarDetails> getEventsByUserIdDateStatus(int userId, String dateFrom, String dateTo, String status)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		List<CalendarDetails> result = new ArrayList<>();
		try
		{
			db_conn = DBPool.getConnection();
			StringBuilder sql = new StringBuilder("SELECT c.*, ct.name FROM calendar c,calendar_invitation ci,calendar_types ct WHERE c.calendar_id=ci.calendar_id AND c.type_id=ct.type_id AND approve = 1 AND  ci.user_id=?"+CloudToolsForCore.getDomainIdSqlWhere(true, "c"));
			if(Tools.isNotEmpty(dateFrom))
				sql.append(" AND c.date_to >= ?");
			if(Tools.isNotEmpty(dateTo))
				sql.append(" AND c.date_from <= ?");
			if (status.equals("A") || status.equals("D"))
				sql.append(" AND ci.status=?");
			ps = db_conn.prepareStatement(sql.toString());
			int ind = 1;
			ps.setInt(ind++, userId);
			if(Tools.isNotEmpty(dateFrom))
				ps.setTimestamp(ind++, new Timestamp(DB.getTimestamp(dateFrom, "00:00")));
			if(Tools.isNotEmpty(dateTo))
				ps.setTimestamp(ind++, new Timestamp(DB.getTimestamp(dateTo, "23:59")));
			if (status.equals("A") || status.equals("D"))
				ps.setString(ind++, status);
			rs = ps.executeQuery();
			while (rs.next())
			{
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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		return result;
	}

	/**
	 * vrati akcie podla urcitych regionov a podla typu akcie
	 * @param request
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<CalendarDetails> getEventsByRegionAndType(HttpServletRequest request, int[] eventTypes, String region)
	{
		Cache c = Cache.getInstance();
		PageParams pageParams = new PageParams(request);
		String cacheKey = "sk.iway.iwcm.calendar.CalendarDB.allEvents";
		int cacheInMinutes = pageParams.getIntValue("cacheInMinutes", 5);
		String calStart = (String)request.getAttribute("calStart");
		String calEnd = (String)request.getAttribute("calEnd");

		List<CalendarDetails> allEvents = null;
		//cachujem, len vsetky udalosti
		if(Tools.isNotEmpty(calStart) && Tools.isEmpty(calEnd) && Tools.formatDate(Tools.getNow()).equals(calStart))
		{
			allEvents = (List<CalendarDetails>)c.getObject(cacheKey);
			if (allEvents == null || allEvents.size() < 1)
			{
				allEvents = getEvents(request);
			   c.setObjectSeconds(cacheKey, allEvents , cacheInMinutes*60, true);
			}
		}
		else
		{
			allEvents = getEvents(request);
		}
		List<CalendarDetails> result = new ArrayList<>();
		Identity user = UsersDB.getCurrentUser(request);
		if (user == null)
			return allEvents;
		Map<Integer, EventTypeDetails> htCheckedRegions = new Hashtable<>();
		for(int i=0; i < eventTypes.length;i++)
		{
			EventTypeDetails eventType = EventTypeDB.getTypeById(eventTypes[i]);
			htCheckedRegions.put(Integer.valueOf(eventType.getTypeId()), eventType);
		}
		//tried podla typu udalosti
		if(htCheckedRegions != null && htCheckedRegions.size() > 0)
		{
			for (CalendarDetails calendarDetails : allEvents)
			{
				if(htCheckedRegions.containsKey(calendarDetails.getTypeId()))
				{
					//tried podla regionu ak je nastaveny
					if(region != null && region.equals("-1") == false && Tools.isNotEmpty(calendarDetails.getArea()))
					{
						if(region.equals(calendarDetails.getArea()))
							result.add(calendarDetails);
					}else
					{
						result.add(calendarDetails);
					}
				}
			}
		}
		else
			return allEvents;

		return result;
	}

	/**
	 * vrati akcie podla urcitych regionov a podla typu akcie, ktore ma uzivatel nastavene
	 * @param request
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<CalendarDetails> getEventsByRegionAndType(HttpServletRequest request, int userId)
	{
		Cache c = Cache.getInstance();
		PageParams pageParams = new PageParams(request);
		String cacheKey = "sk.iway.iwcm.calendar.CalendarDB.allEvents";
		int cacheInMinutes = pageParams.getIntValue("cacheInMinutes", 5);
		String calStart = (String)request.getAttribute("calStart");
		String calEnd = (String)request.getAttribute("calEnd");

		List<CalendarDetails> allEvents = null;
		//cachujem, len vsetky udalosti
		if(Tools.isNotEmpty(calStart) && Tools.isEmpty(calEnd) && Tools.formatDate(Tools.getNow()).equals(calStart))
		{
			allEvents = (List<CalendarDetails>)c.getObject(cacheKey);
			if (allEvents == null || allEvents.size() < 1)
			{
				allEvents = getEvents(request);
			   c.setObjectSeconds(cacheKey, allEvents, cacheInMinutes*60, true);
			}
		}
		else
		{
			allEvents = getEvents(request);
		}

		List<CalendarDetails> result = new ArrayList<>();
		UserDetails user = UsersDB.getUser(userId);
		if (user == null)
			return allEvents;
		List<EventTypeDetails> eventTypes = EventTypeDB.getTypes();
		Map<Integer, EventTypeDetails> htCheckedRegions = new Hashtable<>();
		for(int i=0; i < eventTypes.size();i++)
		{
			EventTypeDetails eventType = eventTypes.get(i);
			SettingsBean sbPerms = user.getSettings().get("kaOblast"+eventType.getTypeId());
			if(sbPerms != null && sbPerms.getSvalue1().equals("true"))
			{
				htCheckedRegions.put(Integer.valueOf(eventType.getTypeId()), eventType);
			}
		}
		//tried podla typu udalosti
		if(htCheckedRegions != null && htCheckedRegions.size() > 0)
		{
			for (CalendarDetails calendarDetails : allEvents)
			{
				if(htCheckedRegions.containsKey(calendarDetails.getTypeId()))
				{
					//tried podla regionu ak je nastaveny
					SettingsBean sbPerms2 = user.getSettings().get("kaRegion");
					if(sbPerms2 != null && sbPerms2.getSvalue1().equals("-1") == false && Tools.isNotEmpty(calendarDetails.getArea()))
					{
						if(sbPerms2.getSvalue1().equals(calendarDetails.getArea()))
							result.add(calendarDetails);
					}else
					{
						result.add(calendarDetails);
					}
				}
			}
		}
		else
			return allEvents;

		return result;
	}

	/**
	 * vrati akcie podla urcitych regionov a podla typu akcie, ktore ma uzivatel nastavene
	 * @param request
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<CalendarDetails> getEventsByRegionAndType(HttpServletRequest request)
	{
		Cache c = Cache.getInstance();
		PageParams pageParams = new PageParams(request);
		String cacheKey = "sk.iway.iwcm.calendar.CalendarDB.allEvents";
		int cacheInMinutes = pageParams.getIntValue("cacheInMinutes", 5);
		String calStart = (String)request.getAttribute("calStart");
		String calEnd = (String)request.getAttribute("calEnd");

		List<CalendarDetails> allEvents = null;
		//cachujem, len vsetky udalosti
		if(Tools.isNotEmpty(calStart) && Tools.isEmpty(calEnd) && Tools.formatDate(Tools.getNow()).equals(calStart))
		{
			allEvents = (List<CalendarDetails>)c.getObject(cacheKey);
			if (allEvents == null || allEvents.size() < 1)
			{
				allEvents = getEvents(request);
			   c.setObjectSeconds(cacheKey, allEvents , cacheInMinutes*60, true);
			}
		}
		else
		{
			allEvents = getEvents(request);
		}

		List<CalendarDetails> result = new ArrayList<>();
		Identity user = UsersDB.getCurrentUser(request);
		if (user == null)
			return allEvents;

		List<EventTypeDetails> eventTypes = EventTypeDB.getTypes();

		Map<Integer, EventTypeDetails> htCheckedRegions = new Hashtable<>();
		for(int i=0; i < eventTypes.size();i++)
		{
			EventTypeDetails eventType = eventTypes.get(i);
			SettingsBean sbPerms = user.getSettings().get("kaOblast"+eventType.getTypeId());
			if(sbPerms != null && sbPerms.getSvalue1().equals("true"))
			{
				htCheckedRegions.put(Integer.valueOf(eventType.getTypeId()), eventType);
			}
		}
		//tried podla typu udalosti
		if(htCheckedRegions != null && htCheckedRegions.size() > 0)
		{
			for (CalendarDetails calendarDetails : allEvents)
			{
				if(htCheckedRegions.containsKey(calendarDetails.getTypeId()))
				{
					//tried podla regionu ak je nastaveny
					SettingsBean sbPerms2 = user.getSettings().get("kaRegion");
					if(sbPerms2 != null && sbPerms2.getSvalue1().equals("-1") == false && Tools.isNotEmpty(calendarDetails.getArea()))
					{
						if(sbPerms2.getSvalue1().equals(calendarDetails.getArea()))
							result.add(calendarDetails);
					}else
					{
						result.add(calendarDetails);
					}
				}
			}
		}
		else
			return allEvents;

		return result;
	}

	/**
	 * nastavi odporucane udalosti podla akcie
	 * @param odporucitAkcia
	 * @param odporuceneId
	 * @return
	 */
	public static boolean setSuggestEvents(int odporucitAkcia, String odporuceneId)
	{
		if(Tools.isEmpty(odporuceneId))
			return false;
		boolean odporucitAkciaDb = false;
		if(odporucitAkcia == 1)
			odporucitAkciaDb = true;
		else
			odporucitAkciaDb = false;
		String[] odporuceneIdDb = odporuceneId.split(",");
		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			db_conn = DBPool.getConnection();
			for (int i = 0; i < odporuceneIdDb.length; i++)
			{
				ps = db_conn.prepareStatement("UPDATE calendar SET suggest = ? WHERE calendar_id = ? "+CloudToolsForCore.getDomainIdSqlWhere(true));
				ps.setBoolean(1, odporucitAkciaDb);
				ps.setInt(2, Tools.getIntValue(odporuceneIdDb[i], -1));
				ps.execute();
				ps.close();
			}
			db_conn.close();
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
			return false;
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
		return true;
	}

	/**
	 * fulltext vyhladavanie v title,description
	 * @param vyraz
	 * @return
	 */
	public static List<CalendarDetails> fullTextSearch(String vyraz)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		List<CalendarDetails> result = new ArrayList<>();
		try
		{
			db_conn = DBPool.getConnection();
			if(Tools.isNotEmpty(vyraz))
			{
				if(vyraz.indexOf("AND") == -1 && vyraz.indexOf("OR") == -1)
				{
					vyraz = "\""+vyraz+"\"";
				}
				else if(vyraz.indexOf("AND") > -1 && vyraz.indexOf("OR") == -1)
				{
					String[] pom = vyraz.split("AND");
					vyraz = "\""+pom[0].trim()+"\""+" AND "+"\""+pom[1].trim()+"\"";
				}
				else if(vyraz.indexOf("OR") > -1 && vyraz.indexOf("AND") == -1)
				{
					String[] pom = vyraz.split("OR");
					vyraz = "\""+pom[0].trim()+"\""+" OR "+"\""+pom[1].trim()+"\"";
				}
				else if(vyraz.indexOf("AND") > -1 && vyraz.indexOf("OR") > -1)
				{
					int pomInd1 = vyraz.indexOf("AND");
					int pomInd2 = vyraz.indexOf(')');
					int pomInd3 = vyraz.indexOf("OR");

					String pom1 = vyraz.substring(1, pomInd1-1);
					String pom2 = vyraz.substring(pomInd1+3, pomInd2);
					String pom3 = vyraz.substring(pomInd3+3, vyraz.length());


					vyraz = "(\""+pom1.trim()+"\""+" AND "+"\""+pom2.trim()+"\") OR "+"\""+pom3+"\"";
				}
			}
			ps = db_conn.prepareStatement("SELECT c.*, ct.name FROM calendar c, calendar_types ct WHERE c.type_id=ct.type_id AND c.approve = ? AND c.date_to >= ? "+CloudToolsForCore.getDomainIdSqlWhere(true, "c")+" AND ((CONTAINS(c.title, ?) OR CONTAINS(c.description, ?)))");
			int ind = 1;
			ps.setInt(ind++, 1);
			ps.setTimestamp(ind++, new Timestamp(DB.getTimestamp(Tools.formatDate(Tools.getNow()), "00:00")));
			ps.setString(ind++, vyraz);
			ps.setString(ind++, vyraz);
			rs = ps.executeQuery();
			while (rs.next())
			{
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

	/**
	 * vrati udalosti pre notifikacie
	 * @param calStart
	 * @param calEnd
	 * @return
	 */
	public static List<CalendarDetails> getEventsForNotify(long calStart, long calEnd)
	{
		List<CalendarDetails> events = new ArrayList<>();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			//ziskaj udaje z db
			db_conn = DBPool.getConnection();
			StringBuilder sql = new StringBuilder("SELECT c.*, ct.name FROM calendar c, calendar_types ct WHERE c.type_id=ct.type_id AND date_from >= ?"+CloudToolsForCore.getDomainIdSqlWhere(true, "c"));

			long now = (new java.util.Date()).getTime();
			now = now - 86400000;
			long end = 0;

			if (calStart > 0)
			{
				//chce zaznamy od datumu
				now = DB.getTimestamp(Tools.formatDate(calStart), "00:00");
			}
			if (calEnd > 0)
			{
				//chce zaznamy od datumu
				end = DB.getTimestamp(Tools.formatDate(calEnd), "23:59");
				sql.append(" AND date_from <= ? ");
			}

			sql.append(" AND approve = 1");
			sql.append(" ORDER BY date_from");
			int psIndex = 1;
			ps = db_conn.prepareStatement(sql.toString());
			ps.setTimestamp(psIndex++, new Timestamp(now));
			if (end > 0) ps.setTimestamp(psIndex++, new Timestamp(end));
			rs = ps.executeQuery();
			CalendarDetails cal;
			while (rs.next())
			{
				cal = new CalendarDetails();
				cal.setCalendarId(rs.getInt("calendar_id"));
				cal.setTitle(DB.getDbString(rs, "title"));
				cal.setDescription(DB.getDbString(rs, "description"));

				try
				{
					//ak je tam len cislo, je to docId
					if (cal.getDescription().length() > 0 && cal.getDescription().length() < 4)
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
				events.add(cal);
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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		return (events);
	}


	/**
	 * posle mail so zoznamom udalosti podla notifikacnych nastaveni uzivatela
	 */
	public static void sendListEventsNotify(String serverName)
	{
		List<UserDetails> users = UsersDB.getUsers();
		String subject ="";
		String emptyBody ="";
		Prop prop = Prop.getInstance();
		if(users != null && users.size() > 0)
		{
			for(int i = 0; i < users.size(); i++)
			{
				UserDetails user = users.get(i);
				//okrem testovacieho usera
				if(user.getLogin().equals("VUBNETgrp") == false)
				{
					int showEvents = 2;
					Calendar datumOd = Calendar.getInstance();
					Calendar datumDo = Calendar.getInstance();
					datumOd.setTimeInMillis(DB.getTimestamp(Tools.formatDate(datumOd.getTimeInMillis())));
					datumDo.setTimeInMillis(DB.getTimestamp(Tools.formatDate(datumDo.getTimeInMillis())));
					Map<String, SettingsBean> userSetings = user.getSettings();
					if(userSetings == null || userSetings.size() < 0)
						continue;
					SettingsBean notif = userSetings.get("kaEmailoveNotifikacie");
					//ziskaj datum poslednej poslanej notifikacie
					SettingsBean notifDate = userSetings.get("kaEmailoveNotifikacieDate");
					if(notif == null)
						continue;
					String notifValue = notif.getSvalue1();
					if(notifValue.equals("vypnute"))
					{
						//odstranenie datumu poslednej poslanej notifikacie, lebo je to potrebne len pre tyzdenne notifikacie
						if(notifDate != null)
							userSetings.remove("kaEmailoveNotifikacieDate");
						UsersDB.setSettings(user.getUserId(), userSetings);
						continue;
					}
					else if(notifValue.equals("denne"))
					{
						if(notifDate != null)
							userSetings.remove("kaEmailoveNotifikacieDate");
						UsersDB.setSettings(user.getUserId(), userSetings);
						datumDo.add(Calendar.DATE, 1);
						subject=prop.getText("calendar.prehlad_48hodin");
						emptyBody =prop.getText("calendar.prehlad_ziadne_48hodin");
						showEvents = 2;
					}
					else if(notifValue.equals("tyzdenne"))
					{
						long notifDateValue = 0;
						if(notifDate != null && notifDate.getSdate() != null)
							notifDateValue = notifDate.getSdate().getTime();
						else//ak datum poslednej poslanej notifikacie neexistuje, tak nastav na aktualny datum
						{
							SettingsBean newSett = new SettingsBean();
							newSett.setSdate(new Timestamp(datumOd.getTimeInMillis()));
							newSett.setSvalue1("");
							newSett.setSvalue2("");
							newSett.setSvalue3("");
							newSett.setSvalue4("");
							userSetings.put("kaEmailoveNotifikacieDate", newSett);
							UsersDB.setSettings(user.getUserId(), userSetings);
						}

						if(notifDateValue == 0)
							datumDo.add(Calendar.DATE, 7);
						else
						{
							//zisti rozdiel dni medzi datumom poslednej poslanej notifikacie a aktualnym datumom
							long pocetDni = (datumOd.getTimeInMillis() - notifDateValue) / (1000 * 60 * 60 * 24);
							if(pocetDni == 7)
							{
								datumDo.add(Calendar.DATE, 7);
							}else
								continue;
						}

						//nastav datum poslanej notifikacie
						notifDate = userSetings.get("kaEmailoveNotifikacieDate");
						notifDate.setSdate(new Timestamp(datumOd.getTimeInMillis()));
						userSetings.put("kaEmailoveNotifikacieDate", notifDate);
						UsersDB.setSettings(user.getUserId(), userSetings);

						subject=prop.getText("calendar.prehlad_7dni");
						emptyBody =prop.getText("calendar.prehlad_ziadne_7dni");
						showEvents = 7;
					}


					List<CalendarDetails> events = getEventsForNotify(datumOd.getTimeInMillis(), datumDo.getTimeInMillis());
					if(events != null && events.size() > 0)
					{
						StringBuilder body= new StringBuilder("<html><head>");
						body.append("<style>");
						body.append("body table td{");
						body.append("font-family: Arial;");
						body.append("font-size: 10pt;");
						body.append('}');
						body.append("</style>");
						body.append("</head><body>");
						body.append("<table border='0' cellspacing='0' cellpadding='0'>\n");
						for (CalendarDetails calendarDetails : events)
						{
							/*
							String datumAkcie = "";
							if(Tools.isNotEmpty(calendarDetails.getFromString()))
								datumAkcie = calendarDetails.getFromString();
							if(Tools.isNotEmpty(calendarDetails.getToString()))
								datumAkcie = datumAkcie.concat(" - "+calendarDetails.getToString());
							if(Tools.isNotEmpty(calendarDetails.getTimeRange()))
								datumAkcie = datumAkcie.concat(" o "+calendarDetails.getTimeRange());
							*/
							body.append("<tr>");
							body.append("   <td valign='top' nowrap><b>").append(prop.getText("calendar_edit.title")).append(":&nbsp;&nbsp;&nbsp;</b></td>");
							body.append("   <td>").append(calendarDetails.getTitle()).append("</td>");
							body.append("</tr>\n");

							body.append("<tr>");
							body.append("   <td valign='top' nowrap><b>").append(prop.getText("calendar_edit.begin")).append(":&nbsp;&nbsp;&nbsp;</b></td>");
							body.append("   <td>").append(calendarDetails.getFromString()).append("</td>");
							body.append("</tr>\n");

							body.append("<tr>");
							body.append("   <td valign='top' nowrap><b>").append(prop.getText("calendar_edit.description")).append(":&nbsp;&nbsp;&nbsp;</b></td>");
							body.append("<td>" ).append(calendarDetails.getDescription()).append("</td>");
							body.append("</tr>\n");

							body.append("<tr>");
							body.append("<td>&nbsp;</td>");
							body.append("<td>&nbsp;</td>");
							body.append("</tr>\n");
						}
						body.append("</table>");
						//tento link sa dava iba v pripade VUB, alebo pri testovani
						if(Tools.isNotEmpty(serverName) && ("testvubnet".equals(serverName) || "vubnet".equals(serverName) || "iwcm.interway.sk".equals(serverName)))
						{
							body.append("<p>");
							body.append("<a href=\"http://").append(serverName).append("/aplikacie/kalendar-akcii/kalendar-akcii.html?showEvents=").append(showEvents).append("\">Zobraziť Kalendár akcií</a>");
							body.append("</p>");
						}
						body.append("</body></html>");

						SendMail.send(Constants.getString("notifyEmail"), Constants.getString("notifyEmail"), user.getEmailAddress(), subject, body.toString());
					}else if(events != null)
					{
						SendMail.send(Constants.getString("notifyEmail"), Constants.getString("notifyEmail"), user.getEmailAddress(), subject, emptyBody);
					}
				}
			}
		}
	}

	/**
	 * vrati akcie pre zadany den z ArrayListu
	 * @param events
	 * @param datum
	 * @return
	 */
	public static List<CalendarDetails> getEventsForDay(List<CalendarDetails> events, long datum)
	{
		List<CalendarDetails> result = new ArrayList<>();
		datum = DB.getTimestamp(Tools.formatDate(datum));
		for (CalendarDetails event : events)
		{
			long calFrom = -1;
			long calTo = -1;
			if(Tools.isNotEmpty(event.getFromString()))
				calFrom =  DB.getTimestamp(event.getFromString());
			if(Tools.isNotEmpty(event.getToString()))
				calTo =  DB.getTimestamp(event.getToString());

			if(calFrom != -1 && calTo == -1)
			{
				if(calFrom == datum)
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
	 * vrati vsetky schvalene udalosti
	 * @param request
	 * @return
	 */
	public static List<CalendarDetails> getApprovedEvents(HttpServletRequest request)
	{
		List<CalendarDetails> events = new ArrayList<>();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			//ziskaj udaje z db
			db_conn = DBPool.getConnection(request);
			StringBuilder sql = new StringBuilder("SELECT c.*, ct.name FROM calendar c, calendar_types ct WHERE c.type_id=ct.type_id AND date_to >= ? AND approve!=-1 "+CloudToolsForCore.getDomainIdSqlWhere(true, "c"));

			long now = (new java.util.Date()).getTime();
			now = now - 86400000;
			long end = 0;

			if (getParamAttribute("calAllEvents", request) != null)
			{
				//chce vsetky zaznamy uplne od zaciatku
				now = 1000;
			}
			if (getParamAttribute("calStart", request) != null)
			{
				//chce zaznamy od datumu
				now = DB.getTimestamp(getParamAttribute("calStart", request), "0:00", DBPool.getDBName(request));
			}
			if (getParamAttribute("calEnd", request) != null)
			{
				//chce zaznamy od datumu
				end = DB.getTimestamp(getParamAttribute("calEnd", request), "23:59", DBPool.getDBName(request));
				sql.append(" AND date_from <= ? ");
			}

			int i;
			i = getParamAttributeInt("calTypeId", request);
			if (i > 0)
			{
				sql.append(" AND c.type_id = ?");
			}
			String typNazvy = getParamAttribute("typyNazvy", request);
			StringBuilder typyInSQL = new StringBuilder(" AND ct.name IN (");
			List<String> typyIn = null;
			if (Tools.isNotEmpty(typNazvy))
			{
				StringTokenizer st = new StringTokenizer(typNazvy, ",+ ");
				while (st.hasMoreTokens())
				{
					String nazov = DB.removeSlashes(st.nextToken());
					if (Tools.isNotEmpty(nazov))
					{
						if (typyIn == null)
						{
							typyIn = new ArrayList<>();
							typyIn.add(nazov);
							typyInSQL.append("?");
						}
						else
						{
							typyIn.add(nazov);	//pripravim si hodnoty do IN
							typyInSQL.append(",?");
						}
					}
				}
				typyInSQL.append(")");
				if (typyIn!=null && typyIn.size() > 0)
				{
					sql.append(typyInSQL);	//pridam IN SQL
				}
			}
			if(request.getAttribute("showApprove") != null)
			{
				sql.append(" AND approve = ?");
			}

			if(request.getAttribute("suggest") != null)
			{
				sql.append(" AND suggest = ?");
			}
			sql.append(" ORDER BY date_from");
			Logger.debug(CalendarDB.class, "sql="+sql.toString());
			int psIndex = 1;
			ps = db_conn.prepareStatement(sql.toString());
			ps.setTimestamp(psIndex++, new Timestamp(0));
			if (end > 0) ps.setTimestamp(psIndex++, new Timestamp(end));
			if(i > 0)
			{
				ps.setInt(psIndex++, i);
			}
			if (typyIn != null && typyIn.size() > 0)
			{
				for(String s: typyIn)
				{
					ps.setString(psIndex++, s);	//postupne nastavim hodnoty do IN
				}
			}
			if(request.getAttribute("showApprove") != null)
			{
				int showApprove = "1".equals(getParamAttribute("showApprove", request)) ? 1 : 0;
				ps.setInt(psIndex++, showApprove);
			}
			if(request.getAttribute("suggest") != null)
				ps.setBoolean(psIndex++, (Boolean)request.getAttribute("suggest"));
			//if (Tools.isNotEmpty(typyIn)) ps.setString(psIndex++, typyIn);
			rs = ps.executeQuery();
			CalendarDetails cal;
			while (rs.next())
			{
				cal = new CalendarDetails();
				cal.setCalendarId(rs.getInt("calendar_id"));
				cal.setTitle(DB.getDbString(rs, "title"));
				cal.setDescription(DB.getDbString(rs, "description"));

				try
				{
					//ak je tam len cislo, je to docId
					if (cal.getDescription().length() > 0 && cal.getDescription().length() < 4)
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
				events.add(cal);
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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		return (events);
	}

	/**
	 * ulozi verejnu udalost do databazy ako neschvalenu a posle mail schvalovatelovi
	 * @param event
	 * @param domain
	 * @param approverMail
	 * @return
	 */
	public static boolean saveEventPublicToDB(EventForm event, String domain, String approverMail, Prop prop)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			String pass = Password.generatePassword(32);
			db_conn = DBPool.getConnection();
			String sql = "INSERT INTO calendar (title, date_from, date_to, type_id, description, time_range, area, city, address, info_1, info_2, info_3, info_4, info_5, notify_hours_before, notify_emails, notify_sender, notify_introtext, notify_sendsms, lng, creator_id, approve, suggest, domain_id, hash_string) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			ps = db_conn.prepareStatement(sql);
			DB.setClob(ps, 1, event.getTitle());
			if (event.getDateFrom().length()>12)
			{
				ps.setTimestamp(2, new Timestamp(DB.getTimestamp(event.getDateFrom())));
			}
			else
			{
				ps.setDate(2, new java.sql.Date(DB.getTimestamp(event.getDateFrom(), "01:01")));
			}
			if (event.getDateTo().length()>12)
			{
				ps.setTimestamp(3, new Timestamp(DB.getTimestamp(event.getDateTo())));
			}
			else
			{
				ps.setDate(3, new java.sql.Date(DB.getTimestamp(event.getDateTo(), "23:59")));
			}
			ps.setInt(4, event.getTypeId());
			DB.setClob(ps, 5, event.getDescription());
			ps.setString(6, event.getTimeRange());
			ps.setString(7, event.getArea());
			ps.setString(8, event.getCity());
			ps.setString(9, event.getAddress());
			ps.setString(10, event.getInfo1());
			ps.setString(11, event.getInfo2());
			ps.setString(12, event.getInfo3());
			ps.setString(13, event.getInfo4());
			ps.setString(14, event.getInfo5());
			ps.setInt(15, event.getNotifyHoursBefore());
			ps.setString(16, event.getNotifyEmails());
			ps.setString(17, event.getNotifySender());
			DB.setClob(ps, 18, event.getNotifyIntrotext());
			ps.setBoolean(19, event.isNotifySendSMS());
			ps.setString(20, event.getLng());
			ps.setInt(21, event.getCreatorId());
			ps.setInt(22, event.getApprove());
			ps.setBoolean(23, false);
			ps.setInt(24, CloudToolsForCore.getDomainId());
			ps.setString(25, pass);
			ps.execute();
			ps.close();

			if (event.getCalendarId()<1)
			{
				//ziskaj ID z db
				ps = db_conn.prepareStatement("SELECT max(calendar_id) as calendar_id FROM calendar WHERE title=?"+CloudToolsForCore.getDomainIdSqlWhere(true));
				ps.setString(1, event.getTitle());
				ResultSet rs = ps.executeQuery();
				if (rs.next())
				{
					event.setCalendarId(rs.getInt("calendar_id"));
				}
				rs.close();
				ps.close();
			}

			db_conn.close();
			ps = null;
			db_conn = null;

			if(Tools.isEmail(approverMail))
			{
				//String hash=CryptoUtil.encrypt(pass);
				String hash = PasswordSecurity.calculateHash(pass, "5671203548520459");
				hash = hash.replace("=", "|");

				String datumAkcie = "";
				String potvrdenieLink = domain+"/components/cloud/calendar/potvrd_akciu_verejnost.jsp?calendarID="+event.getCalendarId()+"&hash="+Tools.URLEncode(hash)+"&schvalit=ano";
				String zrusenieLink = domain+"/components/cloud/calendar/potvrd_akciu_verejnost.jsp?calendarID="+event.getCalendarId()+"&hash="+Tools.URLEncode(hash)+"&schvalit=nie";
				if(Tools.isNotEmpty(event.getDateFrom()))
					datumAkcie = event.getDateFrom();
				if(Tools.isNotEmpty(event.getDateTo()))
					datumAkcie = datumAkcie.concat(" - "+event.getDateTo());
				if(Tools.isNotEmpty(event.getTimeRange()))
					datumAkcie = datumAkcie.concat(" o "+event.getTimeRange());

				StringBuilder emailClientData=new StringBuilder();
				emailClientData.append("<html><head>");
				emailClientData.append("<style>");
				emailClientData.append("body{");
				emailClientData.append("font-family: Arial;");
				emailClientData.append("font-size: 11pt;");
				emailClientData.append('}');
				emailClientData.append("</style></head><body>");

				if(Tools.isNotEmpty(event.getTitle()))
					emailClientData.append(prop.getText("calendar.verejna_akcia_info1", event.getTitle()));
				if(Tools.isNotEmpty(datumAkcie))
					emailClientData.append(prop.getText("calendar.verejna_akcia_info2", datumAkcie));
				if(Tools.isNotEmpty(EventTypeDB.getTypeById(event.getTypeId()).getName()))
					emailClientData.append(prop.getText("calendar.verejna_akcia_info3", EventTypeDB.getTypeById(event.getTypeId()).getName()));
				if(Tools.isNotEmpty(event.getCity()))
					emailClientData.append(prop.getText("calendar.verejna_akcia_info4", event.getCity()));
				if(Tools.isNotEmpty(event.getAddress()))
					emailClientData.append(prop.getText("calendar.verejna_akcia_info5", event.getAddress()));
				if(Tools.isNotEmpty(event.getDescription()))
					emailClientData.append(prop.getText("calendar.verejna_akcia_info6", event.getDescription()));

				emailClientData.append(prop.getText("calendar.potvrdit_verejnu_akciu", potvrdenieLink));
				emailClientData.append(prop.getText("calendar.zmazat_verejnu_akciu", zrusenieLink));

				emailClientData.append("</body></html>");
				SendMail.send(domain, approverMail, approverMail, prop.getText("calendar.na_schvalenie"), emailClientData.toString());
			}

		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
			return false;
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

		return true;
	}

	/**
	 * nastavi status verejnej udalosti o jej schvaleni/neschvaleni
	 * @param eventId
	 * @param status
	 * @return
	 */
	public static boolean savePublicApproveStatus(int eventId, int status)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("UPDATE calendar SET approve = ? WHERE calendar_id = ? "+CloudToolsForCore.getDomainIdSqlWhere(true));
			ps.setInt(1, status);
			ps.setInt(2, eventId);
			ps.execute();
			ps.close();
			db_conn.close();
			ps = null;
			db_conn = null;

		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
			return false;
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

		return true;
	}
}
