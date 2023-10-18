package sk.iway.iwcm;

/**
 *  PkeyBean.java - bean pre Primary Key Generator - riadok tabulky pkey_generator
 *
 *@Title        webjet
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      Date: 29.9.2004 14:16:19
 *@modified     $Date: 2004/10/04 09:18:58 $
 */
class PkeyBean
{
	private String name;
	private long value = 1;
	private long maxValue = 1;
	private String tableName;
	private String tablePkeyName;
	
	public String toString()
	{
		return(getName() + " [" + getTablePkeyName() + "]=" + getValue()+" max:"+getMaxValue());
	}
	
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public String getTableName()
	{
		return tableName;
	}
	public void setTableName(String tableName)
	{
		this.tableName = tableName;
	}
	public String getTablePkeyName()
	{
		return tablePkeyName;
	}
	public void setTablePkeyName(String tablePkeyName)
	{
		this.tablePkeyName = tablePkeyName;
	}
	public long getValue()
	{
		return value;
	}
	public void setValue(long value)
	{
		this.value = value;
	}
	public long getMaxValue()
	{
		return maxValue;
	}
	public void setMaxValue(long maxValue)
	{
		this.maxValue = maxValue;
	}
}
