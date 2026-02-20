package sk.iway.iwcm.components.reservation;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidateNestedProperties;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.stripes.WebJETActionBean;
import sk.iway.iwcm.users.UsersDB;

/**
 * ReservationAjaxAction.java - editacia ReservationBean a ReservationObjectBean
 * zaznamu
 *
 * @Title webjet4
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2008
 * @author $Author: jeeff $
 * @version $Revision: 1.6 $
 * @created Date: 20.12.2008 14:55:51
 * @modified $Date: 2009/10/23 13:35:39 $
 */
public class ReservationAjaxAction extends WebJETActionBean
{
	@ValidateNestedProperties({@Validate(field = "reservationId", on = "saveReservation"),
				@Validate(field = "reservationObjectId", required = true, on = "saveReservation"),
				@Validate(field = "dateFrom", required = true, on = "saveReservation"),
				@Validate(field = "startTime", required = true, on = "saveReservation"),
				@Validate(field = "dateTo", required = true, on = "saveReservation"),
				@Validate(field = "finishTime", required = true, on = "saveReservation"),
				@Validate(field = "name", required = true, on = "saveReservation"),
				@Validate(field = "surname", required = true, on = "saveReservation"),
				@Validate(field = "email", required = true, on = "saveReservation"),
				@Validate(field = "purpose", required = true, on = "saveReservation"),
				@Validate(field = "accepted", on = "saveReservation"), @Validate(field = "hashValue", on = "saveReservation"),
				@Validate(field = "phoneNumber", on = "saveReservation")})
	private ReservationBean reservation;
	@ValidateNestedProperties({@Validate(field = "reservationObjectId", on = "bSaveReservationObject"),
				@Validate(field = "name", required = true, on = "bSaveReservationObject"),
				@Validate(field = "mustAccepted", on = "bSaveReservationObject"),
				@Validate(field = "emailAccepter", on = "bSaveReservationObject"), @Validate(field = "passwd"),
				@Validate(field = "priceForDay", on = "bSaveReservationObject"),
				@Validate(field = "priceForHour", on = "bSaveReservationObject"),
				@Validate(field = "reservationForAllDay", on = "bSaveReservationObject"),
				@Validate(field = "photoLink", on = "bSaveReservationObject"),
				@Validate(field = "description", on = "bSaveReservationObject")})
	private ReservationObjectBean reservationObject;
	int reservationObjectId;

	//rzapach: rozsah casov pre jednotlive zvolene dni
	boolean chooseDays = false;
	boolean[] days = new boolean[7];
	String[] casyOd = {"08:00", "08:00", "08:00", "08:00", "08:00", "08:00", "08:00"};
	String[] casyDo = {"16:00", "16:00", "16:00", "16:00", "16:00", "16:00", "16:00"};
	List<ReservationObjectTimesBean> times = new ArrayList<>();

	@Override
	public void setContext(ActionBeanContext context)
	{
		super.setContext(context);
		reservationObjectId = Tools.getIntValue(context.getRequest().getParameter("reservationObject.reservationObjectId"), -1);
		if (reservationObjectId > 0)
		{
			reservationObject = ReservationManager.getReservationObjectById(reservationObjectId);

			times = ReservationObjectTimesDB.getInstance().getByReservationObjectId(reservationObjectId);
			if(times.size()>0)
				chooseDays = true;
			for(ReservationObjectTimesBean rotb : times)
			{
				days[rotb.getDen()-1] = true;
				casyOd[rotb.getDen()-1] = rotb.getCasOd();
				casyDo[rotb.getDen()-1] = rotb.getCasDo();
			}

			//20897 - pridana editacia konkretnej rezervacie
			int reservationId = Tools.getIntValue(Tools.getParameter(getRequest(), "reservation.reservationId"), -1);
			if(reservationId>0)
				reservation = ReservationManager.getReservationById(reservationId);
		}
		else
		{
			reservationObject = new ReservationObjectBean();
			reservationObject.setTimeUnit("30");
		}
		Identity user = UsersDB.getCurrentUser(context.getRequest());
		if (getReservation() == null || getReservation().getReservationId() < 1)
		{
			if (getReservation() == null)
				setReservation(new ReservationBean());
			if (user != null)
			{
				if (Tools.isEmpty(getReservation().getName()))
					getReservation().setName(user.getFirstName());
				if (Tools.isEmpty(getReservation().getSurname()))
					getReservation().setSurname(user.getLastName());
				if (Tools.isEmpty(getReservation().getEmail()))
					getReservation().setEmail(user.getEmail());
			}
			if (getReservation().getDateFrom() == null)
				getReservation().setDateFrom(new Date());
			if (getReservation().getDateTo() == null)
				getReservation().setDateTo(new Date());
		}
	}

	public ReservationBean getReservation()
	{
		return reservation;
	}

	public void setReservation(ReservationBean reservation)
	{
		this.reservation = reservation;
	}

	private Date filterDateFrom = null;
	private Date filterDateTo = null;
	private int filterObjectId;
	private String filterEmail;

	public int getFilterObjectId()
	{
		return filterObjectId;
	}

	public void setFilterObjectId(int filterObjectId)
	{
		this.filterObjectId = filterObjectId;
	}

	public Date getFilterDateFrom()
	{
		return filterDateFrom == null ? null : (Date) filterDateFrom.clone();
	}

	public void setFilterDateFrom(Date filterDateFrom)
	{
		this.filterDateFrom = filterDateFrom == null ? null : (Date) filterDateFrom.clone();
	}

	public Date getFilterDateTo()
	{
		return filterDateTo == null ? null : (Date) filterDateTo.clone();
	}

	public void setFilterDateTo(Date filterDateTo)
	{
		this.filterDateTo = filterDateTo == null ? null : (Date) filterDateTo.clone();
	}

	public String getFilterEmail()
	{
		return filterEmail;
	}

	public void setFilterEmail(String filterEmail)
	{
		this.filterEmail = filterEmail;
	}

	/**
	 * Vrati zoznam vsetkych sucasnych rezervacii podla ID a datumov
	 *
	 * @return
	 */
	public List<ReservationBean> getAllReservations()
	{
		return Collections.unmodifiableList(ReservationManager.getReservations(filterDateFrom, filterDateTo, filterObjectId));
	}

	/**
	 * Vrati zoznam vsetkych sucasnych rezervacnych objektov vyfiltrovanych podla
	 * emailu
	 *
	 * @return
	 */
	public List<ReservationObjectBean> getAllReservationObjects()
	{
		return Collections.unmodifiableList(ReservationManager.getReservationObjects(filterEmail));
	}

	/**
	 * Vrati zoznam rezervacii vyfiltrovany podla objektov, vrati tie rezervacie
	 * ktorych objekty sa nachadzaju v popskytnutom zozname
	 *
	 * @param objects
	 *           rezervacne objekty
	 * @return
	 */
	public List<ReservationBean> getAllReservationsByObjects(List<ReservationObjectBean> objects)
	{
		List<ReservationBean> original = getAllReservations();
		List<ReservationBean> filtered = new ArrayList<>(original.size());
		for (ReservationBean rb : original)
		{
			for (ReservationObjectBean o : objects)
			{
				if (o.getReservationObjectId() == rb.getReservationObjectId())
				{
					filtered.add(rb);
					break;
				}
			}
		}
		original = filtered;
		return original;
	}

	/**
	 * Vrati zoznam vsetkych emailovych adries schvalovatelov
	 *
	 * @return
	 */
	public List<String> getAllEmails()
	{
		return Collections.unmodifiableList(ReservationManager.getAccepterEmails());
	}

	@DefaultHandler
	public Resolution index()
	{
		return new RedirectResolution("/components/reservation/reservation_list.jsp");
	}

	/**
	 * Ulozenie rezervacie do databazy
	 *
	 * @return
	 * @throws ParseException
	 */
	public Resolution bSaveReservation() throws ParseException
	{
		Prop prop = Prop.getInstance(getRequest());
		ReservationObjectBean rob = ReservationManager.getReservationObject(reservation.getReservationObjectId());
		if (ReservationManager.isConflict(reservation) >= rob.getMaxReservations())
		{
			getRequest().setAttribute("errorText",
						prop.getText("components.reservation.reservation_manager.addReservation.conflict"));
			return (new ForwardResolution("/components/maybeError.jsp"));
		}
		if (!isStartBeforeEnd())
		{
			getRequest().setAttribute("errorText",
						prop.getText("components.reservation.reservation_manager.addReservation.endBeforeStart"));
			return (new ForwardResolution("/components/maybeError.jsp"));
		}
		// otesujem ci nezadava mimo cas na to urceny
		long compareDateStart = DB.getTimestamp(Tools.formatDate(Tools.getNow()) + " " + reservation.getStartTime());
		long compareDateEnd = DB.getTimestamp(Tools.formatDate(Tools.getNow()) + " " + reservation.getFinishTime());

		List<ReservationObjectTimesBean> reservationTimes = ReservationObjectTimesDB.getInstance().getByReservationObjectId(reservation.getReservationObjectId());

		if(reservationTimes.size()>0)
		{
			Calendar calendar = Calendar.getInstance();
	      calendar.setTime(reservation.getDateFrom());
	      int day = calendar.get(Calendar.DAY_OF_WEEK)-1;
	      if(day==0)
	      	day=7;
	      boolean emptyDay = true;

			for(ReservationObjectTimesBean rotb : reservationTimes)
			{
				if(day==rotb.getDen())
				{
					long resTimeFrom = DB.getTimestamp(Tools.formatDate(Tools.getNow()) + " " + rotb.getCasOd());
					long resTimeTo = DB.getTimestamp(Tools.formatDate(Tools.getNow()) + " " + rotb.getCasDo());

					if (compareDateStart < resTimeFrom || compareDateStart > resTimeTo || compareDateEnd < resTimeFrom || compareDateEnd > resTimeTo)
					{
						getRequest().setAttribute(
									"errorText",
									prop.getText("components.reservation.reservation_manager.addReservation.addOutsideOfTime",
												rotb.getCasOd(), rotb.getCasDo()));
						return (new ForwardResolution("/components/maybeError.jsp"));
					}
					emptyDay=false;
				}
			}
			if(emptyDay)
			{
				getRequest().setAttribute(
							"errorText",
							prop.getText("components.reservation.reservation_manager.addReservation.emptyDay"));
				return (new ForwardResolution("/components/maybeError.jsp"));
			}
		}
		else
		{
			long resTimeFrom = DB.getTimestamp(Tools.formatDate(Tools.getNow()) + " " + rob.getReservationTimeFrom());
			long resTimeTo = DB.getTimestamp(Tools.formatDate(Tools.getNow()) + " " + rob.getReservationTimeTo());

			if (compareDateStart < resTimeFrom || compareDateStart > resTimeTo || compareDateEnd < resTimeFrom
						|| compareDateEnd > resTimeTo)
			{
				getRequest().setAttribute(
							"errorText",
							prop.getText("components.reservation.reservation_manager.addReservation.addOutsideOfTime",
										rob.getReservationTimeFrom(), rob.getReservationTimeTo()));
				return (new ForwardResolution("/components/maybeError.jsp"));
			}
		}

		String lng = PageLng.getUserLng(getRequest());

		//20897 - pridana editacia konkretnej rezervacie
		if (reservation.getReservationId()>0 && isAdminLogged()==false)
			return new ForwardResolution(RESOLUTION_NOT_LOGGED);

		int approvalStatusChange = getReservationStatusChange(reservation);
		int saveOK = ReservationManager.addReservation(reservation, Tools.getServerName(getRequest()), prop, lng);
		if (saveOK > 0)
		{
			if (saveOK == 1)
				getRequest().setAttribute("errorText", prop.getText("components.reservation.reservation_manager.addReservation.one"));
			if (saveOK == 2)
				getRequest().setAttribute("errorText", prop.getText("components.reservation.reservation_manager.addReservation.two"));
			return (new ForwardResolution("/components/maybeError.jsp"));
		}

		//poslanie emailu zakaznikovy o zmene stavu schvalenia rezervacie
		if(approvalStatusChange>0)
		{
			if(approvalStatusChange==1)
				ReservationManager.sendApprovalStatusChangedEmail(reservation, true, getRequest());
			else if(approvalStatusChange==2)
				ReservationManager.sendApprovalStatusChangedEmail(reservation, false, getRequest());
		}

		if (getRequest().getParameter("rezobjid") != null)
		{
			if (saveOK == 0)
				getRequest().setAttribute("saveOK", "0");
			return (new ForwardResolution("/components/maybeError.jsp"));
		}
		return (new ForwardResolution("/components/reloadParentClose.jsp"));
	}

	/**
	 * Zisti ci je zaciatok rezervacie
	 *
	 * @return
	 */
	private boolean isStartBeforeEnd()
	{
		boolean ret = false;
		String dateFrom = "";
		String dateTo = "";
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		dateFrom = formatter.format(reservation.getDateFrom());
		dateTo = formatter.format(reservation.getDateTo());
		dateFrom = dateFrom + " " + reservation.getStartTime();
		dateTo = dateTo + " " + reservation.getFinishTime();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		try
		{
			Date from = sdf.parse(dateFrom);
			Date to = sdf.parse(dateTo);
			ret = from.compareTo(to) >= 0 ? false : true;
		}
		catch (ParseException e)
		{
			Logger.debug(getClass(), "Failed to parse reservation time! " + e.getMessage());
		}
		return ret;
	}

	/**
	 * Editovanie existujuceho rezervacneho objektu
	 *
	 * @return
	 */
	public Resolution editReservationObject()
	{
		if (isAdminLogged() == false)
			return new ForwardResolution(RESOLUTION_NOT_LOGGED);
		return (new ForwardResolution("/components/reservation/admin_addObject.jsp?isnew=true"));
	}

	/**
	 * Ulozenie rezervacneho abjektu do databazy
	 *
	 * @return
	 */
	public Resolution bSaveReservationObject()
	{
		if (isAdminLogged() == false)
			return new ForwardResolution(RESOLUTION_NOT_LOGGED);
		Prop prop = Prop.getInstance(getRequest());
		String pattern = "^[\\d]{1,2}:[\\d]{1,2}$";
		if (Tools.isNotEmpty(reservationObject.getReservationTimeFrom())
					&& !reservationObject.getReservationTimeFrom().matches(pattern))
		{
			getRequest().setAttribute("errorText", prop.getText("components.reservation.admin_addObject.cas_od_zly_format"));
			return (new ForwardResolution("/components/maybeError.jsp"));
		}
		if (Tools.isNotEmpty(reservationObject.getReservationTimeTo())
					&& !reservationObject.getReservationTimeTo().matches(pattern))
		{
			getRequest().setAttribute("errorText", prop.getText("components.reservation.admin_addObject.cas_do_zly_format"));
			return (new ForwardResolution("/components/maybeError.jsp"));
		}
		int saveOK = ReservationManager.addEditReservationObject(reservationObject, reservationObjectId);
		if (saveOK > 0)
		{
			if (saveOK == 1)
				getRequest().setAttribute("errorText",
							prop.getText("components.reservation.reservation_manager.addReservationObject.one"));
			if (saveOK == 2)
				getRequest().setAttribute("errorText",
							prop.getText("components.reservation.reservation_manager.addReservationObject.two"));
			if (saveOK == 3)
				getRequest().setAttribute("errorText",
							prop.getText("components.reservation.reservation_manager.addReservationObject.three"));
			if (saveOK == 4)
				getRequest().setAttribute("errorText",
							prop.getText("components.reservation.reservation_manager.addReservationObject.four"));
			return (new ForwardResolution("/components/maybeError.jsp"));
		}

		//rzapach: rozsah casov pre jednotlive zvolene dni
		if(chooseDays)
		{
			for(int i=0; i<7; i++)
			{
				if(days[i])
				{
					if(Tools.isEmpty(casyOd[i]) || !casyOd[i].matches(pattern))
					{
						getRequest().setAttribute("errorText", prop.getText("components.reservation.admin_addObject.cas_od_zly_format"));
						return (new ForwardResolution("/components/maybeError.jsp"));
					}
					if(Tools.isEmpty(casyDo[i]) || !casyOd[i].matches(pattern))
					{
						getRequest().setAttribute("errorText", prop.getText("components.reservation.admin_addObject.cas_do_zly_format"));
						return (new ForwardResolution("/components/maybeError.jsp"));
					}
					ReservationObjectTimesBean den = new ReservationObjectTimesBean();
					for(ReservationObjectTimesBean rotb : times)
					{
						if(rotb.getDen()==i+1)
							den = rotb;
					}
					den.setDen(i+1);
					den.setCasOd(casyOd[i]);
					den.setCasDo(casyDo[i]);
					if(reservationObjectId>0)
						den.setObjectId(reservationObjectId);
					else
						den.setObjectId(ReservationManager.getReservationObjectId(reservationObject));
					den.setDomainId(CloudToolsForCore.getDomainId());
					den.save();
				}
				else
				{
					for(ReservationObjectTimesBean rotb : times)
					{
						if(rotb.getDen()==i+1)
							rotb.delete();
					}
				}
			}
		}
		else
		{
			for(ReservationObjectTimesBean rotb : times)
			{
					rotb.delete();
			}
		}

		return (new ForwardResolution("/components/reloadParentClose.jsp"));
	}

	/**
	 * Vymazanie rezervacie z databazy
	 *
	 * @return
	 */
	public Resolution bDeleteReservation()
	{
		if (isAnyUserLogged() == false)
			return new ForwardResolution(RESOLUTION_NOT_LOGGED);
		Identity currUser = getCurrentUser();
		int delOK = ReservationManager.deleteReservation(reservation.getReservationId(), (reservationObject == null)
					? ""
					: reservationObject.getPasswd(), currUser != null ? getCurrentUser().getEmail() : "");
		Prop prop = Prop.getInstance(getRequest());
		if (delOK > 0)
		{
			if (delOK == 1)
				getRequest().setAttribute("errorText",
							prop.getText("components.reservation.reservation_manager.deleteReservation.one"));
			if (delOK == 2)
				getRequest().setAttribute("errorText",
							prop.getText("components.reservation.reservation_manager.deleteReservation.two"));
			if (delOK == 3)
				getRequest().setAttribute(
							"errorText",
							prop.getText("components.reservation.reservation_manager.deleteReservation.three", String
										.valueOf(ReservationManager.getReservationObject(
													ReservationManager.getReservationObjectId(reservation.getReservationId()))
													.getCancelTimeBefor())));
			return (new ForwardResolution("/components/maybeError.jsp"));
		}
		else
		{
			getRequest().setAttribute("delText", prop.getText("components.reservation.reservation_manager.deleteReservation.zero"));
		}
		if (reservationObject != null && Tools.isNotEmpty(reservationObject.getPasswd()))
		{
			return (new ForwardResolution("/components/reloadParentClose.jsp"));
		}
		return (new ForwardResolution("/components/maybeError.jsp"));
	}

	/**
	 * Vymazanie rezervacneho objektu z databazy
	 *
	 * @return
	 */
	public Resolution bDeleteReservationObject()
	{
		if (isAdminLogged() == false)
			return new ForwardResolution(RESOLUTION_NOT_LOGGED);
		int delOK = ReservationManager.deleteReservationObject(reservationObject.getReservationObjectId());
		deleteObjectPrices(reservationObject.getReservationObjectId());
		deleteObjectTimes(reservationObject.getReservationObjectId());
		Prop prop = Prop.getInstance(getRequest());
		if (delOK > 0)
		{
			if (delOK == 1)
				getRequest().setAttribute("errorText",
							prop.getText("components.reservation.reservation_manager.deleteReservationObject.one"));
			if (delOK == 2)
				getRequest().setAttribute("errorText",
							prop.getText("components.reservation.reservation_manager.deleteReservationObject.two"));
			return (new ForwardResolution("/components/maybeError.jsp"));
		}
		return (new ForwardResolution("/components/reloadParentClose.jsp"));
	}

	/**
	 * Vyhladavanie v zozname rezervacii
	 *
	 * @return
	 */
	public Resolution bFilter()
	{
		return (new ForwardResolution("/components/maybeError.jsp"));
	}

	/**
	 * Akceptacia rezervacie odkazom z mejlu
	 *
	 * @return
	 */
	public Resolution accept()
	{
		boolean acceptOK = ReservationManager.acceptReservation(reservation.getReservationId(), reservation.getHashValue());
		//posleme mail o schvaleni zakaznikovy
		if(acceptOK)
			ReservationManager.sendApprovalStatusChangedEmail(reservation, true, getRequest());
		Prop prop = Prop.getInstance(getRequest());
		getRequest().setAttribute("acceptText",
					prop.getText("components.reservation.reservation_manager.acceptReservation." + acceptOK));
		return (new ForwardResolution("/components/reservation/reservation_list.jsp"));
	}

	private void deleteObjectPrices(int id)
	{
		try
		{
			List<ReservationObjectPriceBean> pricesList = ReservationObjectPriceDB.getInstance().getByReservationObjectId(id);
			for(ReservationObjectPriceBean ropb : pricesList)
			{
				ropb.delete();
			}
		}
		catch(Exception e)
		{}
	}

	private void deleteObjectTimes(int id)
	{
		try
		{
			List<ReservationObjectTimesBean> timesList = ReservationObjectTimesDB.getInstance().getByReservationObjectId(id);
			for(ReservationObjectTimesBean ropb : timesList)
			{
				ropb.delete();
			}
		}
		catch(Exception e)
		{}
	}

	/**
	 * porovna staru rezervaciu ulozenu v databaze s novou aktualnou rezervaciou a vrati cislo reprezentujuce zmenu schvalenia rezervacie
	 * 0 - ziadna zmena, alebo niektora rezervacia je null
	 * 1 - stara bola neschvalena, nova je schvalena
	 * 2 - stara bola schvalena, nova je neschvalena
	 * @param reservation
	 * @return
	 */
	private int getReservationStatusChange(ReservationBean reservation)
	{
		int statusChange = 0;

		if(reservation!=null)
		{
			ReservationBean oldReservation = ReservationManager.getReservationById(reservation.getReservationId());
			if(oldReservation!=null)
			{
				if(!oldReservation.isAccepted() && reservation.isAccepted())
					statusChange = 1;
				else if(oldReservation.isAccepted() && !reservation.isAccepted())
					statusChange = 2;
			}
		}

		return statusChange;
	}

	public ReservationObjectBean getReservationObject()
	{
		return reservationObject;
	}

	public void setReservationObject(ReservationObjectBean reservationObject)
	{
		this.reservationObject = reservationObject;
	}

	public boolean isChooseDays()
	{
		return chooseDays;
	}

	public void setChooseDays(boolean chooseDays)
	{
		this.chooseDays = chooseDays;
	}

	public boolean[] getDays()
	{
		return days;
	}

	public void setDays(boolean[] days)
	{
		this.days = days;
	}

	public String[] getCasyOd()
	{
		return casyOd;
	}

	public void setCasyOd(String[] casyOd)
	{
		this.casyOd = casyOd;
	}

	public String[] getCasyDo()
	{
		return casyDo;
	}

	public void setCasyDo(String[] casyDo)
	{
		this.casyDo = casyDo;
	}
}
