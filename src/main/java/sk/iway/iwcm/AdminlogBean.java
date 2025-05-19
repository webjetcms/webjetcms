package sk.iway.iwcm;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 *  AdminlogBean.java - riadok tabulky _adminlog_
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2006
 *@author       $Author: jeeff $
 *@version      $Revision: 1.1 $
 *@created      Date: 14.7.2006 13:32:53
 *@modified     $Date: 2006/07/21 14:57:13 $
 */
public class AdminlogBean
{
	private int logId;
	private int logType;
	private int userId;
	private Timestamp createDate;
	private String description;
	private int subId1;
	private int subId2;
	private String ip;
	private String hostname;
	
	public AdminlogBean()
	{
		
	}
	
	public AdminlogBean(ResultSet rs) throws SQLException
	{
		logId = rs.getInt("log_id");
		logType = rs.getInt("log_type");
		userId = rs.getInt("user_id");
		createDate = rs.getTimestamp("create_date");
		description = DB.getDbString(rs, "description");
		subId1 = rs.getInt("sub_id1");
		subId2 = rs.getInt("sub_id2");
		ip = DB.getDbString(rs, "ip");
		hostname = DB.getDbString(rs, "hostname");
	}
	
	public Timestamp getCreateDate()
	{
		return createDate;
	}
	public void setCreateDate(Timestamp createDate)
	{
		this.createDate = createDate;
	}
	public String getDescription()
	{
		return description;
	}
	public void setDescription(String description)
	{
		this.description = description;
	}
	public String getHostname()
	{
		return hostname;
	}
	public void setHostname(String hostname)
	{
		this.hostname = hostname;
	}
	public String getIp()
	{
		return ip;
	}
	public void setIp(String ip)
	{
		this.ip = ip;
	}
	public int getLogId()
	{
		return logId;
	}
	public void setLogId(int logId)
	{
		this.logId = logId;
	}
	public int getLogType()
	{
		return logType;
	}
	public void setLogType(int logType)
	{
		this.logType = logType;
	}
	public int getSubId1()
	{
		return subId1;
	}
	public void setSubId1(int subId1)
	{
		this.subId1 = subId1;
	}
	public int getSubId2()
	{
		return subId2;
	}
	public void setSubId2(int subId2)
	{
		this.subId2 = subId2;
	}
	public int getUserId()
	{
		return userId;
	}
	public void setUserId(int userId)
	{
		this.userId = userId;
	}
}
