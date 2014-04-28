package infradev.lumis.lumisportalinstaller.javaserver.jboss;

import infradev.lumis.lumisportalinstaller.common.PathEnum;
import infradev.lumis.lumisportalinstaller.common.ResourceEnum;
import infradev.lumis.lumisportalinstaller.common.inject.annotation.Logged;
import infradev.lumis.lumisportalinstaller.javaserver.tomcat.AbstractTomcat;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import com.google.common.base.Ascii;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;

/**
 * JBoss 5 Java Server handler.
 * 
 * @author Alexandre Ribeiro de Souza
 */
@Named("JBOSS5")
public class JBoss5Impl extends AbstractTomcat {

	public JBoss5Impl() {
		connectorConfig = "maxPostSize=\"2097152\" emptySessionPath=\"true\" URIEncoding=\"UTF-8\" redirectPort=";
	}

	@Override
	protected void copyLumisportal_Files() throws IOException {
		super.copyLumisportal_Files();

		List<File> moveFiles = Lists.newArrayList();
		moveFiles.add(new File(PathEnum.JAVASERVER_DIR.getFile(), "server/default/lib/xercesImpl.jar"));
		moveFiles.add(new File(PathEnum.LUMISPORTAL_DIR_CONTEXTLIB.getFile(), "serializer.jar"));
		moveFiles.add(new File(PathEnum.LUMISPORTAL_DIR_CONTEXTLIB.getFile(), "xalan.jar"));

		List<File> deleteFiles = Lists.newArrayList();
		deleteFiles.add(new File(PathEnum.JAVASERVER_DIR.getFile(), "server/default/lib/commons-logging-api.jar"));
		deleteFiles.add(new File(PathEnum.LUMISPORTAL_DIR_CONTEXTLIB.getFile(), "jboss-common-core-2.2.0.GA.jar"));
		deleteFiles.add(new File(PathEnum.LUMISPORTAL_DIR_CONTEXTLIB.getFile(), "jta.jar"));

		for (File from : moveFiles) {
			File to = new File(PathEnum.JAVASERVER_DIR.getFile(), String.format("lib/endorsed/%s", from.getName()));
			if (to.exists()) {
				to.delete();
			}
			Files.move(from, to);
		}

		for (File file : deleteFiles) {
			file.delete();
		}
	}

	@Override
	protected String getSharedDir() {
		return "server/default/lib";
	}

	@Override
	protected String getEndorsedDir() {
		return "lib/endorsed";
	}

	@Override
	protected void configureJavaServer() throws IOException {
		configureRunConf();
		configureLoginconfigXml();
		configureBindingPort();
		configureServerXml();
		configureDeployment();
		configureDatasource();
	}

	/**
	 * Configure run.conf and run.conf.bat files.
	 * 
	 * @throws IOException
	 *             Throws IOException.
	 */
	@Logged
	protected void configureRunConf() throws IOException {
		Map<String, Object> model = Maps.newHashMap();
		model.put("javaOpts", javaOpts);
		model.put("javaHome", getFormatedPath(System.getProperty("java.home")));
		model.put("lumisDataPath", getPath(PathEnum.LUMISPORTAL_DIR_LUMISDATA.getFile()));

		configureRunConfFile(String.format(PathEnum.JBOSS_FILE_PROFILECONF.getPath(), "run"), model);
		configureRunConfFile(String.format(PathEnum.JBOSS_FILE_PROFILECONF.getPath() + ".bat", "run"), model);
	}

	/**
	 * Apply a model to a run.conf file
	 * 
	 * @param fileName
	 *            Name of configuration file.
	 * @param model
	 *            Model to be applyed.
	 * @throws IOException
	 *             Throws IOException.
	 */
	protected void configureRunConfFile(String fileName, Map<String, Object> model) throws IOException {
		File file = new File(fileName);

		List<String> lines = Files.readLines(file, Charsets.UTF_8);
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);

			if (line.contains("JAVA_HOME=")) {
				line = (fileName.endsWith(".bat")) ? "set \"JAVA_HOME={{javaHome}}\"" : "JAVA_HOME=\"{{javaHome}}\"";
			} else if (line.matches(".*(?i)-Xms128m -Xmx512m -XX:MaxPermSize=256m.*")) {
				line = line.replaceAll("(?i)-Xms128m -Xmx512m -XX:MaxPermSize=256m",
						"{{javaOpts}} -Djavax.xml.transform.TransformerFactory=org.apache.xalan.xsltc.trax.SmartTransformerFactoryImpl "
								+ "-Dlumis.portal.lumisDataPath={{lumisDataPath}}");
			}

			lines.set(i, line);
		}

		String str = applyTemplateModel(Joiner.on((char) Ascii.LF).join(lines), model);
		saveToFile(str, file);
	}

	/**
	 * Configure login-config.xml file.
	 * 
	 * @throws IOException
	 *             Throws IOException.
	 */
	@Logged
	protected void configureLoginconfigXml() throws IOException {
		File file = new File(PathEnum.JAVASERVER_DIR.getFile(), "server/default/conf/login-config.xml");

		List<String> lines = Files.readLines(file, Charsets.UTF_8);
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).equalsIgnoreCase("</policy>")) {
				StringBuilder sb = new StringBuilder();
				sb.append("  <application-policy name=\"LumisPortal\">\n");
				sb.append("    <authentication>\n");
				sb.append("      <login-module code=\"lumis.portal.authentication.LumisLoginModule\" flag=\"sufficient\"/>\n");
				sb.append("    </authentication>\n");
				sb.append("  </application-policy>\n");
				sb.append("\n</policy>\n");
				lines.set(i, sb.toString());
			}
		}

		saveToFile(lines, file);
	}

	/**
	 * Configure bindings-jboss-beans.xml binding port.
	 * 
	 * @throws IOException
	 *             Throws IOException.
	 */
	@Logged
	protected void configureBindingPort() throws IOException {
		File file = new File(PathEnum.JAVASERVER_DIR.getFile(), "server/default/conf/bindingservice.beans/META-INF/bindings-jboss-beans.xml");

		Integer portOffSet = frameworkUrl.getPort() - 8080;
		List<Integer> portOffSetList = Lists.newArrayList(100, 200, 300);

		List<String> lines = Files.readLines(file, Charsets.UTF_8);
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).equalsIgnoreCase("jboss.service.binding.set:ports-default") && portOffSetList.contains(portOffSet)) {
				lines.set(i, lines.get(i).replace("default", "0" + portOffSet / 100));
			}
		}

		saveToFile(lines, file);
	}

	@Override
	@Logged
	protected void configureServerXml() throws IOException {
		File file = new File(PathEnum.JAVASERVER_DIR.getFile(), "server/default/deploy/jbossweb.sar/server.xml");

		String hostName = InetAddress.getLocalHost().getHostName();
		int portOffSet = frameworkUrl.getPort() - 8080;

		List<String> lines = Files.readLines(file, Charsets.UTF_8);
		for (int i = 0; i < lines.size(); i++) {
			lines.set(
					i,
					lines.get(i)
							.replace("redirectPort=", connectorConfig)
							.replace("defaultHost=\"localhost\">",
									String.format("defaultHost=\"localhost\" jvmRoute=\"%s-%d\">", hostName, portOffSet)));
		}

		saveToFile(lines, file);
	}

	/**
	 * Configure the way Lumis Portal will be deployed.
	 * 
	 * @throws IOException
	 *             Throws IOException.
	 */
	@Logged
	protected void configureDeployment() throws IOException {
		File contextRootFile = new File(PathEnum.LUMISPORTAL_DIR.getFile(), deployAsWar ? "lumisportal.war" : "www");
		File warFile = new File(PathEnum.JAVASERVER_DIR.getFile(), String.format("server/default/deploy/%s.war", getContextName()));

		if (warFile.exists()) {
			if (warFile.isDirectory()) {
				deleteDirectory(warFile);
			} else {
				warFile.delete();
			}
		}

		String format = "ln -s %1$s %2$s";
		if (isWindows) {
			if (deployAsWar) {
				format = "cmd /c mklink %2$s %1$s";
			} else {
				format = "cmd /c mklink /D %2$s %1$s";
			}
		}

		String command = String.format(format, contextRootFile.getCanonicalPath(), warFile.getCanonicalPath());

		executeCommand(command);
	}

	/**
	 * Configure Lumis Portal datasource file.
	 * 
	 * @throws IOException
	 *             Throws IOException.
	 */
	@Logged
	protected void configureDatasource() throws IOException {
		File file = new File(PathEnum.JAVASERVER_DIR.getFile(), "server/default/deploy/lumisportal-ds.xml");

		String dbType = "mySQL";
		String connectionValidator = "";

		switch (databaseType) {
		case "MYSQL":
			dbType = "mySQL";
			connectionValidator = "<valid-connection-checker-class-name>org.jboss.resource.adapter.jdbc.vendor.MySQLValidConnectionChecker</valid-connection-checker-class-name>\n";
			break;
		case "SQLSERVER":
			dbType = "MS SQLSERVER2000";
			connectionValidator = "<check-valid-connection-sql>SELECT 1 FROM sysobjects</check-valid-connection-sql>\n";
			break;
		case "ORACLE":
			dbType = "Oracle9i";
			connectionValidator = "<valid-connection-checker-class-name>org.jboss.resource.adapter.jdbc.vendor.OracleValidConnectionChecker</valid-connection-checker-class-name>\n";
			break;
		}

		Map<String, Object> model = Maps.newHashMap();
		model.put("datasourceName", datasourceName.replace("java:", ""));
		model.put("driverClass", database.getClassname());
		model.put("databaseURL", databaseUrl);
		model.put("databaseUsername", databaseUser);
		model.put("databasePassword", databasePassword);
		model.put("connectionValidator", connectionValidator);
		model.put("databaseType", dbType);

		saveToFile(applyTemplateModel(ResourceEnum.JBOSS5_LUMISPORTALDSXML.getText(), model), file);
	}
}
