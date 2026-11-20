package sk.iway.iwcm.stat.heat_map;

import static sk.iway.iwcm.Tools.isEmpty;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
 * Records legacy tracking cookies and reads monthly click data for reports and heatmap images.
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
	 * Queues the coordinates from a legacy tracking cookie when click tracking is enabled.
	 *
	 * @param value document ID followed by bracketed coordinate pairs
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

	/**
	 * Writes a heatmap from weighted click positions within the requested calendar days.
	 *
	 * @param out destination PNG file
	 * @param documentId page whose clicks are rendered
	 * @param from start time in milliseconds since the epoch
	 * @param to end time in milliseconds since the epoch
	 * @throws IOException if the image cannot be written
	 * @throws NoRecordException if the selected partitions contain no renderable clicks
	 */
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

		clicks.addAll(clicksFor(startSuffix, documentId, dayFrom, endSuffix == null ? dayTo : NO_DATE));
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

	/**
	 * Loads weighted click positions within the configured image bounds for one monthly table.
	 *
	 * @param tableSuffix partition suffix including its leading underscore
	 * @param documentId page whose clicks are loaded
	 * @param dayFrom first day to include, or {@code -1} for no lower bound
	 * @param dayTo last day to include, or {@code -1} for no upper bound
	 * @return positions and the number of clicks represented by each row
	 */
	private static Collection<Click> clicksFor(String tableSuffix, int documentId, int dayFrom, int dayTo)
	{
		if (isEmpty(tableSuffix)) return new ArrayList<>();
		List<Object> params = new ArrayList<>();
		StringBuilder sql = new StringBuilder().
			append("SELECT x, y, count AS click_weight FROM stat_clicks").append(tableSuffix).append(" WHERE document_id = ?");

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

			Click click = new Click().setX(x).setY(y);
			click.count = ((Number) dynaBean.get("click_weight")).intValue();
			clicks.add(click);
		}

		return clicks;
	}

    /**
     * Adds weighted click totals by document, capping each total at {@link Integer#MAX_VALUE}.
     *
     * @param tableSuffix month in {@code YYYY_M} format, or {@code null} to retrieve the cached map
     * @param dayFrom first day to include, or {@code -1} for day one
     * @param dayTo last day to include, or {@code -1} for day 31
     * @param clicks accumulator keyed by document ID
     * @return the supplied accumulator, or the cached map when the suffix is null
     */
    public static Map<Integer, Integer> getClicksInDate(String tableSuffix, int dayFrom, int dayTo, Map<Integer, Integer> clicks) {
        if (tableSuffix == null) return clicksMap;
        clicksMap = clicks;
        if (!tableSuffix.matches("[0-9]{4}_(?:0?[1-9]|1[0-2])")) return clicks;
        String sql = "SELECT document_id, " + HeatMapStorage.SUM_CLICKS + " FROM stat_clicks_" + tableSuffix
                + " WHERE day_of_month BETWEEN ? AND ? GROUP BY document_id";
        try (Connection db = DBPool.getConnection(); PreparedStatement statement = db.prepareStatement(sql)) {
            statement.setInt(1, dayFrom == NO_DATE ? 1 : dayFrom);
            statement.setInt(2, dayTo == NO_DATE ? 31 : dayTo);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    int docId = rows.getInt(1);
                    long total = rows.getLong(2) + clicks.getOrDefault(docId, 0);
                    clicks.put(docId, (int) Math.min(Integer.MAX_VALUE, total));
                }
            }
        } catch (SQLException exception) {
            Logger.error(HeatMapDB.class, exception);
        }
        return clicks;
    }
}
