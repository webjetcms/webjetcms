package sk.iway.iwcm.system.monitoring;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.components.monitoring.jpa.ExecutionEntry;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.stripes.SyncDirAction;
import sk.iway.iwcm.system.cluster.ClusterRefresher;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

/**
 *  MonitoringDB.java
 *
 *		Serializes and deserializes monitoring statistics
 *		into/from table cluster_monitoring, using {@link XMLEncoder}.
 *
 *		This is done if and only if server runs in cluster configuration.
 *		Otherwise it makes no sense to persist this data, as we can take
 *		them directly from memory. Persisting this data allows us
 *		to monitor execution times on any other node, even if that
 *		node lacks administration interface.
 *
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 27.7.2010 14:32:19
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class MonitoringDB
{
	private static final String insert = "INSERT INTO cluster_monitoring(node, type, content, created_at) VALUES(?, ?, ?, ?)";
	public static final String TYPE_SQL = "sql";
	public static final String TYPE_COMPONENT = "component";
	public static final String TYPE_DOCUMENT = "document";

	/**
	 * Refresh called by {@link ClusterRefresher}
	 */
	public static void getInstance(boolean refresh)
	{
		writeStatsToSharedTable();
	}

	/**
	 * Writes snapshot of stats into cluster_monitoring table
	 */
	public static void writeStatsToSharedTable()
	{
		if (!Constants.getBoolean("serverMonitoringEnablePerformance")) return;

		eraseMyStats();
		writeSqlStats();
		writeComponentStats();
		writeDocumentStats();
		sk.iway.iwcm.Logger.println(MonitoringDB.class, "Persisting performance stats done");
	}

	private static void eraseMyStats()
	{
		new SimpleQuery().execute("DELETE FROM cluster_monitoring WHERE node = ? AND (type = ? OR type = ? OR type = ?)", myNode(), TYPE_SQL, TYPE_COMPONENT, TYPE_DOCUMENT);
		Logger.debug(MonitoringDB.class, String.format("Erased performance stats for %s", myNode()));
	}

	private static void writeSqlStats()
	{
		new SimpleQuery().execute(insert, myNode(), TYPE_SQL, serialize(ExecutionTimeMonitor.statsForSqls()), now());
		Logger.debug(MonitoringDB.class, String.format("Persisted SQL performance stats for %s", myNode()));
	}

	private static void writeComponentStats()
	{
		new SimpleQuery().execute(insert, myNode(), TYPE_COMPONENT, serialize(ExecutionTimeMonitor.statsForComponents()), now());
		Logger.debug(MonitoringDB.class, String.format("Persisted component performance stats for %s", myNode()));
	}

	private static void writeDocumentStats()
	{
		new SimpleQuery().execute(insert, myNode(), TYPE_DOCUMENT, serialize(ExecutionTimeMonitor.statsForDocuments()), now());
		Logger.debug(MonitoringDB.class, String.format("Persisted URL performance stats for %s", myNode()));
	}

	private static Timestamp now()
	{
		return new Timestamp(System.currentTimeMillis());
	}

	private static String myNode()
	{
		return Constants.getString("clusterMyNodeName");
	}

	private static String serialize(List<ExecutionEntry> stats)
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XMLEncoder encoder = new XMLEncoder(out);
		encoder.writeObject(stats);
		encoder.close();
		String content = new String(out.toByteArray(), 0, out.size());
		return content.replaceAll("version=\"[0-9.-]+\"", "version=\"1.0\"");
	}


	public static List<ExecutionEntry> getSqlStatsFor(String node)
	{
		return readStatsFromDb(node, TYPE_SQL);
	}

	public static List<ExecutionEntry> getComponentStatsFor(String node)
	{
		return readStatsFromDb(node, TYPE_COMPONENT);
	}

	public static List<ExecutionEntry> getDocumentStatsFor(String node)
	{
		return readStatsFromDb(node, TYPE_DOCUMENT);
	}

	private static List<ExecutionEntry> readStatsFromDb(String node, String type)
	{
		Logger.println(MonitoringDB.class, String.format("Request to read %s performance stats of %s", type, node));
		ComplexQuery query = new ComplexQuery().setSql("SELECT content FROM cluster_monitoring WHERE node = ? AND type = ?").setParams(node, type);
		List<String> content = query.list(new Mapper<String>(){
			@Override
			public String map(ResultSet rs) throws SQLException{
				return rs.getString(1);
			}
		});

		if (content.size() == 0)
		{
			Logger.println(MonitoringDB.class, "There are no such stats");
			return Collections.emptyList();
		}

		try {
			return deserialize(content.get(0));
		}
		catch (IOException e) {
			sk.iway.iwcm.Logger.error(e);
		}

		return Collections.emptyList();
	}

	@SuppressWarnings("unchecked")
	private static List<ExecutionEntry> deserialize(String content) throws IOException {
		//Bug FIX, our ExecutionEntry is in different package than original one
		//We need to replace old package with new one
		content = content.replace("sk.iway.iwcm.system.monitoring.ExecutionEntry", "sk.iway.iwcm.components.monitoring.jpa.ExecutionEntry");

		InputStream in = new ByteArrayInputStream(content.getBytes());
		InputStream in2 = SyncDirAction.checkXmlForAttack(in);
		XMLDecoder decoder = new XMLDecoder(in2);
		List<ExecutionEntry> records = (List<ExecutionEntry>)decoder.readObject();
		decoder.close();
		try { in.close(); } catch (Exception ex) {};
		try { in2.close(); } catch (Exception ex) {};
		Logger.println(MonitoringDB.class, String.format("Node stats contains %d records", records.size()));
		return records;
	}
}