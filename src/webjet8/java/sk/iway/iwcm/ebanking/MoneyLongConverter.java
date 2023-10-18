package sk.iway.iwcm.ebanking;

import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.converters.Converter;
import org.eclipse.persistence.sessions.Session;

/**
 *  MoneyLongConverter.java
 *  
 *  JPA converter for long/int to {@link Money} conversion.
 *  Note that this class is meant to work for tables containing
 *  no currency column, and implicitly assumes EUR currency.
 *  
 *  If your table oughts to work with multiple currencies, use
 *  get / set methods instead
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 4.11.2010 14:51:41
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class MoneyLongConverter implements Converter
{
	private static final long serialVersionUID = -6883909823262721416L;

	@Override
	public Object convertDataValueToObjectValue(Object databaseObject, Session session)
	{
		if(databaseObject == null)
			return Money.NOTHING;
		if(databaseObject instanceof Number){
			Number amount = (Number)databaseObject;
			return Money.fromEuroCents(amount.longValue());
		}
		if (databaseObject instanceof String){
			return stringToMoney((String)databaseObject);
		}
		
		return Money.NOTHING;
	}

	private Money stringToMoney(String databaseObject)
	{
		try{
			long howMuch = Long.parseLong(databaseObject);
			return Money.fromEuroCents(howMuch);
		}catch (Exception e) {
			return Money.NOTHING;
		}
	}

	@Override
	public Object convertObjectValueToDataValue(Object object, Session session)
	{
		if (object == null)
			return null;
		return ((Money)object).getAmount();
	}

	@Override
	public void initialize(DatabaseMapping arg0, Session session){}

	@Override
	public boolean isMutable()
	{
		return false;
	}
}
