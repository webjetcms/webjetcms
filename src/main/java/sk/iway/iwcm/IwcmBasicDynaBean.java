package sk.iway.iwcm;

import java.math.BigDecimal;

import org.apache.commons.beanutils.BasicDynaBean;
import org.apache.commons.beanutils.DynaClass;
import org.apache.commons.beanutils.DynaProperty;

/**
 * IwcmBasicDynaBean.java
 *
 * @Title webjet4
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2008
 * @author $Author: jeeff $
 * @version $Revision: 1.3 $
 * @created Date: 27.5.2008 10:28:55
 * @modified $Date: 2010/01/20 11:13:38 $
 */
public class IwcmBasicDynaBean extends BasicDynaBean
{
	private static final long serialVersionUID = 7437506094160412002L;

	public IwcmBasicDynaBean(DynaClass class_)
	{
		super(class_);
	}

	@Override
	public void set(String name, Object value)
	{
		DynaProperty descriptor = getDynaProperty(name);
		if (value == null)
		{
			if (descriptor.getType().isPrimitive())
			{
				throw new NullPointerException("Primitive value for '" + name + "'");
			}
		}
		if (value instanceof oracle.sql.TIMESTAMP)
		{
			try
			{

			values.put(name,((oracle.sql.TIMESTAMP)value).timestampValue());
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}
		else
		{
			if (value instanceof java.math.BigDecimal)
			{
				values.put(name,((BigDecimal)value).longValue());
			}
			else
			{
				values.put(name, value);
			}
		}

	}
}
