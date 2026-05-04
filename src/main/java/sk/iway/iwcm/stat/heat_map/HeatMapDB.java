package sk.iway.iwcm.stat.heat_map;

import static sk.iway.iwcm.Tools.isEmpty;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.DynaBean;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.stat.StatNewDB;
import sk.iway.iwcm.stat.StatWriteBuffer;

/**
 *  HeatMapDB.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 26.5.2010 16:16:54
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class HeatMapDB
{
	private static final int NO_DATE = -1;
	private static Map<Integer, Integer> clicksMap = new HashMap<>();

	protected HeatMapDB() {
		//utility class
	}

	/**
	 * Tranforms cookie values into corresponding clicks
	 * @param
	 */
	public static void recordCookie(String value)
	{
		if (Constants.getBoolean("statEnableClickTracking") == false || "none".equals(Constants.getString("statMode")))
			return;
		try
		{
			if (value == null) return;
			value = value.trim();
			Logger.debug(HeatMapDB.class, "Clicking cookie received: "+value);
			String[] decomposited = value.split("\\[");
			//document id is on the beginning
			if (decomposited.length < 2)
				return;

			Integer docId = Integer.valueOf(decomposited[0]);
			int today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);

			for (String tuple : decomposited)
			{
				String[] parts = tuple.split(",");
				if (isEmpty(tuple) || parts.length != 2) continue;
				Integer x = Integer.valueOf(parts[0].trim());
				Integer y = Integer.valueOf(parts[1].replace("]", "").trim());
				StatWriteBuffer.add("INSERT INTO stat_clicks"+StatNewDB.getTableSuffix("stat_clicks")+" (x, y, document_id, day_of_month) VALUES(?,?,?,?)", "stat_clicks", x,y,docId,today);
			}
		}
		catch (NumberFormatException e)
		{
			//malformed expression, just ignore, nothing wrong is happening
		}
	}

	public static IwcmFile generateHeatMap(HttpServletRequest request)
	{
		try{
			int documentId = Integer.parseInt(request.getParameter("document_id"));
			Date startDate = (Date)request.getSession().getAttribute("startDate");
			Date endDate = (Date)request.getSession().getAttribute("endDate");
			StringBuilder stringBuilder = new StringBuilder().append("/WEB-INF/tmp/heat_map/heat_map_").append(documentId).
				append("_").append(Tools.formatDate(startDate)).append('_').append(Tools.formatDate(endDate)).append(".png");
			String fileName = stringBuilder.toString();
			IwcmFile image = new IwcmFile(Tools.getRealPath(fileName));
			image.getParentFile().mkdirs();

			final long now = System.currentTimeMillis();
			final long TOO_OLD = Constants.getInt("statHeatMapImageTimeout")*1000L;

			if (!image.exists() || (now - image.lastModified() > TOO_OLD))
			{
				HeatMapDB.createHeatMap(image, documentId, startDate.getTime(), endDate.getTime());
			}

			return image;
		}
		catch (NoRecordException e)
		{
			return new IwcmFile(Tools.getRealPath("/components/stat/images/heat_map_no_clicks.gif"));
		}
		catch (IOException e) {sk.iway.iwcm.Logger.error(e);}
		throw new IllegalStateException("Unexpected code path in.");
	}

	public static void createHeatMap(IwcmFile out, int documentId, long from, long to) throws IOException
	{
		//caller gets table names starting with table creation date unless null is supplied
		String[] suffices = StatNewDB.getTableSuffix("stat_clicks", from, to);
		int dayFrom = getDayFromDate(from);
		int dayTo = getDayFromDate(to);
		if (suffices.length == 0) return;

		String startSuffix = suffices[0];
		String endSuffix = null;
		if (suffices.length > 1)
			endSuffix = suffices[suffices.length - 1];
		List<String> middleSuffices = new ArrayList<>(Arrays.asList(suffices));
		middleSuffices.remove(0);
		if (middleSuffices.size() > 0)
			middleSuffices.remove(middleSuffices.size() - 1);

		List<Click> clicks = new ArrayList<>();

		clicks.addAll(clicksFor(startSuffix, documentId, dayFrom, NO_DATE));
		for (String suffix : middleSuffices)
			clicks.addAll(clicksFor(suffix, documentId, NO_DATE, NO_DATE));
		if (endSuffix != null)
			clicks.addAll(clicksFor(endSuffix, documentId, NO_DATE, dayTo));

		new HeatMapGenerator(clicks, out).generate();
	}

	private static int getDayFromDate(long date)
	{
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date(date));
		return cal.get(Calendar.DAY_OF_MONTH);
	}

	private static Collection<Click> clicksFor(String tableSuffix, int documentId, int dayFrom, int dayTo)
	{
		if (isEmpty(tableSuffix)) return new ArrayList<>();
		List<Object> params = new ArrayList<>();
		StringBuilder sql = new StringBuilder().
			append("SELECT x, y FROM stat_clicks").append(tableSuffix).append(" WHERE document_id = ?");

		params.add(documentId);

		if (dayFrom != NO_DATE)
		{
			sql.append(" AND day_of_month >= ?");
			params.add(dayFrom);
		}
		if (dayTo != NO_DATE)
		{
			sql.append(" AND day_of_month <= ?");
			params.add(dayTo);
		}

		List<DynaBean> results = DB.getDynaList(sql.toString(), params);
		List<Click> clicks = new ArrayList<>();

		int statHeatMapMaxWidth = Constants.getInt("statHeatMapMaxWidth");
		int statHeatMapMaxHeight = Constants.getInt("statHeatMapMaxHeight");
		for (DynaBean dynaBean : results)
		{
			int x = (int)Double.parseDouble(dynaBean.get("x").toString());
			int y = (int)Double.parseDouble(dynaBean.get("y").toString());

			//TODO: toto by chcelo nejako rozumnejsie nastavovat, nie takto natvrdo, pri velkych cislach to hadzalo OOM
			if (x>statHeatMapMaxWidth) continue;
			if (y>statHeatMapMaxHeight) continue;

			clicks.add(new Click().setX(x).setY(y));
		}

		return clicks;
	}

	/**
	 * Prida k zadanej HashMap-e dokumenty podla zadaneho datumu
	 * ak sa tableSuffix rovna null, vrati uz nacitanu hashmapu clicksMap
	 * @param tableSuffix
	 * @param dayFrom
	 * @param dayTo
	 * @param clicks
	 * @return
	 */
	public static Map<Integer, Integer> getClicksInDate(String tableSuffix, int dayFrom, int dayTo, Map<Integer, Integer> clicks){
		if(tableSuffix == null) return clicksMap;

		clicksMap = clicks;
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			String sql = "SELECT DISTINCT document_id FROM stat_clicks_"+tableSuffix+" WHERE day_of_month >= ? AND day_of_month <= ?";
			Logger.debug(HeatMapDB.class, "sql=" + sql +" from=" + dayFrom+" to="+dayTo);
			db_conn = DBPool.getConnection();

			ps = db_conn.prepareStatement(sql);
			if (dayFrom != NO_DATE)
			{
				ps.setInt(1, dayFrom);
			}
			else ps.setInt(1, 1);
			if (dayTo != NO_DATE)
			{
				ps.setInt(2, dayTo);
			}
			else ps.setInt(2, 31);
			rs = ps.executeQuery();
			List<Integer> documentList = new ArrayList<>();
			while (rs.next())
			{
				documentList.add(rs.getInt("document_id"));
			}
			rs.close();
			ps.close();
			for(Integer id: documentList)
			{
				sql = "SELECT COUNT(document_id) FROM stat_clicks_"+tableSuffix+" WHERE document_id=?";
				Logger.debug(HeatMapDB.class, "sql="+sql);
				ps = db_conn.prepareStatement(sql);
				ps.setInt(1, id);
				rs = ps.executeQuery();
				while(rs.next())
				{
					if(!clicksMap.containsKey(id)){
						clicksMap.put(id, rs.getInt(1));	//count
					}
					else {
						int count = clicksMap.get(id);
						clicksMap.put(id, count+rs.getInt(1));	//ak uz existuje - napr. v inom mesiaci - pripocitam
					}
				}
				rs.close();
				ps.close();
			}

			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex){sk.iway.iwcm.Logger.error(ex);}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null)	db_conn.close();
			}
			catch (Exception ex2){sk.iway.iwcm.Logger.error(ex2);}
		}
		return clicksMap;
	}
}