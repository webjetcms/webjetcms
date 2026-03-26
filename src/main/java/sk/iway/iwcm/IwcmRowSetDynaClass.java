package sk.iway.iwcm;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.RowSetDynaClass;

/**
 * IwcmRowSetDynaClass.java
 *
 * @Title webjet4
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2008
 * @author $Author: thaber $
 * @version $Revision: 1.1 $
 * @created Date: 26.5.2008 16:13:16
 * @modified $Date: 2008/05/27 16:18:37 $
 */
public class IwcmRowSetDynaClass extends RowSetDynaClass
{
	private static final long serialVersionUID = 1L;

	public IwcmRowSetDynaClass(ResultSet rs) throws SQLException
	{
		super(rs, true, -1, true);
	}

	@Override
	protected void copy(ResultSet resultSet) throws SQLException
	{
		int cnt = 0;
		while (resultSet.next() && (limit < 0 || cnt++ < limit))
		{
			DynaBean bean = createDynaBean();
			for (int i = 0; i < properties.length; i++)
			{
				String name = properties[i].getName();
				//v oracle 10g jdbc driveroch je chyba, vdaka ktorej vracia oracle.sql.DATE tam kde by mal vracat oracle.sql.Timestamp

				if (resultSet.getObject(name) instanceof oracle.sql.DATE)
				{
					bean.set(name, new oracle.sql.TIMESTAMP((oracle.sql.DATE)resultSet.getObject(name)));
				}
				else if(resultSet.getObject(name) instanceof net.sourceforge.jtds.jdbc.ClobImpl)
				{
					bean.set(name, resultSet.getString(name));
				}
				else
				{
					bean.set(name, resultSet.getObject(name));
				}

			}
			rows.add(bean);
		}
	}

	@Override
	protected DynaBean createDynaBean()
	{
		return (new IwcmBasicDynaBean(this));
	}
}
