package sk.iway.iwcm.components.reservation;

import java.util.Date;

/**
 * ReservationBean.java - zaznam z tabulky reservation pre uchovanie rezervacie
 *	@Title        webjet4
 *	@Company      Interway s.r.o. (www.interway.sk)
 *	@Copyright    Interway s.r.o. (c) 2001-2008
 *	@author       $Author: kmarton $
 *	@version      $Revision: 1.4 $
 *	@created      Date: 20.12.2008 14:55:51
 *	@modified     $Date: 2009/04/06 12:54:09 $
 */

public class ReservationBean 
{
	private int reservationId;
	private int reservationObjectId;
	private Date dateFrom = null;
	private String startTime;
	private Date dateTo = null;
	private String finishTime;
	private String name;
	private String surname;
	private String email;
	private String purpose;
	
	private boolean accepted;
	private String hashValue;
	
	private String phoneNumber;
	
	public String getStartTime()
	{
		return startTime;
	}
	public void setStartTime(String startTime)
	{
		this.startTime = startTime;
	}
	public String getFinishTime()
	{
		return finishTime;
	}
	public void setFinishTime(String finishTime)
	{
		this.finishTime = finishTime;
	}
	
	
	public String getHashValue() {
		return hashValue;
	}
	public void setHashValue(String hashValue) {
		this.hashValue = hashValue;
	}
	
	public boolean isAccepted()
	{
		return accepted;
	}
	public void setAccepted(boolean accepted)
	{
		this.accepted = accepted;
	}
	public String getReservationObjectName() 
	{
		return (ReservationManager.getReservationObjectName(reservationObjectId));
	}
	
	public int getReservationId() {
		return reservationId;
	}
	public void setReservationId(int reservationId) {
		this.reservationId = reservationId;
	}
	public int getReservationObjectId() {
		return reservationObjectId;
	}
	public void setReservationObjectId(int reservationObjectId) {
		this.reservationObjectId = reservationObjectId;
	}
	
	
	public Date getDateFrom()
	{
		return dateFrom == null ? null : (Date) dateFrom.clone();
	}
	public void setDateFrom(Date dateFrom)
	{
		this.dateFrom = dateFrom == null ? null : (Date) dateFrom.clone();
	}
	public Date getDateTo()
	{
		return dateTo == null ? null : (Date) dateTo.clone();
	}
	public void setDateTo(Date dateTo)
	{
		this.dateTo = dateTo == null ? null : (Date) dateTo.clone();
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getSurname() {
		return surname;
	}
	public void setSurname(String surname) {
		this.surname = surname;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getPurpose() {
		return purpose;
	}
	public void setPurpose(String purpose) {
		this.purpose = purpose;
	}
	public String getPhoneNumber() {
		return phoneNumber;
	}
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
}
