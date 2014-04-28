package infradev.lumis.lumisportalinstaller.database;

import infradev.lumis.lumisportalinstaller.common.AbstractInstaller;
import infradev.lumis.lumisportalinstaller.common.PathEnum;
import infradev.lumis.lumisportalinstaller.common.inject.annotation.Logged;
import infradev.lumis.lumisportalinstaller.common.tools.Log;
import infradev.lumis.lumisportalinstaller.common.tools.ProgressMeter;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.common.io.PatternFilenameFilter;

/**
 * Abstract class to databases.
 * 
 * @author Alexandre Ribeiro de Souza
 */
public abstract class AbstractDatabase extends AbstractInstaller {

	public static boolean driverRegistered = false;

	protected String driverPattern = null;
	protected String commentPattern = null;
	protected String classname = null;

	public String getClassname() {
		return classname;
	}

	@Override
	protected void extract() throws IOException {
	}

	@Override
	protected void configure() throws IOException {
	}

	/**
	 * Configure lumishibernate.cfg.xml
	 * 
	 * @param file
	 *            lumishibernate.cfg.xml file.
	 * @return List of lines of processed lumishibernate.cfg.xml file.
	 * @throws IOException
	 *             Throws IOException.
	 */
	public List<String> configureLumishibernateCfgXml(File file) throws IOException {
		List<String> lines = Files.readLines(file, Charsets.UTF_8);

		boolean hasDuplicatedSqlServerConfiguration = hasDuplicatedSqlServerConfiguration(lines);
		boolean endCommendLineFound = false;
		int startCommentLine = 0;

		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).contains(commentPattern)) {
				startCommentLine = i;
			}
		}

		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);

			if (hasDuplicatedSqlServerConfiguration && line.contains("<!-- MS SQL Server")) {
				lines.set(i + 1, "\t\t<!--");
				lines.set(i + 7, "\t\t-->");
				hasDuplicatedSqlServerConfiguration = false;
			}

			if (i == startCommentLine + 1) {
				lines.set(i, "\t\t<!-- -->");
			}
			if (i == startCommentLine + 4) {
				lines.set(i, String.format("\t\t<property name=\"connection.username\">%s</property>", databaseUser));
			}
			if (i == startCommentLine + 5) {
				lines.set(i, String.format("\t\t<property name=\"connection.password\">%s</property>", databasePassword));
			}
			if (i == startCommentLine + 6) {
				lines.set(i, String.format("\t\t<property name=\"connection.url\">%s</property>", databaseUrl.replace("&", "&amp;")));
			}
			if (i > startCommentLine && (!endCommendLineFound & line.contains("-->"))) {
				lines.set(i, "");
				endCommendLineFound = true;
			}
		}

		return lines;
	}

	/**
	 * Execute statements on a file.
	 * 
	 * @param file
	 *            File with the statements.
	 * @return count of statements executed.
	 * @throws IOException
	 *             Throws IOException.
	 */
	@Logged
	public int executeScript(File file) throws IOException {
		List<String> lines = convertSql(file);
		int statementCount = executeScript(lines);

		return statementCount;
	}

	/**
	 * Execute statements on a list.
	 * 
	 * @param lines
	 *            List with the statements.
	 * @return count of statements executed.
	 * @throws IOException
	 *             Throws IOException.
	 */
	public int executeScript(List<String> lines) throws IOException {
		Connection connection = null;
		Statement statement = null;

		int result = 0;

		try {
			connection = createConnection();
			statement = connection.createStatement();

			ProgressMeter progressMeter = injector.getInstance(ProgressMeter.class);
			for (int i = 0; i < lines.size(); i++) {
				result++;
				String line = lines.get(i);
				executeSql(line, statement);
				String msg = String.format("  |- %d of %d statements executed", i + 1, lines.size());
				progressMeter.tick(msg);
			}
		} catch (ClassNotFoundException | IllegalAccessException | InstantiationException | MalformedURLException | SQLException e) {
			Log.error("(Line " + result + ") " + e.getLocalizedMessage());
		} finally {
			closeConnection(connection, statement);
		}

		return result;
	}

	/**
	 * Create a SQL Connection.
	 * 
	 * @return Created connection.
	 * @throws MalformedURLException
	 *             Throws MalformedURLException.
	 * @throws InstantiationException
	 *             Throws InstantiationException.
	 * @throws IllegalAccessException
	 *             Throws IllegalAccessException.
	 * @throws ClassNotFoundException
	 *             Throws ClassNotFoundException.
	 * @throws SQLException
	 *             Throws SQLException.
	 */
	public Connection createConnection() throws MalformedURLException, InstantiationException, IllegalAccessException, ClassNotFoundException,
			SQLException {
		if (!driverRegistered) {
			File jdbcDriverFile = null;
			for (File lib : PathEnum.LUMISPORTAL_DIR_CONTEXTLIB.getFile().listFiles(new PatternFilenameFilter(driverPattern + ".*"))) {
				jdbcDriverFile = lib;
			}

			if (null == jdbcDriverFile) {
				return null;
			}

			URL driverUrl = new URL(String.format("jar:%s!/", jdbcDriverFile.toURI()));
			URLClassLoader urlClassLoader = new URLClassLoader(new URL[] { driverUrl });
			Driver driver = (Driver) Class.forName(classname, true, urlClassLoader).newInstance();
			DriverManager.registerDriver(new DriverShim(driver));
		}

		Connection connection = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword);

		return connection;
	}

	/**
	 * Close a SQL Connection.
	 * 
	 * @param connection
	 *            Connection to be closed.
	 * @param statement
	 *            Statement to be closed.
	 */
	public void closeConnection(Connection connection, Statement statement) {
		try {
			if (connection != null) {
				connection.close();
			}
			if (statement != null) {
				statement.close();
			}
		} catch (SQLException e) {
			Log.error(e.getLocalizedMessage());
		}
	}

	/**
	 * Execute a string on a statement.
	 * 
	 * @param query
	 *            string containing a query to be executed.
	 * @param statement
	 *            statement where the query will be executed.
	 * @return true if the first result is a ResultSet object; false if it is an update count or there are no results.
	 * @throws SQLException
	 *             Throws SQLException.
	 */
	public boolean executeSql(String query, Statement statement) throws SQLException {
		if ("ORACLE".equalsIgnoreCase(databaseType) && query.substring(query.length() - 1).equals(";")) {
			query = query.substring(0, query.length() - 1);
		}
		boolean result = statement.execute(query);

		return result;
	}

	private Boolean hasDuplicatedSqlServerConfiguration(List<String> lines) {
		int sqlServers = 0;

		for (String line : lines) {
			if (line.contains("<!-- MS SQL Server")) {
				sqlServers++;
			}
		}

		return sqlServers > 1;
	}

	private List<String> convertSql(File file) throws IOException {
		List<String> lines = Files.readLines(file, Charsets.UTF_8);

		List<String> result = Lists.newArrayList();
		StringBuilder query = new StringBuilder();

		for (String line : lines) {
			line = line.trim();
			boolean match = false;
			if (!line.isEmpty() && !(line.startsWith("PRINT") || line.startsWith("--") || line.startsWith("#"))) {
				if (line.startsWith("GO")) {
					if (!query.toString().trim().isEmpty()) {
						match = true;
					}
				} else {
					query.append(line.replace("\t", "")).append(" ");
					if (query.toString().trim().endsWith(";")) {
						match = true;
					}
				}
			}
			if (match) {
				result.add(query.toString().trim());
				query = new StringBuilder();
			}
		}

		return result;
	}

}
