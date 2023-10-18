package sk.iway.iwcm.components.reservation;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import sk.iway.iwcm.database.ActiveRecord;

/**
 *  ReservationObjectPriceBean.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2015
 *@author       $Author: jeeff rzapach $
 *@version      $Revision: 1.3 $
 *@created      Date: 11.12.2015 14:51:55
 *@modified     $Date: 2004/08/16 06:26:11 $
 */

@Entity
@Table(name="reservation_object_price")
public class ReservationObjectPriceBean extends ActiveRecord implements Serializable
{
	private static final long serialVersionUID = -1L;
	
	@Id
	@GeneratedValue(generator="WJGen_reservation_object_price")
	@TableGenerator(name="WJGen_reservation_object_price",pkColumnValue="reservation_object_price")	
	@Column(name="object_price_id")
	private int objectPriceId;
	@Column(name="object_id")
	private int objectId;
	@Column(name="datum_od")
	@Temporal(TemporalType.DATE)
	private Date datumOd;
	@Column(name="datum_do")
	@Temporal(TemporalType.DATE)
	private Date datumDo;
	@Column(name="cena")
	private BigDecimal cena;
	@Column(name="domain_id")
	private int domainId;
	
	@Override
	public int getId()
	{
		return getObjectPriceId();
	}
	@Override
	public void setId(int id)
	{
		setObjectPriceId(id);
	}
	public int getObjectPriceId()
	{
		return objectPriceId;
	}
	public void setObjectPriceId(int objectPriceId)
	{
		this.objectPriceId = objectPriceId;
	}
	public int getObjectId()
	{
		return objectId;
	}
	public void setObjectId(int objectId)
	{
		this.objectId = objectId;
	}
	public Date getDatumOd()
	{
		return datumOd;
	}
	public void setDatumOd(Date datumOd)
	{
		this.datumOd = datumOd;
	}
	public Date getDatumDo()
	{
		return datumDo;
	}
	public void setDatumDo(Date datumDo)
	{
		this.datumDo = datumDo;
	}
	public BigDecimal getCena()
	{
		return cena;
	}
	public void setCena(BigDecimal cena)
	{
		this.cena = cena;
	}
	public int getDomainId()
	{
		return domainId;
	}
	public void setDomainId(int domainId)
	{
		this.domainId = domainId;
	}
	
}
