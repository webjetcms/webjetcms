package org.apache.commons.dbcp;

import java.beans.PropertyVetoException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.DataSources;

import org.apache.commons.lang.NotImplementedException;

/**
 *  WebJETc3p0dataSource.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 12.7.2010 15:07:07
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class WebJETc3p0dataSource implements ConfigurableDataSource
{
	ComboPooledDataSource originalSource;

	public WebJETc3p0dataSource(ComboPooledDataSource originalSource)
	{
		this.originalSource = originalSource;
	}

	public ComboPooledDataSource getOriginalSource()
	{
		return originalSource;
	}
	@Override
	public String getUser()
	{
		return originalSource.getUser();
	}

	public void setAcquireIncrement(int acquireIncrement)
	{
		originalSource.setAcquireIncrement(acquireIncrement);
	}
	@Override
	public void setDriverClass(String driverClass)
	{
		try
		{
			originalSource.setDriverClass(driverClass);
		}
		catch (PropertyVetoException e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
	}
	@Override
	public void setInitialPoolSize(int initialPoolSize)
	{
		originalSource.setInitialPoolSize(initialPoolSize);
	}
	@Override
	public void setJdbcUrl(String jdbcUrl)
	{
		originalSource.setJdbcUrl(jdbcUrl);
	}

	public void setMaxConnectionAge(int maxConnectionAge)
	{
		originalSource.setMaxConnectionAge(maxConnectionAge);
	}

	public void setMaxIdleTime(int maxIdleTime)
	{
		originalSource.setMaxIdleTime(maxIdleTime);
	}

	public void setMaxPoolSize(int maxPoolSize)
	{
		originalSource.setMaxPoolSize(maxPoolSize);
	}
	@Override
	public void setMaxStatements(int maxStatements)
	{
		originalSource.setMaxStatements(maxStatements);
	}

	public void setMaxStatementsPerConnection(int maxStatementsPerConnection)
	{
		originalSource.setMaxStatementsPerConnection(maxStatementsPerConnection);
	}

	public void setMinPoolSize(int minPoolSize)
	{
		originalSource.setMinPoolSize(minPoolSize);
	}
	@Override
	public void setPassword(String password)
	{
		originalSource.setPassword(password);
	}
	@Override
	public void setPreferredTestQuery(String preferredTestQuery)
	{
		originalSource.setPreferredTestQuery(preferredTestQuery);
	}

	public void setProperties(Properties properties)
	{
		originalSource.setProperties(properties);
	}
	@Override
	public void setUser(String user)
	{
		originalSource.setUser(user);
	}
	@Override
	public String toString()
	{
		return originalSource.toString();
	}
	@Override
	public void destroy() throws SQLException
	{
		DataSources.destroy(originalSource);
	}
	@Override
	public String getDriverClass()
	{
		return originalSource.getDriverClass();
	}
	@Override
	public Connection getConnection() throws SQLException
	{
		return originalSource.getConnection();
	}
	@Override
	public Connection getConnection(String username, String password) throws SQLException
	{
		return originalSource.getConnection(username, password);
	}
	@Override
	public PrintWriter getLogWriter() throws SQLException
	{
		return originalSource.getLogWriter();
	}
	@Override
	public int getLoginTimeout() throws SQLException
	{
		return originalSource.getLoginTimeout();
	}
	@Override
	public void setLogWriter(PrintWriter out) throws SQLException
	{
		originalSource.setLogWriter(out);
	}
	@Override
	public void setLoginTimeout(int seconds) throws SQLException
	{
		originalSource.setLoginTimeout(seconds);
	}
	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException
	{
		return false;
	}
	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException
	{
		return null;
	}

	public void setNumHelperThreads(int i)
	{
		originalSource.setNumHelperThreads(i);
	}
	@Override
	public int getNumActive()
	{
		try
		{
			return originalSource.getNumBusyConnections();
		}
		catch (SQLException e)
		{
			sk.iway.iwcm.Logger.error(e);
			return 0;
		}
	}
	@Override
	public int getNumIdle()
	{
		try
		{
			return originalSource.getNumIdleConnections();
		}
		catch (SQLException e)
		{
			sk.iway.iwcm.Logger.error(e);
			return 0;
		}
	}
	@Override
	public void printStackTraces() throws SQLException
	{
		System.out.println(this.originalSource.sampleThreadPoolStackTraces());
	}

   // TODO nove v Jave 1.7; ked prejdeme na Javu 1.7, odkomentujte nasledujuci riadok
   // @Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException
	{
		throw new NotImplementedException();
	}

	 @Override
	 public void printStackTraces(PrintWriter s) throws SQLException
	 {
	 	 s.println(this.originalSource.sampleThreadPoolStackTraces());
	 }
}
