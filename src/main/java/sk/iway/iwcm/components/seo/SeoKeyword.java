package sk.iway.iwcm.components.seo;

import java.util.Date;

import sk.iway.iwcm.users.UserDetails;

public class SeoKeyword
{
	private int seoKeywordId;
	private String name;
	private String domain;
	private Date createdTime;
	private UserDetails author;
	private String searchBot;
	
	public String getDomain()
	{
		return domain;
	}
	public void setDomain(String domain)
	{
		this.domain = domain;
	}
	
	public int getSeoKeywordId()
	{
		return seoKeywordId;
	}
	public void setSeoKeywordId(int seoKeywordId)
	{
		this.seoKeywordId = seoKeywordId;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public Date getCreatedTime()
	{
		return createdTime;
	}
	public void setCreatedTime(Date createdTime)
	{
		this.createdTime = createdTime;
	}
	public UserDetails getAuthor()
	{
		return author;
	}
	public void setAuthor(UserDetails author)
	{
		this.author = author;
	}
	public String getSearchBot()
	{
		return searchBot;
	}
	public void setSearchBot(String searchBot)
	{
		this.searchBot = searchBot;
	}
}
