package sk.iway.iwcm.system;

import java.io.Serializable;
import java.util.Date;

/**
 * Predpripravene konfiguracne premenne, ktore sa stanu aktivnymi, ked nastane
 * datum datePrepared Kazdu minutu bude bezat cron a pozerat ci je nejaka
 * hodnota nastavena v tejto beane a skopiruje ju do ConfDetails( tabulka
 * _config_ )
 * 
 * Tabulka _conf_prepared_
 * 
 * ConfPreparedDetailsBean.java
 *
 * @Title webjet7
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2010
 * @author $Author: mkolejak $
 * @version $Revision: 1.3 $
 * @created Date: 10.12.2014 08:59:15
 * @modified $Date: 2004/08/16 06:26:11 $
 */
public class ConfPreparedDetails implements Serializable
{
	private static final long serialVersionUID = -1L;
	String name;
	String value;
	Date dateChanged;
	Date datePrepared;

	public ConfPreparedDetails()
	{
	}

	public ConfPreparedDetails(String name, String value, Date datePrepared)
	{
		this.name = name;
		this.value = value;
		this.datePrepared = datePrepared;
	}

	public ConfPreparedDetails(String name, String value, Date dateChanged, Date datePrepared)
	{
		this.name = name;
		this.value = value;
		this.dateChanged = dateChanged;
		this.datePrepared = datePrepared;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}

	public Date getDateChanged()
	{
		return dateChanged;
	}

	public void setDateChanged(Date dateChanged)
	{
		this.dateChanged = dateChanged;
	}

	public Date getDatePrepared()
	{
		return datePrepared;
	}

	public void setDatePrepared(Date datePrepared)
	{
		this.datePrepared = datePrepared;
	}
}