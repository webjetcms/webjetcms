package sk.iway.iwcm.components.reservation;

import sk.iway.iwcm.Tools;

/**
 * ReservationObjectBean.java - zaznam z tabulky reservation_object pre uchovanie rezervacneho objektu
 *	@Title        webjet4
 *	@Company      Interway s.r.o. (www.interway.sk)
 *	@Copyright    Interway s.r.o. (c) 2001-2008
 *	@author       $Author: kmarton $
 *	@version      $Revision: 1.3 $
 *	@created      Date: 20.12.2008 14:55:51
 *	@modified     $Date: 2009/04/06 12:54:09 $
 */

public class ReservationObjectBean 
{
	private int reservationObjectId;
	private String name;
	private boolean mustAccepted;
	private String mustAcceptedString;
	private String emailAccepter;
	private String passwd;
	private String passwdRepeat;
	private boolean changePass;
	
	private int maxReservations = 1;
	private int cancelTimeBefor = 0;
	private String reservationTimeFrom = "00:00";
	private String reservationTimeTo = "23:59";
	
	private double priceForDay;
	private double priceForHour;
	private String priceForDayString;
	private String priceForHourString;
	private boolean reservationForAllDay;
	private String photoLink;
	private String description;
	
	private String timeUnit;

	
	
	public boolean isChangePass()
	{
		return changePass;
	}
	public void setChangePass(boolean changePass)
	{
		this.changePass = changePass;
	}
	public String getPasswdRepeat()
	{
		return passwdRepeat;
	}
	public void setPasswdRepeat(String passwdRepeat)
	{
		this.passwdRepeat = passwdRepeat;
	}
	public String getMustAcceptedString() {
		return mustAcceptedString;
	}
	public void setMustAcceptedString(String mustAcceptedString) {
		this.mustAcceptedString = mustAcceptedString;
	}
	public boolean isMustAccepted() {
		return mustAccepted;
	}
	public void setMustAccepted(boolean mustAccepted) {
		this.mustAccepted = mustAccepted;
	}
	public String getPasswd() {
		return passwd;
	}
	public void setPasswd(String passwd) {
		this.passwd = passwd;
	}
	
	public String getEmailAccepter() {
		return emailAccepter;
	}
	public void setEmailAccepter(String emailAccepter) {
		this.emailAccepter = emailAccepter;
	}
	public int getReservationObjectId() {
		return reservationObjectId;
	}
	public void setReservationObjectId(int reservationObjectId) {
		this.reservationObjectId = reservationObjectId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (changePass ? 1231 : 1237);
		result = prime * result + ((emailAccepter == null) ? 0 : emailAccepter.hashCode());
		result = prime * result + (mustAccepted ? 1231 : 1237);
		result = prime * result + ((mustAcceptedString == null) ? 0 : mustAcceptedString.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((passwd == null) ? 0 : passwd.hashCode());
		result = prime * result + ((passwdRepeat == null) ? 0 : passwdRepeat.hashCode());
		result = prime * result + reservationObjectId;
		return result;
	}
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReservationObjectBean other = (ReservationObjectBean) obj;
		if (changePass != other.changePass)
			return false;
		if (emailAccepter == null)
		{
			if (other.emailAccepter != null)
				return false;
		}
		else if (!emailAccepter.equals(other.emailAccepter))
			return false;
		if (mustAccepted != other.mustAccepted)
			return false;
		if (mustAcceptedString == null)
		{
			if (other.mustAcceptedString != null)
				return false;
		}
		else if (!mustAcceptedString.equals(other.mustAcceptedString))
			return false;
		if (name == null)
		{
			if (other.name != null)
				return false;
		}
		else if (!name.equals(other.name))
			return false;
		if (passwd == null)
		{
			if (other.passwd != null)
				return false;
		}
		else if (!passwd.equals(other.passwd))
			return false;
		if (passwdRepeat == null)
		{
			if (other.passwdRepeat != null)
				return false;
		}
		else if (!passwdRepeat.equals(other.passwdRepeat))
			return false;
		if (reservationObjectId != other.reservationObjectId)
			return false;
		return true;
	}
	public int getMaxReservations()
	{
		return maxReservations;
	}
	public void setMaxReservations(int maxReservations)
	{
		this.maxReservations = maxReservations;
	}
	public int getCancelTimeBefor()
	{
		return cancelTimeBefor;
	}
	public void setCancelTimeBefor(int cancelTimeBefor)
	{
		this.cancelTimeBefor = cancelTimeBefor;
	}
	public String getReservationTimeFrom()
	{
		if(Tools.isEmpty(reservationTimeFrom)) reservationTimeFrom = "00:00";
		return reservationTimeFrom;
	}
	public void setReservationTimeFrom(String reservationTimeFrom)
	{
		this.reservationTimeFrom = reservationTimeFrom;
	}
	public String getReservationTimeTo()
	{
		if(Tools.isEmpty(reservationTimeTo)) reservationTimeTo = "23:59";
		return reservationTimeTo;
	}
	public void setReservationTimeTo(String reservationTimeTo)
	{
		this.reservationTimeTo = reservationTimeTo;
	}
	public double getPriceForDay()
	{
		return priceForDay;
	}
	public void setPriceForDay(double priceForDay)
	{
		this.priceForDay = priceForDay;
	}
	public double getPriceForHour()
	{
		return priceForHour;
	}
	public void setPriceForHour(double priceForHour)
	{
		this.priceForHour = priceForHour;
	}
	public String getPriceForDayString()
	{
		return this.priceForDayString;
	}
	public void setPriceForDayString(double priceForDay)
	{
		this.priceForDayString = String.format("%.2f", priceForDay);
	}
	public String getPriceForHourString()
	{
		return this.priceForHourString;
	}
	public void setPriceForHourString(double priceForHour)
	{
		this.priceForHourString = String.format("%.2f", priceForHour);
	}
	public boolean getReservationForAllDay()
	{
		return reservationForAllDay;
	}
	public void setReservationForAllDay(boolean reservationForAllDay)
	{
		this.reservationForAllDay = reservationForAllDay;
	}
	public String getPhotoLink()
	{
		return photoLink;
	}
	public void setPhotoLink(String photoLink)
	{
		this.photoLink = photoLink;
	}
	public String getDescription()
	{
		return description;
	}
	public void setDescription(String description)
	{
		this.description = description;
	}
	public String getTimeUnit()
	{
		return timeUnit;
	}
	public void setTimeUnit(String timeUnit)
	{
		this.timeUnit = timeUnit;
	}
}
