package infradev.lumis.lumisportalinstaller.database;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import javax.inject.Inject;

/**
 * Implements an abstraction to the real JDBC driver.
 * 
 * @author Alexandre Ribeiro de Souza
 */
public class DriverShim implements Driver {

	@Inject
	private Logger logger;

	private final Driver driver;

	@Inject
	public DriverShim(Driver driver) {
		super();
		this.driver = driver;
	}

	@Override
	public Connection connect(String url, Properties info) throws SQLException {
		return driver.connect(url, info);
	}

	@Override
	public boolean acceptsURL(String url) throws SQLException {
		return driver.acceptsURL(url);
	}

	@Override
	public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
		return driver.getPropertyInfo(url, info);
	}

	@Override
	public int getMajorVersion() {
		return driver.getMajorVersion();
	}

	@Override
	public int getMinorVersion() {
		return driver.getMinorVersion();
	}

	@Override
	public boolean jdbcCompliant() {
		return driver.jdbcCompliant();
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return logger;
	}
}
