package sk.iway.iwcm.components.banner.model;

import java.io.Serializable;
import java.util.Date;


public class BannerStatViews implements Serializable 
{

	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3669221437223374580L;

	public static final String FIELD_INSERT_DATE = "insert_date";

   private int id;
	private Integer bannerId;
   private Integer views;
   private String host;
   private Date insertDate = null;
   private String ip;
   
	public int getId()
	{
		return id;
	}
	public void setId(int id)
	{
		this.id = id;
	}
	public Integer getBannerId()
	{
		return bannerId;
	}
	public void setBannerId(Integer bannerId)
	{
		this.bannerId = bannerId;
	}
	public Integer getViews()
	{
		return views;
	}
	public void setViews(Integer views)
	{
		this.views = views;
	}
	public String getHost()
	{
		return host;
	}
	public void setHost(String host)
	{
		this.host = host;
	}
	public Date getInsertDate()
	{
		return insertDate == null ? null : (Date) insertDate.clone();
	}
	public void setInsertDate(Date insertDate)
	{
		this.insertDate = insertDate == null ? null : (Date) insertDate.clone();
	}
	public String getIp()
	{
		return ip;
	}
	public void setIp(String ip)
	{
		this.ip = ip;
	}
	
   
}



