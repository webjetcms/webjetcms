package org.apache.commons.dbcp;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import oracle.ucp.UniversalConnectionPoolException;
import oracle.ucp.admin.UniversalConnectionPoolManager;
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl;
import oracle.ucp.jdbc.PoolDataSource;

public class WebJetUcpDataSource implements ConfigurableDataSource {
    PoolDataSource originalSource;
    String poolName = "";

    public WebJetUcpDataSource(PoolDataSource source, String name) throws SQLException {
        originalSource = source;

        // default connection factory
        originalSource.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");

        poolName = name;
        originalSource.setConnectionPoolName(poolName);
    }

    @Override
    public void destroy() throws SQLException {
        try {
            UniversalConnectionPoolManager mgr = UniversalConnectionPoolManagerImpl.getUniversalConnectionPoolManager();
            mgr.destroyConnectionPool(poolName);
        } catch(UniversalConnectionPoolException e) {
            sk.iway.iwcm.Logger.error(e);

            throw new SQLException(e.getMessage());
        }
    }

    public void setConnectionProperty(String key, String value) {
        try {
            originalSource.setConnectionProperty(key, value);
        } catch (SQLException e) {
            sk.iway.iwcm.Logger.error(e);
        }
    }

    public String getConnectionProperty(String key) {
        return originalSource.getConnectionProperty(key);
    }

    @Override
    public String getDriverClass() {
        return originalSource.getConnectionFactoryClassName();
    }

    @Override
    public String getUser() {
        return originalSource.getUser();
    }

    @Override
    public void setDriverClass(String driverClass) {
        try {
            originalSource.setConnectionFactoryClassName(driverClass);
        } catch (SQLException e) {
            sk.iway.iwcm.Logger.error(e);
        }
    }

    @Override
    public void setInitialPoolSize(int initialPoolSize) {
        try {
            originalSource.setInitialPoolSize(initialPoolSize);
        } catch (SQLException e) {
            sk.iway.iwcm.Logger.error(e);
        }
    }

    public void setMinPoolSize(int minActive) {
        try {
            originalSource.setMinPoolSize(minActive);
        } catch (SQLException e) {
            sk.iway.iwcm.Logger.error(e);
        }
    }

    public void setMaxPoolSize(int maxPoolSize)
    {
        try {
            originalSource.setMaxPoolSize(maxPoolSize);
        } catch (SQLException e) {
            sk.iway.iwcm.Logger.error(e);
        }
    }

    public void setFastConnectionFailoverEnabled(boolean val) {
        try {
            originalSource.setFastConnectionFailoverEnabled(val);
        } catch (SQLException e) {
            sk.iway.iwcm.Logger.error(e);
        }
    }

    public void setONSConfiguration(String val) {
        try {
            originalSource.setONSConfiguration(val);
        } catch (SQLException e) {
            sk.iway.iwcm.Logger.error(e);
        }
    }

    public void setValidateConnectionOnBorrow(boolean val) {
        try {
            originalSource.setValidateConnectionOnBorrow(val);
        } catch(SQLException e) {
            sk.iway.iwcm.Logger.error(e);
        }
    }

    @Override
    public void setJdbcUrl(String jdbcUrl) {
        try {
            originalSource.setURL(jdbcUrl);
        } catch (SQLException e) {
            sk.iway.iwcm.Logger.error(e);
        }
    }

    @Override
    public void setMaxStatements(int maxStatements) {
        try {
            originalSource.setMaxStatements(maxStatements);
        } catch (SQLException e) {
            sk.iway.iwcm.Logger.error(e);
        }
    }

    @Override
    public void setPassword(String password) {
        try {
            originalSource.setPassword(password);
        } catch (SQLException e) {
            sk.iway.iwcm.Logger.error(e);
        }
    }

    @Override
    public void setPreferredTestQuery(String preferredTestQuery) {
        try {
            originalSource.setSQLForValidateConnection(preferredTestQuery);
        } catch (SQLException e) {
            sk.iway.iwcm.Logger.error(e);
        }
    }

    @Override
    public void setUser(String user) {
        try {
            originalSource.setUser(user);
        } catch (SQLException e) {
            sk.iway.iwcm.Logger.error(e);
        }
    }

    public PoolDataSource getOriginalSource() {
        return originalSource;
    }

    @Override
    public int getNumActive() {
        return originalSource.getStatistics().getTotalConnectionsCount() - originalSource.getStatistics().getRemainingPoolCapacityCount();
    }

    @Override
    public int getNumIdle() {
        return originalSource.getStatistics().getRemainingPoolCapacityCount();
    }

    @Override
    public void printStackTraces() throws SQLException {
        // ???
    }

    @Override
    public void printStackTraces(PrintWriter s) throws SQLException {
        // ???
    }

    @Override
    public Connection getConnection() throws SQLException {
        return originalSource.getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return originalSource.getConnection(username, password);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return originalSource.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        originalSource.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        originalSource.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return originalSource.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return originalSource.getParentLogger();
    }
}
