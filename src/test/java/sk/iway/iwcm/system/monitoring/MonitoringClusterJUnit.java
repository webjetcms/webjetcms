package sk.iway.iwcm.system.monitoring;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.components.monitoring.jpa.ExecutionEntry;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.stripes.SyncDirAction;

/**
 *  MonitoringClusterJUnit.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 27.7.2010 11:49:30
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class MonitoringClusterJUnit
{

	ByteArrayOutputStream out;

	@BeforeEach
	public void addSomeRedocrds()
	{
		Constants.setBoolean("serverMonitoringEnablePerformance", true);
		Constants.setString("clusterMyNodeName", "test_node");
		ExecutionTimeMonitor.recordSqlExecution("SELECT 1 FROM documents", 1);
		ExecutionTimeMonitor.recordComponentExecution("/components/news/news.jsp", 5, 1000);
		out = new ByteArrayOutputStream();
	}

	@AfterEach
	public void cleanUp()
	{
		Constants.setBoolean("serverMonitoringEnablePerformance", false);
		Constants.setString("clusterMyNodeName", "");
		new SimpleQuery().execute("DELETE FROM cluster_monitoring");
	}

	@Test
	public void xmlPersistence()
	{
		XMLEncoder encoder = new XMLEncoder(out);
		encoder.writeObject(ExecutionTimeMonitor.statsForSqls());
		encoder.close();
		System.out.println(content());
		assertTrue(content().contains("<string>SELECT 1 FROM documents</string>"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void xmlDeserialization() throws IOException {
		xmlPersistence();
		InputStream is = new ByteArrayInputStream(content().getBytes());
		InputStream is2 = SyncDirAction.checkXmlForAttack(is);
		XMLDecoder decoder = new XMLDecoder(is2);
		List<ExecutionEntry> records = (List<ExecutionEntry>)decoder.readObject();
		decoder.close();
		try { is.close(); } catch (Exception ex) {};
		try { is2.close(); } catch (Exception ex) {};
		assertTrue(records.size() == 1);
		assertTrue(records.get(0).getName().equals("SELECT 1 FROM documents"));
	}

	public String content()
	{
		return new String(out.toByteArray(), 0, out.size());
	}

	@Test
	public void persisting()
	{
		MonitoringDB.writeStatsToSharedTable();
		String content = new SimpleQuery().forString("SELECT content FROM cluster_monitoring WHERE node = ? AND type = ?", "test_node", "sql");
		assertTrue(content.contains("SELECT 1 FROM documents"));
	}

	@Test
	public void loading()
	{
		MonitoringDB.writeStatsToSharedTable();
		List<ExecutionEntry> records = MonitoringDB.getSqlStatsFor("test_node");
		assertTrue(records.size() == 1);
	}
}