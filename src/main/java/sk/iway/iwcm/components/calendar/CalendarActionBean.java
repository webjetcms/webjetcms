package sk.iway.iwcm.components.calendar;

import static sk.iway.iwcm.system.stripes.WebJETActionBean.RESOLUTION_CLOSE;
import static sk.iway.iwcm.system.stripes.WebJETActionBean.RESOLUTION_CONTINUE;

import java.util.List;

import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.DontValidate;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.SpamProtection;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.calendar.CalendarDB;
import sk.iway.iwcm.calendar.CalendarDetails;
import sk.iway.iwcm.calendar.CalendarInvitationDetails;
import sk.iway.iwcm.calendar.EventForm;
import sk.iway.iwcm.calendar.EventTypeDB;
import sk.iway.iwcm.calendar.EventTypeDetails;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.users.SettingsBean;

/**
 *  CalendarActionBean.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: bhric $
 *@version      $Revision: 1.2 $
 *@created      Date: 23.1.2009 13:53:50
 *@modified     $Date: 2009/08/04 07:51:02 $
 */
public class CalendarActionBean implements ActionBean
{
	private ActionBeanContext context;
	private int userId;
	private int calendarId;
	private String akcia;
	private EventForm event = new EventForm();
	private EventTypeDetails eventType = new EventTypeDetails();
	private int approveEventId;
	private int approveStatus;
	private String odporuceneId;
	private int odporucitAkcia;
	private String vyraz;
	private String region;
	private int[] eventTypes;
	private String domain;
	private String approverMail;

	@Override
	public ActionBeanContext getContext()
	{
		return context;
	}
	@Override
   public void setContext(ActionBeanContext context)
   {
 	   this.context = context;
 	   if (context.getRequest().getParameter("typeId") != null)
		{
 	   	int typeId = Tools.getIntValue(context.getRequest().getParameter("typeId"), -1);
 	   	if(typeId != -1)
 	   		this.eventType = EventTypeDB.getTypeById(typeId);
		}
   }
	public int getUserId()
	{
		return userId;
	}
	public void setUserId(int userId)
	{
		this.userId = userId;
	}
	public int getCalendarId()
	{
		return calendarId;
	}
	public void setCalendarId(int calendarId)
	{
		this.calendarId = calendarId;
	}
	public String getAkcia()
	{
		return akcia;
	}
	public void setAkcia(String akcia)
	{
		this.akcia = akcia;
	}
	public EventForm getEvent()
	{
		return event;
	}
	public void setEvent(EventForm event)
	{
		this.event = event;
	}
	public EventTypeDetails getEventType()
	{
		return eventType;
	}
	public void setEventType(EventTypeDetails eventType)
	{
		this.eventType = eventType;
	}
	public int getApproveEventId()
	{
		return approveEventId;
	}
	public void setApproveEventId(int approveEventId)
	{
		this.approveEventId = approveEventId;
	}
	public int getApproveStatus()
	{
		return approveStatus;
	}
	public void setApproveStatus(int approveStatus)
	{
		this.approveStatus = approveStatus;
	}
	public String getOdporuceneId()
	{
		return odporuceneId;
	}
	public void setOdporuceneId(String odporuceneId)
	{
		this.odporuceneId = odporuceneId;
	}
	public int getOdporucitAkcia()
	{
		return odporucitAkcia;
	}
	public void setOdporucitAkcia(int odporucitAkcia)
	{
		this.odporucitAkcia = odporucitAkcia;
	}
	public String getVyraz()
	{
		return vyraz;
	}
	public void setVyraz(String vyraz)
	{
		this.vyraz = vyraz;
	}
	public String getRegion()
	{
		return region;
	}
	public void setRegion(String region)
	{
		this.region = region;
	}
	public void setEventTypes(int[] eventTypes)
	{
		this.eventTypes = eventTypes;
	}

	@DefaultHandler
	@DontValidate
   @HandlesEvent("saveRemoveUserId")
   public Resolution saveRemoveUserId()
   {
		boolean saveOk = false;
		if(userId != -1)
		{
			CalendarInvitationDetails inv = CalendarDB.getInvitationByUser(calendarId, userId);
			if(akcia.equals("save"))
			{
				if(inv == null)
				{
					saveOk = CalendarDB.saveCalendarInvitation(calendarId, userId);
					if(saveOk)
					{
						inv = CalendarDB.getInvitationByUser(calendarId, userId);
						saveOk = CalendarDB.setCalendarInvitationStatus(inv.getCalendarInvitationId(), "A");
					}
				}
				else
				{
					saveOk = CalendarDB.setCalendarInvitationStatus(inv.getCalendarInvitationId(), "A");
				}
			}
			else if(akcia.equals("remove"))
			{
				if(inv == null)
				{
					saveOk = CalendarDB.saveCalendarInvitation(calendarId, userId);
					if(saveOk)
					{
						inv = CalendarDB.getInvitationByUser(calendarId, userId);
						saveOk = CalendarDB.setCalendarInvitationStatus(inv.getCalendarInvitationId(), "D");
					}
				}
				else
				{
					saveOk = CalendarDB.setCalendarInvitationStatus(inv.getCalendarInvitationId(), "D");
				}
			}
		}

		context.getRequest().setAttribute("saveOk", saveOk);
 		context.getRequest().setAttribute("calendarId", calendarId);
	   return new ForwardResolution(RESOLUTION_CONTINUE);
   }

	@DontValidate
   @HandlesEvent("pridajAkciu")
   public Resolution pridajAkciu()
   {
		EventTypeDetails eventTypeDetails = EventTypeDB.getTypeById(event.getTypeId());
		String lng = PageLng.getUserLng(context.getRequest());
		Prop prop = Prop.getInstance(lng);
		if(eventTypeDetails.getSchvalovatelId() == -1)
			event.setApprove(1);
		else
			event.setApprove(-1);
		boolean saveOk = CalendarDB.saveEventToDB(event);
		if(saveOk)
		{
			context.getRequest().setAttribute("saveOk",  prop.getText("components.calendar.akciaSaved"));
			return new ForwardResolution(RESOLUTION_CONTINUE);
		}
		else{
			context.getRequest().setAttribute("errorText", prop.getText("editor.perex_group.chyba_pri_ukladani"));
			return new ForwardResolution(RESOLUTION_CONTINUE);
		}
   }

	@DontValidate
   @HandlesEvent("editujTyp")
   public Resolution editujTyp()
   {
		int ret = EventTypeDB.updateType(eventType);
		String lng = PageLng.getUserLng(context.getRequest());
		Prop prop = Prop.getInstance(lng);
		boolean saveOk = false;
		if(ret == -1)
			saveOk = false;
		else
			saveOk = true;
		if(saveOk)
		{
			context.getRequest().setAttribute("customScript",  "alert('"+prop.getText("calendar_edit.configType.saved")+"');window.opener.location.reload();");
			return new ForwardResolution(RESOLUTION_CLOSE);
		}
		else{
			context.getRequest().setAttribute("errorText", prop.getText("editor.perex_group.chyba_pri_ukladani"));
			return new ForwardResolution(RESOLUTION_CONTINUE);
		}
   }

	@DontValidate
   @HandlesEvent("schvalujem")
   public Resolution schvalujem()
   {
		boolean saveOk = CalendarDB.saveApproveStatus(approveEventId, approveStatus, true);
		String lng = PageLng.getUserLng(context.getRequest());
		Prop prop = Prop.getInstance(lng);
		if(saveOk)
		{
			context.getRequest().setAttribute("customScript",  "alert(\""+prop.getText("components.calendar.akciaSaved")+"\");window.opener.location.reload();");
			return new ForwardResolution(RESOLUTION_CLOSE);
		}
		else{
			context.getRequest().setAttribute("errorText", prop.getText("editor.perex_group.chyba_pri_ukladani"));
			return new ForwardResolution(RESOLUTION_CONTINUE);
		}

   }

	@DontValidate
   @HandlesEvent("odporucujem")
   public Resolution odporucujem()
   {
		boolean saveOk = false;
		String lng = PageLng.getUserLng(context.getRequest());
		Prop prop = Prop.getInstance(lng);
		if(Tools.isNotEmpty(odporuceneId))
		{
			saveOk = CalendarDB.setSuggestEvents(odporucitAkcia, odporuceneId);
		}
		else
		{
			context.getRequest().setAttribute("errorText", prop.getText("calendar.check_event_error"));
			return new ForwardResolution(RESOLUTION_CONTINUE);
		}
		if(saveOk)
		{
			context.getRequest().setAttribute("saveOk",saveOk);
			return new ForwardResolution(RESOLUTION_CONTINUE);
		}
		else
		{
			context.getRequest().setAttribute("errorText", prop.getText("editor.perex_group.chyba_pri_ukladani"));
			return new ForwardResolution(RESOLUTION_CONTINUE);
		}

   }

	@DontValidate
   @HandlesEvent("fullTextSearch")
   public Resolution fullTextSearch()
   {
		Identity user = (Identity)context.getRequest().getSession().getAttribute(Constants.USER_KEY);
		boolean zobrazVsetky = true;
		if (user != null)
		{
			SettingsBean sbPerms = user.getSettings().get("kaZobrazujAkcie");
			if (sbPerms!=null)
			{
				if("vsetky".equals(sbPerms.getSvalue1()))
					zobrazVsetky = true;
				else
					zobrazVsetky = false;
			}
		}

		if(Tools.isNotEmpty(vyraz))
		{
			List<CalendarDetails> events = CalendarDB.fullTextSearch(vyraz);
			context.getRequest().setAttribute("events",events);
		}
		else
		{
			List<CalendarDetails> events = null;
			if(zobrazVsetky)
				events = CalendarDB.getEventsByRegionAndType(getContext().getRequest());
			else
			{
				//VUB: todo - toto treba znova odkomentovat po prechode VUB na WJ7
				//events = KalendarAkciiDB.getVubAkcie(getContext().getRequest());
			}
			context.getRequest().setAttribute("events",events);
		}
		return new ForwardResolution(RESOLUTION_CONTINUE);
   }

	@DontValidate
   @HandlesEvent("filtruj")
   public Resolution filtruj()
   {
		String lng = PageLng.getUserLng(context.getRequest());
		Prop prop = Prop.getInstance(lng);
		if(eventTypes != null && eventTypes.length > 0)
		{
			context.getRequest().setAttribute("calStart", Tools.formatDate(Tools.getNow()));
			context.getRequest().removeAttribute("calEnd");
			context.getRequest().setAttribute("showApprove", "1");
			context.getRequest().removeAttribute("suggest");
			List<CalendarDetails> events = CalendarDB.getEventsByRegionAndType(context.getRequest(), eventTypes, region);
			context.getRequest().setAttribute("events",events);
			return new ForwardResolution(RESOLUTION_CONTINUE);
		}
		else{
			context.getRequest().setAttribute("errorText", prop.getText("components.calendar.empty_search_term"));
			return new ForwardResolution(RESOLUTION_CONTINUE);
		}

   }

		@DontValidate
	   @HandlesEvent("pridajAkciuVerejnost")
	   public Resolution pridajAkciuVerejnost()
	   {
			String lng = PageLng.getUserLng(context.getRequest());
			Prop prop = Prop.getInstance(lng);

			if (!SpamProtection.canPost("form", null, context.getRequest()))
	   		{
	   			Adminlog.add(Adminlog.TYPE_FORMMAIL, "detectSpam (ReviewsAction.bSave()) TRUE: can't post", -1, -1);
	   			//setErrorText("Formulár bol detekovaný ako SPAM");
	   			context.getRequest().setAttribute("errorText", prop.getText("checkform.fail_probablySpamBot"));
	   			return (new ForwardResolution("/components/maybeError.jsp"));
	   		}
			event.setApprove(-1);

			if(event.getTimeRange() == null) event.setTimeRange("");
			if(event.getArea() == null) event.setArea("");
			if(event.getAddress() == null) event.setAddress("");
			if(event.getInfo1() == null) event.setInfo1("");
			if(event.getInfo2() == null) event.setInfo2("");
			if(event.getInfo3() == null) event.setInfo3("");
			if(event.getInfo4() == null) event.setInfo4("");
			if(event.getInfo5() == null) event.setInfo5("");
			if(event.getLng() == null) event.setLng("");
			if(event.getDescription() == null) event.setDescription("");

			if(event.getNotifyEmails() == null) event.setNotifyEmails("");
			if(event.getNotifySender() == null) event.setNotifySender("");
			if(event.getNotifyIntrotext() == null) event.setNotifyIntrotext("");

			event.setTitle(DB.filterHtml(event.getTitle()));
			event.setDateFrom(DB.filterHtml(event.getDateFrom()));
			event.setDateTo(DB.filterHtml(event.getDateTo()));
			event.setTimeRange(DB.filterHtml(event.getTimeRange()));
			event.setCity(DB.filterHtml(event.getCity()));
			event.setAddress(DB.filterHtml(event.getAddress()));
			event.setDescription(DB.filterHtml(event.getDescription()));

			boolean saveOk = CalendarDB.saveEventPublicToDB(event, domain, approverMail, prop);
			if(saveOk)
			{
				context.getRequest().setAttribute("saveOk",  prop.getText("components.calendar.akciaSaved"));
				return new ForwardResolution(RESOLUTION_CONTINUE);
			}
			else{
				context.getRequest().setAttribute("errorText", prop.getText("editor.perex_group.chyba_pri_ukladani"));
				return new ForwardResolution(RESOLUTION_CONTINUE);
			}
	   }
		public String getDomain() {
			return domain;
		}
		public void setDomain(String domain) {
			this.domain = domain;
		}

		public String getApproverMail() {
			return approverMail;
		}
		public void setApproverMail(String approverMail) {
			this.approverMail = approverMail;
		}

}