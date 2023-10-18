package sk.iway.iwcm.system.cron;

import java.util.List;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.system.cluster.ClusterDB;

/**
 *  WebjetDatabaseTaskSource.java
 *
 *		Default {@link TaskSource} provider. Loads cron tasks from
 *		crontab table. Discards tasks not designed to run on current cluster node.
 *
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 9.7.2010 16:07:48
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class WebjetDatabaseTaskSource implements TaskSource
{
	@Override
	public List<CronTask> getTasks()
	{
		Logger.println(WebjetDatabaseTaskSource.class, "Loading cron tasks from database.");
		List<CronTask> tasks = loadFromDatabase();
		Logger.println(WebjetDatabaseTaskSource.class, String.format("%d tasks should run on this node", tasks.size()));
		return tasks;
	}

	private List<CronTask> loadFromDatabase()
	{
		if (ClusterDB.isServerRunningInClusterMode()==false) return new ComplexQuery().setSql("SELECT * FROM crontab").list(CronDB.mapper);
		
		return new ComplexQuery().setSql("SELECT * FROM crontab WHERE cluster_node IS NULL OR cluster_node = 'all' OR cluster_node = ?").
			setParams(Constants.getString("clusterMyNodeName")).list(CronDB.mapper);
	}
}