package sk.iway.iwcm.calendar;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import sk.iway.iwcm.Constants;

/**
 *  formular na pridanie udalosti do kalendara
 *
 *@Title        magma-web
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.8 $
 *@created      Streda, 2002, február 20
 *@modified     $Date: 2004/02/17 22:34:03 $
 */
public class EventForm extends ActionForm
{
	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;
	private boolean recode = true;
	private int calendarId;
	private String title;
	private String dateFrom;
	private String dateTo;
	private int typeId;
	private String description;
	private String area;
	private String city;
	private String address;
	private String timeRange;
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
	private String lng = Constants.getString("defaultLanguage");
	private int creatorId;
	private int approve;
	private boolean suggest;
	private String approveCheckBox;
	private int domainId;

	/**
	 *  Description of the Method
	 *
	 *@param  input  Description of the Parameter
	 *@return        Description of the Return Value
	 */
	private String recode(String input)
	{
		if (input == null)
		{
			return ("");
		}
		//Logger.println(this,"Recoding: "+input);
		return (input.trim());
	}

	/**
	 *  Sets the recode attribute of the EventForm object
	 *
	 *@param  recode  The new recode value
	 */
	public void setRecode(boolean recode)
	{
		this.recode = recode;
	}

	/**
	 *  Gets the recode attribute of the EventForm object
	 *
	 *@return    The recode value
	 */
	public boolean isRecode()
	{
		return recode;
	}

	@Override
	public void reset(ActionMapping mapping, HttpServletRequest request)
	{
		this.notifySendSMS = false;
	}

	/**
	 *  Sets the calendarId attribute of the EventForm object
	 *
	 *@param  calendarId  The new calendarId value
	 */
	public void setCalendarId(int calendarId)
	{
		this.calendarId = calendarId;
	}

	/**
	 *  Gets the calendarId attribute of the EventForm object
	 *
	 *@return    The calendarId value
	 */
	public int getCalendarId()
	{
		return calendarId;
	}

	/**
	 *  Sets the title attribute of the EventForm object
	 *
	 *@param  title  The new title value
	 */
	public void setTitle(String title)
	{
		this.title = recode(title);
	}

	/**
	 *  Gets the title attribute of the EventForm object
	 *
	 *@return    The title value
	 */
	public String getTitle()
	{
		return title;
	}

	/**
	 *  Sets the dateFrom attribute of the EventForm object
	 *
	 *@param  dateFrom  The new dateFrom value
	 */
	public void setDateFrom(String dateFrom)
	{
		this.dateFrom = dateFrom;
	}

	/**
	 *  Gets the dateFrom attribute of the EventForm object
	 *
	 *@return    The dateFrom value
	 */
	public String getDateFrom()
	{
		return dateFrom;
	}

	/**
	 *  Sets the dateTo attribute of the EventForm object
	 *
	 *@param  dateTo  The new dateTo value
	 */
	public void setDateTo(String dateTo)
	{
		this.dateTo = dateTo;
	}

	/**
	 *  Gets the dateTo attribute of the EventForm object
	 *
	 *@return    The dateTo value
	 */
	public String getDateTo()
	{
		return dateTo;
	}

	/**
	 *  Sets the type attribute of the EventForm object
	 *
	 *@param  typeId  The new type value
	 */
	public void setTypeId(int typeId)
	{
	 this.typeId = typeId;
	}

	/**
	 *  Gets the type attribute of the EventForm object
	 *
	 *@return    The type value
	 */
	public int getTypeId()
	{
	 return typeId;
	}
  public String getDescription() {
	 return description;
  }
  public void setDescription(String description) {
	 this.description = description;
  }
  public String getArea() {
	 return area;
  }
  public void setArea(String area) {
	 this.area = area;
  }
  public String getCity() {
	 return city;
  }
  public void setCity(String city) {
	 this.city = city;
  }
  public String getAddress() {
	 return address;
  }
  public void setAddress(String address) {
	 this.address = address;
  }
  public String getTimeRange() {
	 return timeRange;
  }
  public void setTimeRange(String timeRange) {
	 this.timeRange = timeRange;
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

	public int getApprove()
	{
		return approve;
	}

	public void setApprove(int approve)
	{
		this.approve = approve;
	}

	public void setCreatorId(int creatorId)
	{
		this.creatorId = creatorId;
	}

	public boolean isSuggest()
	{
		return suggest;
	}

	public void setSuggest(boolean suggest)
	{
		this.suggest = suggest;
	}

	public String getApproveCheckBox() {
		return approveCheckBox;
	}

	public void setApproveCheckBox(int ap) {
		if(ap==1)
			this.approveCheckBox = "true";
		else
			this.approveCheckBox = "false";
	}

	public int getDomainId() {
		return domainId;
	}

	public void setDomainId(int domainId) {
		this.domainId = domainId;
	}

}
