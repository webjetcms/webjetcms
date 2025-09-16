package sk.iway.iwcm.components.welcome;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DateTools;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.stat.Column;
import sk.iway.iwcm.stat.StatNewDB;
import sk.iway.iwcm.stat.StatTableDB;

/**
 * WelcomeDataBackTime.java
 *
 * Ziskanie a cachovanie udajov z WebJETu pre homepage WebJET 8. Poskytuje
 * nasledovne udaje za obdobie poslednych 24 hodin. Obdobie sa konfiguruje cez
 * konfiguracnu premennu "welcomeDataBackTimeMinutes" co je pocet minut za ktore
 * sa budu data z DB vyberat.
 *
 *
 * @Title webjet7
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2014
 * @author $Author: jeeff mkolejak $
 * @version $Revision: 1.3 $
 * @created Date: 14.7.2014 13:37:29
 * @modified $Date: 2004/08/16 06:26:11 $
 */
public class WelcomeDataBackTime
{
	private static WelcomeDataBean welcomeDataBean;
	//private static long month_4 = 4l * 30l * 24l * 60l * 60l * 1000l;
	//private static long days_17 = 17l * 24l * 60l * 60l * 1000l;

	protected WelcomeDataBackTime() {
		//utility class
	}

	public static WelcomeDataBean getWelcomeDataBackTime()
	{
		String cacheKey = "welcomeDataBackTimes-domainId="+CloudToolsForCore.getDomainId();
		Cache cache = Cache.getInstance();
		if (cache.getObject(cacheKey) != null)
		{
			welcomeDataBean = (WelcomeDataBean) (cache.getObject(cacheKey));
		}
		else
		{
			welcomeDataBean = initialize();
			cache.setObjectSeconds(cacheKey, welcomeDataBean, getWelcomeDataCacheMinutesConst()*60, true);
		}
		return welcomeDataBean;
	}

	public static int getWelcomeDataCacheMinutesConst()
	{
		return Constants.getInt("welcomeDataCacheMinutes");
	}

	public static WelcomeDataBean initialize()
	{
		welcomeDataBean = new WelcomeDataBean();
		initFillFormNumber();
		initDocumentForumNumber();
		Calendar calendarFrom = DateTools.timestampToCalendar(getTimeFrom());
		Calendar calendarTo = DateTools.timestampToCalendar(getTimeTo());
		initStatsViewNumber(calendarFrom, calendarTo);
		initStatsErrorNumber(calendarFrom, calendarTo);
		return welcomeDataBean;
	}

	private static void initDocumentForumNumber()
	{
		try
		{
			int documentForumNumber = new SimpleQuery().forInt(
						"SELECT count(*) AS countForums FROM document_forum WHERE question_date between ? and ? " + CloudToolsForCore.getDomainIdSqlWhere(true), getTimeFrom(),
						getTimeTo());
			Logger.debug(WelcomeDataBackTime.class, "documentForumNumber: " + documentForumNumber);
			welcomeDataBean.setDocumentForumNumber(documentForumNumber);
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

	}

	private static void initFillFormNumber()
	{
		try
		{
			int fillFormsNumber = new SimpleQuery().forInt(
						"SELECT count(*) AS countForms FROM forms WHERE create_date is not null AND create_date between ? and ? " + CloudToolsForCore.getDomainIdSqlWhere(true), getTimeFrom(),
						getTimeTo());
			Logger.debug(WelcomeDataBackTime.class, "fillFormsNumber: " + fillFormsNumber);
			welcomeDataBean.setFillFormsNumber(fillFormsNumber);
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
	}

	private static void initStatsErrorNumber(Calendar calendarFrom, Calendar calendarTo)
	{
		int statErrorNumber = 0;
		if ("none".equals(Constants.getString("statMode"))==false)
		{
			try
			{
			/*
			String multiwebSql = "";
			if(InitServlet.isTypeCloud())
			{
				multiwebSql = " AND query_string LIKE '%"+CloudToolsForCore.getDomainName()+"%'";
			}

			String table_stat_error = "stat_error" + "_" + calendarTo.get(Calendar.YEAR) + "_" + (calendarTo.get(Calendar.MONTH) + 1);
			String sql_stat_error = "select sum(count) from  " + table_stat_error + " where week = ? ";
			int week = calendarFrom.get(Calendar.WEEK_OF_YEAR)+1;
			//.get(Calendar.WEEK_OF_YEAR)+1 lebo calendarFrom je cas presne pred tyzdnom a v tabulke su zaznamy iba podla cisel tyzdnov.
			//To znamena ze ak by nebolo +1 dostaly by sme zaznamy v celom minulom tyzdni (pondelok-nedela), takto je to v aktualnom
			statErrorNumber = new SimpleQuery().forInt(sql_stat_error+multiwebSql, week);
			if (calendarFrom.get(Calendar.MONTH) + 1 == calendarTo.get(Calendar.MONTH))
			{
				table_stat_error = "stat_error" + "_" + calendarFrom.get(Calendar.YEAR) + "_" + (calendarFrom.get(Calendar.MONTH) + 1);
				sql_stat_error = "select sum(count) from  " + table_stat_error + " where week = ? ";
				statErrorNumber += new SimpleQuery().forInt(sql_stat_error+multiwebSql, week);
			}
			*/

				List<Column> errorPages = StatTableDB.getErrorPages(99999, calendarFrom.getTime(), calendarTo.getTime(), null, false);
			/*for (Column c : errorPages)
			{
				statErrorNumber += c.getIntColumn5();
			}*/
				statErrorNumber = errorPages.size();

				Logger.debug(WelcomeDataBackTime.class, "statErrorNumber: " + statErrorNumber);
			} catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}
		welcomeDataBean.setStatErrorNumber(statErrorNumber);
	}

	private static void initStatsViewNumber(Calendar calendarFrom, Calendar calendarTo)
	{
		int statViewsNumber = 0;
		if ("none".equals(Constants.getString("statMode"))==false)
		{
			try
			{
			/*
			String multiwebSql = "";
			if(InitServlet.isTypeCloud())
			{
				// " AND " + column + " IN ("+searchGroups+") ";
				multiwebSql = StatDB.getRootGroupWhere("group_id", CloudToolsForCore.getDomainId());
				if(multiwebSql.startsWith(" AND"))
					multiwebSql = multiwebSql.replace(" AND ", "");
				multiwebSql = " WHERE " + multiwebSql;
			}

			String table_stat_views = "stat_views" + "_" + calendarTo.get(Calendar.YEAR) + "_" + (calendarTo.get(Calendar.MONTH) + 1);
			String sql_stat_views = "select count(distinct session_id) from  " + table_stat_views;
			int statViewsNumber = new SimpleQuery().forInt(sql_stat_views+multiwebSql);
			if (calendarFrom.get(Calendar.MONTH) + 1 == calendarTo.get(Calendar.MONTH))
			{
				table_stat_views = "stat_views" + "_" + calendarFrom.get(Calendar.YEAR) + "_" + (calendarFrom.get(Calendar.MONTH) + 1);
				sql_stat_views = "select count(distinct session_id) from " + table_stat_views;
				statViewsNumber += new SimpleQuery().forInt(sql_stat_views+multiwebSql);
			}
			*/

				int groupId = -1;
				if (InitServlet.isTypeCloud()) groupId = CloudToolsForCore.getDomainId();

				List<Column> statVSDays = StatNewDB.getDayViews(calendarFrom.getTime(), calendarTo.getTime(), groupId, true);

				for (Column c : statVSDays)
				{
					statViewsNumber += c.getIntColumn3();
				}

				Logger.debug(WelcomeDataBackTime.class, "statViewsNumber: " + statViewsNumber);
			} catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}
		welcomeDataBean.setStatViewsNumber(statViewsNumber);
	}

	public static Timestamp getTimeFrom()
	{
		long welcomeDataBackTimeMinutes = getWelcomeDataBackTimeConst();
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, -(int)welcomeDataBackTimeMinutes);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);

		Timestamp timeFrom = new Timestamp(cal.getTimeInMillis());
		Logger.debug(WelcomeDataBackTime.class, "timeFrom: " + timeFrom);
		return timeFrom;
	}

	public static Timestamp getTimeTo()
	{
		Timestamp timeTo = new Timestamp(new Date().getTime());
		Logger.debug(WelcomeDataBackTime.class, "timeTo: " + timeTo);
		return timeTo;
	}

	public static int getWelcomeDataBackTimeConst()
	{
		return Constants.getInt("welcomeDataBackTime");
	}
}
