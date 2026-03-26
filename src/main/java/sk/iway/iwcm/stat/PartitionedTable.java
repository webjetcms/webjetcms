package sk.iway.iwcm.stat;


/**
 *  PartitionedTables.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2012
 *@author       $Author: jeeff Marián Halaš $
 *@version      $Revision: 1.3 $
 *@created      Date: 6.8.2012 11:16:21
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public enum PartitionedTable {
	
	
	StatError("stat_error"),
	StatFrom("stat_from"),
	StatSearchengine("stat_searchengine"),
	StatViews("stat_views"),
	StatClicks("stat_clicks");
	
	
	private String tableName;
	
	public String getTableName()
	{
		return tableName;
	}

	private PartitionedTable(String tableName)
	{
		this.tableName = tableName;
	}
	
	public static PartitionedTable fromString(String tableName)
	{
		for (PartitionedTable table : PartitionedTable.values())
			if (table.tableName.equals(tableName.toLowerCase()))
				return table;
		throw new IllegalArgumentException("Neexistuje tabulka s menom : "+tableName);
	}
}
