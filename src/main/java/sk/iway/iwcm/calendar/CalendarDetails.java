package sk.iway.iwcm.calendar;

import java.text.SimpleDateFormat;
import java.util.Date;

import sk.iway.iwcm.Constants;

/**
 *  drzi riadok z tabulky calendar
 *
 *@Title        magma-web
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.6 $
 *@created      Utorok, 2002, február 19
 *@modified     $Date: 2004/02/17 21:37:28 $
 */
public class CalendarDetails
{
	private int calendarId;
	private String title;
	private java.sql.Timestamp from;
	private java.sql.Timestamp to;
	private int typeId;
	private long l_from, l_to;

	private SimpleDateFormat formatter = new SimpleDateFormat(Constants.getString("dateFormat"));
	private int bgColorIndex=0;
	private String type;
	private String timeRange;
	private String area;
	private String city;
	private String address;
	private String description;
	private String info1;
	private String info2;
	private String info3;
	private String info4;
	private String info5;
	private int notifyHoursBefore;
	private String notifyEmails;
	private String notifySender;
	private String notifyIntrotext;
	private boolean notifySendSMS;
	private String lng;
	private int creatorId;
	private int approve;
	private boolean suggest;
	private String hashString;
	private int domainId;

	/**
	 *  Gets the calendarId attribute of the CalendarDetails object
	 *
	 *@return    The calendarId value
	 */
	public int getCalendarId()
	{
		return calendarId;
	}

	/**
	 *  Sets the calendarId attribute of the CalendarDetails object
	 *
	 *@param  calendarId  The new calendarId value
	 */
	public void setCalendarId(int calendarId)
	{
		this.calendarId = calendarId;
	}

	/**
	 *  Sets the title attribute of the CalendarDetails object
	 *
	 *@param  title  The new title value
	 */
	public void setTitle(String title)
	{
		this.title = title;
	}

	/**
	 *  Gets the title attribute of the CalendarDetails object
	 *
	 *@return    The title value
	 */
	public String getTitle()
	{
		return title;
	}

	/**
	 *  Sets the from attribute of the CalendarDetails object
	 *
	 *@param  from  The new from value
	 */
	public void setFrom(java.sql.Timestamp from)
	{
		this.from = from;
		l_from=from.getTime();
	}

	/**
	 *  Gets the from attribute of the CalendarDetails object
	 *
	 *@return    The from value
	 */
	public java.sql.Timestamp getFrom()
	{
		return from;
	}

	public long getFromLong()
	{
		return(l_from);
	}

	public String getFromString()
	{
		return(formatter.format(new Date(l_from)));
	}

	/**
	 *  Sets the to attribute of the CalendarDetails object
	 *
	 *@param  to  The new to value
	 */
	public void setTo(java.sql.Timestamp to)
	{
		this.to = to;
		l_to = to.getTime();
	}

	/**
	 *  Gets the to attribute of the CalendarDetails object
	 *
	 *@return    The to value
	 */
	public java.sql.Timestamp getTo()
	{
		return to;
	}

	public long getToLong()
	{
		return(l_to);
	}

	public String getToString()
	{
		return(formatter.format(new Date(l_to)));
	}

	/**
	 *  Sets the type attribute of the CalendarDetails object
	 *
	 *@param  typeId  The new type value
	 */
	public void setTypeId(int typeId)
	{
	 this.typeId = typeId;
	}

	/**
	 *  Gets the type attribute of the CalendarDetails object
	 *
	 *@return    The type value
	 */
	public int getTypeId()
	{
	 return typeId;
	}
	public void setBgColorIndex(int bgColorIndex)
	{
		this.bgColorIndex = bgColorIndex;
	}
	public int getBgColorIndex()
	{
		return bgColorIndex;
	}

	public String getCircleImgString(jakarta.servlet.http.HttpServletRequest request)
	{
	  if (typeId <= 5)
		 return "<img src='/images/kruh_" + typeId + ".gif'>";
	  else
		 return EventTypeDB.getTypeName(typeId, request);
	}
	public String getType()
	{
		return type;
	}
	public void setType(String type)
	{
		this.type = type;
	}
	public String getTimeRange()
	{
		return timeRange;
	}
	public void setTimeRange(String timeRange)
	{
		this.timeRange = timeRange;
	}
	public String getArea()
	{
		return area;
	}
	public void setArea(String area)
	{
		this.area = area;
	}
	public String getCity()
	{
		return city;
	}
	public void setCity(String city)
	{
		this.city = city;
	}
	public String getAddress()
	{
		return address;
	}
	public void setAddress(String address)
	{
		this.address = address;
	}
  public String getDescription() {
	 return description;
  }
  public void setDescription(String description) {
	 this.description = description;
  }
	public String getInfo1()
	{
		return info1;
	}
	public void setInfo1(String info1)
	{
		this.info1 = info1;
	}
	public String getInfo2()
	{
		return info2;
	}
	public void setInfo2(String info2)
	{
		this.info2 = info2;
	}
	public String getInfo3()
	{
		return info3;
	}
	public void setInfo3(String info3)
	{
		this.info3 = info3;
	}
	public String getInfo4()
	{
		return info4;
	}
	public void setInfo4(String info4)
	{
		this.info4 = info4;
	}
	public String getInfo5()
	{
		return info5;
	}
	public void setInfo5(String info5)
	{
		this.info5 = info5;
	}
	public int getNotifyHoursBefore()
	{
		return notifyHoursBefore;
	}
	public void setNotifyHoursBefore(int notifyHoursBefore)
	{
		this.notifyHoursBefore = notifyHoursBefore;
	}
	public String getNotifyEmails()
	{
		return notifyEmails;
	}
	public void setNotifyEmails(String notifyEmails)
	{
		this.notifyEmails = notifyEmails;
	}
	public String getNotifySender()
	{
		return notifySender;
	}
	public void setNotifySender(String notifySender)
	{
		this.notifySender = notifySender;
	}
	public String getNotifyIntrotext()
	{
		return notifyIntrotext;
	}
	public void setNotifyIntrotext(String notifyIntrotext)
	{
		this.notifyIntrotext = notifyIntrotext;
	}
	public boolean isNotifySendSMS()
	{
		return notifySendSMS;
	}
	public void setNotifySendSMS(boolean notifySendSMS)
	{
		this.notifySendSMS = notifySendSMS;
	}
	public String getLng()
	{
		return lng;
	}
	public void setLng(String lng)
	{
		this.lng = lng;
	}

	public int getCreatorId()
	{
		return creatorId;
	}

	public void setCreatorId(int creatorId)
	{
		this.creatorId = creatorId;
	}

	public int getApprove()
	{
		return approve;
	}

	public void setApprove(int approve)
	{
		this.approve = approve;
	}

	public boolean isSuggest()
	{
		return suggest;
	}

	public void setSuggest(boolean suggest)
	{
		this.suggest = suggest;
	}

	public String getHashString() {
		return hashString;
	}

	public void setHashString(String hashString) {
		this.hashString = hashString;
	}

	public int getDomainId() {
		return domainId;
	}

	public void setDomainId(int domainId) {
		this.domainId = domainId;
	}

}
