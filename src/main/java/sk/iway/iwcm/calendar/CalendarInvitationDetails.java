package sk.iway.iwcm.calendar;

import java.util.Date;

import sk.iway.iwcm.users.UserDetails;

/**
 *  CalendarInvitationDetails.java - riadok tabulky calendar_invitation
 *
 *@Title        webjet5
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2007
 *@author       $Author: jeeff $
 *@version      $Revision: 1.1 $
 *@created      Date: 2.5.2007 16:25:12
 *@modified     $Date: 2007/05/16 09:27:45 $
 */
public class CalendarInvitationDetails
{
	int calendarInvitationId;
	int calendarId;
	int userId;
	UserDetails user;
	Date sentDate = null;
	Date statusDate = null;
	String status;
	
	public String getStatusKey()
	{
		return("calendar.invitation.status-"+status);
	}
	
	public int getCalendarId()
	{
		return calendarId;
	}
	public void setCalendarId(int calendarId)
	{
		this.calendarId = calendarId;
	}
	public int getCalendarInvitationId()
	{
		return calendarInvitationId;
	}
	public void setCalendarInvitationId(int calendarInvitationId)
	{
		this.calendarInvitationId = calendarInvitationId;
	}
	public Date getSentDate()
	{
		return sentDate == null ? null : (Date)sentDate.clone();
	}
	public void setSentDate(Date sentDate)
	{
		this.sentDate = sentDate == null ? null : (Date)sentDate.clone();
	}
	public String getStatus()
	{
		return status;
	}
	public void setStatus(String status)
	{
		this.status = status;
	}
	public Date getStatusDate()
	{
		return statusDate == null ? null : (Date)statusDate.clone();
	}
	public void setStatusDate(Date statusDate)
	{
		this.statusDate = statusDate == null ? null : (Date)statusDate.clone();
	}
	public UserDetails getUser()
	{
		return user;
	}
	public void setUser(UserDetails user)
	{
		this.user = user;
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
