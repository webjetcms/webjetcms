package sk.iway.iwcm.system;

/**
 *  UpdateDBBean.java - bean s informaciami o aktualizacii databazy
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      $Date: 2005/07/15 10:06:26 $
 *@modified     $Date: 2005/07/15 10:06:26 $
 */
public class UpdateDBBean
{
	String author;
	String desc;
	String date;
	String mysql;
	String mssql;
	String oracle;
	/**
	 * @return Returns the author.
	 */
	public String getAuthor()
	{
		return author;
	}
	/**
	 * @param author The author to set.
	 */
	public void setAuthor(String author)
	{
		this.author = author;
	}
	/**
	 * @return Returns the date.
	 */
	public String getDate()
	{
		return date;
	}
	/**
	 * @param date The date to set.
	 */
	public void setDate(String date)
	{
		this.date = date;
	}
	/**
	 * @return Returns the desc.
	 */
	public String getDesc()
	{
		return desc;
	}
	/**
	 * @param desc The desc to set.
	 */
	public void setDesc(String desc)
	{
		this.desc = desc;
	}
	/**
	 * @return Returns the mssql.
	 */
	public String getMssql()
	{
		return mssql;
	}
	/**
	 * @param mssql The mssql to set.
	 */
	public void setMssql(String mssql)
	{
		this.mssql = mssql;
	}
	/**
	 * @return Returns the mysql.
	 */
	public String getMysql()
	{
		return mysql;
	}
	/**
	 * @param mysql The mysql to set.
	 */
	public void setMysql(String mysql)
	{
		this.mysql = mysql;
	}
	
	public String getNote()
	{
		return(date + " [" + author + "] " + desc);
	}
	/**
	 * @return Returns the oracle.
	 */
	public String getOracle()
	{
		return oracle;
	}
	/**
	 * @param oracle The oracle to set.
	 */
	public void setOracle(String oracle)
	{
		this.oracle = oracle;
	}
}
