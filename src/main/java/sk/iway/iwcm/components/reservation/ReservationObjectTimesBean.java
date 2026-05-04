package sk.iway.iwcm.components.reservation;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;

import sk.iway.iwcm.DB;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.ActiveRecord;

/**
 *  ReservationObjectTimesBean.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2016
 *@author       $Author: jeeff rzapach $
 *@version      $Revision: 1.3 $
 *@created      Date: 20.1.2016 15:23:42
 *@modified     $Date: 2004/08/16 06:26:11 $
 */

@Entity
@Table(name="reservation_object_times")
public class ReservationObjectTimesBean extends ActiveRecord implements Serializable
{
	private static final long serialVersionUID = -1L;

	@Id
	@GeneratedValue(generator="WJGen_reservation_object_times")
	@TableGenerator(name="WJGen_reservation_object_times",pkColumnValue="reservation_object_times")
	@Column(name="object_time_id")
	private int objectTimeId;
	@Column(name="object_id")
	private int objectId;
	@Column(name="cas_od")
	private Date timeFrom;
	@Column(name="cas_do")
	private Date timeTo;
	@Column(name="den")
	private int den;
	@Column(name="domain_id")
	private int domainId;

	public int getObjectTimeId()
	{
		return objectTimeId;
	}
	public void setObjectTimeId(int objectTimeId)
	{
		this.objectTimeId = objectTimeId;
	}
	public int getObjectId()
	{
		return objectId;
	}
	public void setObjectId(int objectId)
	{
		this.objectId = objectId;
	}
	public int getDen()
	{
		return den;
	}
	public void setDen(int den)
	{
		this.den = den;
	}
	public int getDomainId()
	{
		return domainId;
	}
	public void setDomainId(int domainId)
	{
		this.domainId = domainId;
	}

	@Override
	public int getId()
	{
		return getObjectTimeId();
	}
	@Override
	public void setId(int id)
	{
		setObjectTimeId(id);
	}
	public String getCasOd()
	{
		return Tools.formatTime(timeFrom);
	}
	public void setCasOd(String casOd)
	{
		timeFrom = new Date(DB.getTimestamp("01.01.2000", casOd));
	}
	public String getCasDo()
	{
		return Tools.formatTime(timeTo);
	}
	public void setCasDo(String casDo)
	{
		timeTo = new Date(DB.getTimestamp("01.01.2000", casDo));
	}

}
