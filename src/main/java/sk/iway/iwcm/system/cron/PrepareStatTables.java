package sk.iway.iwcm.system.cron;

import java.util.Calendar;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.stat.PartitionedTable;
import sk.iway.iwcm.stat.StatNewDB;

/**
 *  PrepareStatTables.java
 *  
 *  Zisti ci existuju generovane tabulky pre statistiky pre aktualny mesiac + args[0] mesiacov dopredu
 *  ak neexistuju tak ich vytvori a grantne prava na ne userovi z Constants "publicWebDbUserName"
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2012
 *@author       $Author: Marián Halaš $
 *@version      $Revision: 1.3 $
 *@created      Date: 6.8.2012 10:43:38
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class PrepareStatTables
{
	public static void main(String[] args)
	{	
		Logger.debug(PrepareStatTables.class, "Preparing tables for StatDB");
		if(!Constants.getBoolean("statEnableTablePartitioning"))
		{
			Logger.debug(PrepareStatTables.class,"Partitioning disabled. Prepare not needed. Exit.");
			return;
		}
		if(args !=null)
		{
			int monthsForward = Tools.getIntValue(args[0], 3); //pocet mesiacov na ktore sa dopredu budu testovat tabulky
			Calendar now = Calendar.getInstance();
			Calendar statTo = Calendar.getInstance();
			statTo.add(Calendar.MONTH, monthsForward);
			
			String publicWebDbUserName = Constants.getString("publicWebDbUserName");
			if(Tools.isEmpty(publicWebDbUserName))
			{
				Logger.debug(PrepareStatTables.class,"publicWebDbUserName not defined, don't know whom to grant rights! Exit.");
				return;
			}
			String [] suffixes = StatNewDB.getTableSuffix(now.getTimeInMillis(), statTo.getTimeInMillis());
			for(String suffix : suffixes)
			{
				for(PartitionedTable table : PartitionedTable.values())
				{
					if(!StatNewDB.tableExists(table.getTableName()+suffix))
					{
						StatNewDB.createStatTable(table.getTableName(), suffix);
						StatNewDB.grantRightsToUser(table.getTableName(), suffix, publicWebDbUserName);
					}
				}
			}
		}
		Logger.debug(PrepareStatTables.class, "Tables are prepared");
	}
}
