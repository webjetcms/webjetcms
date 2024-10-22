package sk.iway.iwcm.system.dbpool;

import java.sql.SQLException;

import javax.sql.DataSource;

/**
 *  ConfigurableDataSource.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 12.7.2010 15:03:53
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public interface ConfigurableDataSource extends DataSource
{
	public void destroy() throws SQLException;
	public String getDriverClass();
	public String getUser();

	public void setDriverClass(String driverClass);

	public void setInitialPoolSize(int initialPoolSize);

	public void setJdbcUrl(String jdbcUrl);

	public void setMaxStatements(int maxStatements);

	public void setPassword(String password);

	public void setPreferredTestQuery(String preferredTestQuery);

	public void setUser(String user);

	public int getNumActive();

	public int getNumIdle();

}
