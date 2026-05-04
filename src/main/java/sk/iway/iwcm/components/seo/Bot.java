package sk.iway.iwcm.components.seo;

import java.util.Date;

public class Bot implements Comparable<Bot>
{
	private int botId;
	private String name;
	private int visits;
	private Date lastVisit;
	
	public int getBotId()
	{
		return botId;
	}
	public void setBotId(int botId)
	{
		this.botId = botId;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public int getVisits()
	{
		return visits;
	}
	public void setVisits(int visits)
	{
		this.visits = visits;
	}
	public Date getLastVisit()
	{
		return lastVisit;
	}
	public void setLastVisit(Date lastVisit)
	{
		this.lastVisit = lastVisit;
	}
	@Override
	public int compareTo(Bot o)
	{
		return ( o.getVisits() - this.getVisits());
	}
	@Override
	public boolean equals(Object o)
	{
		if (o instanceof Bot && compareTo((Bot)o)==0) return true;
		return false;
	}
	@Override
	public int hashCode()
	{
		return name.hashCode();
	}
}
