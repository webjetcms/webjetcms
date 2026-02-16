package sk.iway.iwcm.system;

import java.sql.Timestamp;
import java.util.List;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.SimpleQuery;

/**
 * ConfPreparedPublisher.java
 *
 * Prechadza tabulku _conf_prepared_ a pozera ktore zaznamy maju date_prepared
 * mensi ako aktualny datum a tie skopiruje do tabulky _conf_
 *
 * @Title webjet7
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2014
 * @author $Author: jeeff mkolejak $
 * @version $Revision: 1.3 $
 * @created Date: 12.12.2014 10:40:47
 * @modified $Date: 2004/08/16 06:26:11 $
 */
public class ConfPreparedPublisher
{
	public static void main(String[] args)
	{
		sk.iway.iwcm.Logger.println(ConfPreparedPublisher.class, "START");
		try
		{
			long now = Tools.getNow();
			@SuppressWarnings("unchecked")
			List<String> rowNames =  new SimpleQuery().forList("SELECT name FROM "+ConfDB.CONF_PREPARED_TABLE_NAME+" WHERE date_prepared IS NOT NULL AND date_published IS NULL AND date_prepared < ?",
						new Timestamp(now));
			if (rowNames.size() == 0)
			{
				sk.iway.iwcm.Logger.println(ConfPreparedPublisher.class, "rowNames.size() == 0");
				return;
			}
			for (String name : rowNames)
			{
				sk.iway.iwcm.Logger.println(ConfPreparedPublisher.class, "name = " + name);
				String value = new SimpleQuery().forString("SELECT value FROM "+ConfDB.CONF_PREPARED_TABLE_NAME+" WHERE name = ? AND date_prepared IS NOT NULL AND date_published IS NULL AND date_prepared < ? ORDER BY date_prepared DESC, id DESC", name, new Timestamp(now));
				sk.iway.iwcm.Logger.println(ConfPreparedPublisher.class, "value = " + value);
				ConfDB.setName(name, value);
				ConfDB.deleteNamePrepared(name, now);
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
	}
}
