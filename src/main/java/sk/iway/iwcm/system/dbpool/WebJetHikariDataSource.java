package sk.iway.iwcm.system.dbpool;

import com.zaxxer.hikari.HikariDataSource;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * https://github.com/brettwooldridge/HikariCP
 */
public class WebJetHikariDataSource implements ConfigurableDataSource {
    HikariDataSource originalSource;

    public WebJetHikariDataSource(HikariDataSource source)
    {
        originalSource = source;
    }

    @Override
    public void destroy() throws SQLException {
        originalSource.close();
    }

    public void setConnectionProperty(String key, String value) {
        originalSource.addDataSourceProperty(key, value);
    }

    @Override
    public String getDriverClass() {
        return originalSource.getDriverClassName();
    }

    @Override
    public String getUser() {
        return originalSource.getUsername();
    }

    @Override
    public void setDriverClass(String driverClass) {
        originalSource.setDriverClassName(driverClass);
    }

    @Override
    public void setInitialPoolSize(int initialPoolSize) {
        originalSource.setMinimumIdle(initialPoolSize);
    }

    public void setMinPoolSize(int minActive) {
        originalSource.setMinimumIdle(minActive);
    }

    public void setMaxPoolSize(int maxPoolSize)
    {
        originalSource.setMaximumPoolSize(maxPoolSize);
    }

    @Override
    public void setJdbcUrl(String jdbcUrl) {
        originalSource.setJdbcUrl(jdbcUrl);
    }

    @Override
    public void setMaxStatements(int maxStatements) {
        // ???
    }

    @Override
    public void setPassword(String password) {
        originalSource.setPassword(password);
    }

    @Override
    public void setPreferredTestQuery(String preferredTestQuery) {
        originalSource.setConnectionInitSql(preferredTestQuery);
        //documentation says do not set testSql, it will use JDBC4 isValid instead
    }

    @Override
    public void setUser(String user) {
        originalSource.setUsername(user);
    }

    @Override
    public int getNumActive() {
        return originalSource.getHikariPoolMXBean().getActiveConnections();
    }

    @Override
    public int getNumIdle() {
        return originalSource.getHikariPoolMXBean().getIdleConnections();
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
    public <T> T unwrap(Class<T> iface)
    {
        try {
            return originalSource.unwrap(iface);
        } catch (SQLException e) {
            return null;
        }

    }

    @Override
    public boolean isWrapperFor(Class<?> iface)
    {
        try {
            return originalSource.isWrapperFor(iface);
        } catch (SQLException e) {
            return false;
        }
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
