package infradev.lumis.lumisportalinstaller.lumisportal;

import infradev.lumis.lumisportalinstaller.common.AbstractInstaller;
import infradev.lumis.lumisportalinstaller.common.PathEnum;
import infradev.lumis.lumisportalinstaller.common.ResourceEnum;
import infradev.lumis.lumisportalinstaller.common.inject.annotation.Logged;
import infradev.lumis.lumisportalinstaller.common.tools.Log;
import infradev.lumis.lumisportalinstaller.database.AbstractDatabase;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.base.Charsets;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.common.io.PatternFilenameFilter;

/**
 * Abstract class to Lumis Portal.
 * 
 * @author Alexandre Ribeiro de Souza
 */
public class AbstractLumisPortal extends AbstractInstaller {

	@Inject
	@Named("${database.type:-MYSQL}")
	protected AbstractDatabase database;

	@Override
	protected void extract() throws IOException {
		unpackZipFile(lumisportalInstallFile, PathEnum.LUMISPORTAL_DIR.getFile(), extractDoc ? "" : "www/lumis/doc.*", false);
	}

	@Override
	protected void configure() throws IOException {
		configureHtdocs();
		configureWebXml();
		configureJbosswebXml();
		configureLumisportalconfigXml();
		configureSetup_Files();

		database.install();

		configureLumishibernateCfgXml();
		initializePortal();
	}

	/**
	 * Configure htdocs directory.
	 * 
	 * @throws IOException
	 *             Throws IOException.
	 */
	@Logged
	protected void configureHtdocs() throws IOException {
		File wwwLumisDir = PathEnum.LUMISPORTAL_DIR_STATICLUMIS.getFile();

		Predicate<File> predicate = new Predicate<File>() {

			@Override
			public boolean apply(File input) {
				boolean result = false;
				try {
					Predicate<CharSequence> ignorePatternPredicate = Predicates.containsPattern("www/lumis/doc.*");
					Predicate<CharSequence> jspPredicate = Predicates.containsPattern(".jsp$");
					Predicate<Object> isDirectoryPredicate = input.isDirectory() ? Predicates.alwaysFalse() : Predicates.alwaysTrue();
					result = Predicates.and(Predicates.not(Predicates.or(ignorePatternPredicate, jspPredicate)), isDirectoryPredicate).apply(getPath(input));
				} catch (IOException e) {
				}
				return result;
			}
		};
		List<File> fileList = Files.fileTreeTraverser().preOrderTraversal(wwwLumisDir).filter(predicate).toList();

		for (File file : fileList) {
			File newFile = new File(getPath(file).replace(PathEnum.LUMISPORTAL_DIR_STATICLUMIS.getPath(), PathEnum.HTDOCS_DIR_LUMIS.getPath()));
			if (!newFile.getParentFile().exists()) {
				Files.createParentDirs(newFile);
			}
			Files.copy(file, newFile);
		}
	}

	/**
	 * Configure web.xml file.
	 * 
	 * @throws IOException
	 *             Throws IOException.
	 */
	@Logged
	protected void configureWebXml() throws IOException {
		File file = PathEnum.LUMISPORTAL_FILE_WEBXML.getFile();

		List<String> lines = Files.readLines(file, Charsets.UTF_8);
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).contains("lumisDataPath")) {
				lines.set(i + 1, String.format("\t\t<param-value>%s</param-value>", PathEnum.LUMISPORTAL_DIR_LUMISDATA.getPath()));
			}
		}

		saveToFile(lines, file);
	}

	/**
	 * Configure jboss-web.xml from Lumis Portal.
	 * 
	 * @throws IOException
	 *             Throws IOException.
	 */
	@Logged
	protected void configureJbosswebXml() throws IOException {
		File file = PathEnum.LUMISPORTAL_FILE_JBOSSWEBXML.getFile();
		if (!file.exists()) {
			return;
		}

		List<String> lines = Files.readLines(file, Charsets.UTF_8);
		for (int i = 0; i < lines.size(); i++) {
			lines.set(i, lines.get(i).replace("myapp", getContextName()));
		}

		saveToFile(lines, file);
	}

	/**
	 * Configure lumisportalconfig.xml file.
	 * 
	 * @throws IOException
	 *             Throws IOException.
	 */
	@Logged
	protected void configureLumisportalconfigXml() throws IOException {
		File file = PathEnum.LUMISPORTAL_FILE_LUMISPORTALCONFIGXML.getFile();

		List<String> lines = Files.readLines(file, Charsets.UTF_8);
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);

			if (line.contains("<serverId>")) {
				lines.set(i, String.format("\t<serverId>%s-%d</serverId>", InetAddress.getLocalHost().getHostName(), frameworkUrl.getPort() - 8080));
			} else if (line.contains("<frameworkUrl>")) {
				lines.set(i, String.format("\t\t<frameworkUrl>%s</frameworkUrl>", frameworkUrl.toString()));
			} else if (line.contains("<webRootPath>")) {
				if (!lines.get(i - 1).endsWith("-->")) {
					lines.set(i - 1, String.format("%s -->", lines.get(i - 1)));
				}
				lines.set(i, String.format("\t<webRootPath>%s</webRootPath>", PathEnum.HTDOCS_DIR.getPath()));
				lines.set(i + 1, "");
			} else if (line.contains("</layoutFile>")) {
				lines.set(i - 2, "\t\t-->");
				lines.set(i - 1, "\t\t<pollIntervalSecs>300</pollIntervalSecs>");
			}

			if (line.contains("<htmlGeneration>")) {
				if (!lines.get(i - 1).endsWith("-->")) {
					lines.set(i - 1, String.format("%s -->", lines.get(i - 1)));
				}
			} else if (line.contains("</htmlGeneration>")) {
				lines.set(i + 1, "");
			}
		}

		saveToFile(lines, file);
	}

	/**
	 * Configure setup directory files.
	 * 
	 * @throws IOException
	 *             Throws IOException.
	 */
	@Logged
	protected void configureSetup_Files() throws IOException {
		File setupDir = PathEnum.LUMISPORTAL_DIR_SETUP.getFile();
		File mysqlDriverFile = (mysqldriverFile.startsWith("http") || mysqldriverFile.startsWith("ftp")) ? downloadFile(new URL(mysqldriverFile)) : new File(mysqldriverFile);

		String mysqlDriverFileName = mysqlDriverFile.getName();
		Files.copy(mysqlDriverFile, new File(PathEnum.LUMISPORTAL_DIR_CONTEXTLIB.getPath(), mysqlDriverFileName));

		for (File file : setupDir.listFiles(new PatternFilenameFilter(".*\\.(sh|cmd|bat)"))) {
			List<String> lines = Files.readLines(file, Charsets.UTF_8);

			for (int i = 0; i < lines.size(); i++) {
				String line = lines.get(i);

				if (line.contains("JAVA_HOME=")) {
					lines.set(i, line.replace("JAVA_HOME=", String.format("JAVA_HOME=%s", getFormatedPath(System.getProperty("java.home")))));
				}
				if (line.contains(". setclasspath.sh")) {
					lines.set(i, line.replace(". setclasspath.sh", ". ./setclasspath.sh"));
				}
				if (line.contains("mysql-connector-java")) {
					lines.set(i, line.replace(line.substring(line.indexOf("mysql-connector-java")), mysqlDriverFileName));
				}
			}

			saveToFile(lines, file);

			file.setExecutable(true);
		}
	}

	/**
	 * Configure lumishibernate.cfg.xml file.
	 * 
	 * @throws IOException
	 *             Throws IOException.
	 */
	@Logged
	protected void configureLumishibernateCfgXml() throws IOException {
		File lumishibernatecfgxmlFile = PathEnum.LUMISPORTAL_FILE_LUMISHIBERNATECFGXML.getFile();
		File lumishibernatecfgxmlConnectionFile = new File(PathEnum.LUMISPORTAL_FILE_LUMISHIBERNATECFGXML.getPath() + ".connection");
		File lumishibernatecfgxmlDatasourceFile = new File(PathEnum.LUMISPORTAL_FILE_LUMISHIBERNATECFGXML.getPath() + ".datasource");

		List<String> lumishibernateCfgXmlLines = database.configureLumishibernateCfgXml(lumishibernatecfgxmlFile);
		saveToFile(lumishibernateCfgXmlLines, lumishibernatecfgxmlConnectionFile);

		Map<String, Object> model = Maps.newHashMap();
		model.put("datasourceName", datasourceName);
		saveToFile(applyTemplateModel(ResourceEnum.LUMISPORTAL_LUMISHIBERNATECFGXML.getText(), model), lumishibernatecfgxmlDatasourceFile);

		Files.copy(lumishibernatecfgxmlConnectionFile, lumishibernatecfgxmlFile);
	}

	/**
	 * Execute initializeportal script.
	 * 
	 * @throws IOException
	 *             Throws IOException.
	 */
	protected void initializePortal() throws IOException {
		File setupDir = PathEnum.LUMISPORTAL_DIR_SETUP.getFile();
		File scriptDBFile = new File(setupDir, String.format("db_%s.sql", databaseType.toLowerCase()));

		String generatesqlCommand = (isWindows) ? "cmd /c generatesql.cmd" : "./generatesql.sh";
		String initializeportalCommand = (isWindows) ? "cmd /c initializeportal.cmd" : "./initializeportal.sh";

		Connection connection = null;
		Statement statement = null;

		int result = -1;

		try {
			connection = database.createConnection();
			statement = connection.createStatement();

			String query = "select count(*) as count from lum_User;";
			database.executeSql(query, statement);

			ResultSet resultSet = statement.getResultSet();
			while (resultSet.next()) {
				result = resultSet.getInt(1);
			}
		} catch (SQLException e) {
			executeCommand(generatesqlCommand, setupDir);
			int scriptResult = database.executeScript(scriptDBFile);
			if (scriptResult > 0) {
				result = 0;
			}
		} catch (MalformedURLException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			Log.error(e.getLocalizedMessage());
		} finally {
			database.closeConnection(connection, statement);
		}

		if (result == 0) {
			executeCommand(initializeportalCommand, setupDir);
		}
	}
}
