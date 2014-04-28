package infradev.lumis.lumisportalinstaller.javaserver.jboss;

import infradev.lumis.lumisportalinstaller.common.PathEnum;
import infradev.lumis.lumisportalinstaller.common.ResourceEnum;
import infradev.lumis.lumisportalinstaller.common.inject.annotation.Logged;
import infradev.lumis.lumisportalinstaller.javaserver.AbstractJavaServer;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import com.google.common.base.Ascii;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

/**
 * Abstract class to JBoss Java Servers.
 * 
 * @author Alexandre Ribeiro de Souza
 */
public abstract class AbstractJBoss extends AbstractJavaServer {

	protected String originalJavaOpts = "-Xms64m -Xmx512m -XX:MaxPermSize=256m";
	protected String[] profiles = { "standalone", "domain" };
	protected String lumisportalmoduleDirPath;
	protected String sha1;

	@Override
	protected void copyLumisportal_Files() throws IOException {
		copyJboss_Module();
		configureMysql_Module();
		removeJbosswebXml();
	}

	@Override
	protected void configureJavaServer() throws IOException {
		configureDeployment();
		configureProfiles();
		configureHostXml();
		configureRunConf();
	}

	/**
	 * Copy JBoss Module from Lumis Portal to JBoss installation.
	 * 
	 * @throws IOException
	 *             Throws IOException.
	 */
	@Logged
	protected void copyJboss_Module() throws IOException {
		copyDir(new File(PathEnum.LUMISPORTAL_DIR.getFile(), lumisportalmoduleDirPath), PathEnum.JBOSS_DIR_MODULES.getFile());
	}

	/**
	 * Configure MySQL Module.
	 * 
	 * @throws IOException
	 *             Throws IOException.
	 */
	@Logged
	protected void configureMysql_Module() throws IOException {
		File file = PathEnum.JBOSS_FILE_MYSQLDRIVERMODULEXML.getFile();

		File mysqlDriverFile = (mysqldriverFile.startsWith("http") || mysqldriverFile.startsWith("ftp")) ? downloadFile(new URL(mysqldriverFile)) : new File(mysqldriverFile);
		String mysqlDriverFileName = mysqlDriverFile.getName();

		List<String> lines = Files.readLines(file, Charsets.UTF_8);
		for (int i = 0; i < lines.size(); i++) {
			lines.set(i, lines.get(i).replace("mysql-connector-java-X.X.X-bin.jar", mysqlDriverFileName));
		}

		saveToFile(lines, file);

		Files.copy(mysqlDriverFile, new File(file.getParentFile(), mysqlDriverFileName));
	}

	/**
	 * Remove jboss-web.xml from Lumis Portal.
	 * 
	 * @throws IOException
	 *             Throws IOException.
	 */
	@Logged
	protected void removeJbosswebXml() throws IOException {
		File file = PathEnum.LUMISPORTAL_FILE_JBOSSWEBXML.getFile();
		if (file.exists()) {
			file.delete();
			new File(file.getAbsolutePath() + ".original").delete();
		}
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

		File warFile = new File(String.format(PathEnum.JBOSS_FILE_STANDALONEWAR.getPath(), getContextName()));

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

		File warDoDeployFile = new File(getPath(warFile) + ".dodeploy");
		warDoDeployFile.createNewFile();

		if (deployAsWar) {
			sha1 = Files.hash(contextRootFile, Hashing.sha1()).toString();
			String firstPart = sha1.substring(0, 2);
			String secondPart = sha1.substring(2);

			File contentFile = new File(String.format(PathEnum.JBOSS_FILE_DOMAINWAR.getPath(), firstPart, secondPart));
			Files.createParentDirs(contentFile);

			command = String.format(format, contextRootFile.getCanonicalPath(), contentFile.getCanonicalPath());

			executeCommand(command);
		}
	}

	/**
	 * Configure standalone and domain profile files.
	 * 
	 * @throws IOException
	 *             Throws IOException.
	 */
	@Logged
	protected void configureProfiles() throws IOException {
		String[] javaOptsArray = javaOpts.split(" ");
		String xms = null;
		String xmx = null;
		String maxPermSize = null;
		for (String str : javaOptsArray) {
			if (str.startsWith("-Xms")) {
				xms = str.substring(4);
			}
			if (str.startsWith("-Xmx")) {
				xmx = str.substring(4);
			}
			if (str.startsWith("-XX:MaxPermSize")) {
				maxPermSize = str.substring(16);
			}
		}

		Map<String, Object> model = Maps.newHashMap();
		model.put("lumisDataPath", PathEnum.LUMISPORTAL_DIR_LUMISDATA.getPath());
		model.put("datasourceName", datasourceName);
		model.put("databaseURL", databaseUrl);
		model.put("databaseType", databaseType.toLowerCase());
		model.put("databaseClassname", database.getClassname());
		model.put("databaseUsername", databaseUser);
		model.put("databasePassword", databasePassword);
		model.put("contextName", getContextName());
		model.put("contextSha1", sha1);
		model.put("Xms", xms);
		model.put("Xmx", xmx);
		model.put("MaxPermSize", maxPermSize);

		for (String profile : profiles) {
			File file = new File(String.format(PathEnum.JBOSS_FILE_PROFILEXML.getPath(), profile));

			List<String> lines = Files.readLines(file, Charsets.UTF_8);

			for (int i = 0; i < lines.size(); i++) {
				String line = lines.get(i);

				if (line.contains("jboss:domain:ee")) {
					if (line.endsWith("/>")) {
						lines.set(i, line.replace("/>", ">"));
						lines.add(i + 1, "        </subsystem>");
					}
					lines.addAll(i + 1, ResourceEnum.JBOSS_JBOSSDOMAINEE_PART.getList());
				} else if (line.contains("jboss:domain:deployment-scanner")) {
					lines.set(i + 1, lines.get(i + 1).replace("/>", " deployment-timeout=\"300\"/>"));
				} else if (line.contains("jboss:domain:jaxrs") || line.contains("org.jboss.as.jaxrs") || line.contains("org.jboss.as.webservices")) {
					lines.remove(i);
				} else if (line.contains("jboss:domain:webservices")) {
					removeIndexUntilMatch(lines, i, "</subsystem>");
				} else if (line.contains("jboss:domain:security")) {
					lines.addAll(i + 2, ResourceEnum.JBOSS_JBOSSDOMAINSECURITY_PART.getList());
				} else if (line.contains("enable-welcome-root=\"true\"")) {
					lines.set(i, lines.get(i).replace("enable-welcome-root=\"true\"", "enable-welcome-root=\"false\""));
				} else if (profile.equals("standalone") && line.contains("</extensions>")) {
					lines.addAll(i + 1, ResourceEnum.JBOSS_SYSTEMPROPERTIES_PART.getList());
				} else if (line.contains("<datasources>")) {
					lines.addAll(i + 1, ResourceEnum.JBOSS_DATASOURCE_PART.getList());
				} else if (line.contains("<drivers>")) {
					lines.addAll(i + 1, ResourceEnum.JBOSS_DRIVER_PART.getList());
				} else if (line.contains("</socket-binding-groups>")) {
					lines.addAll(i + 1, ResourceEnum.JBOSS_DEPLOYMENT_PART.getList());
				} else if (line.contains("<server-groups>")) {
					removeIndexUntilMatch(lines, i, "</server-groups>");
					lines.addAll(i, ResourceEnum.JBOSS_SERVERGROUPS_PART.getList());
				}
			}
			String str = applyTemplateModel(Joiner.on((char) Ascii.LF).join(lines), model);
			saveToFile(str, file);
		}
	}

	/**
	 * Configure host.xml file
	 * 
	 * @throws IOException
	 *             Throws IOException.
	 */
	@Logged
	protected void configureHostXml() throws IOException {
		File file = PathEnum.JBOSS_FILE_DOMAINHOSTXML.getFile();

		Map<String, Object> model = Maps.newHashMap();
		model.put("lumisDataPath", PathEnum.LUMISPORTAL_DIR_LUMISDATA.getPath());

		List<String> lines = Files.readLines(file, Charsets.UTF_8);
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);

			if (line.contains("<servers>")) {
				removeIndexUntilMatch(lines, i, "</servers>");
				lines.addAll(i, ResourceEnum.JBOSS_SERVERS_PART.getList());
			}
		}

		String str = applyTemplateModel(Joiner.on((char) Ascii.LF).join(lines), model);
		saveToFile(str, file);
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

		for (String profile : profiles) {
			configureRunConfFile(String.format(PathEnum.JBOSS_FILE_PROFILECONF.getPath(), profile), model);
			configureRunConfFile(String.format(PathEnum.JBOSS_FILE_PROFILECONF.getPath() + ".bat", profile), model);
		}
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
			} else if (file.getName().startsWith("standalone")) {
				if (line.matches(String.format(".*(?i)%s.*", originalJavaOpts))) {
					line = line.replaceAll(String.format("(?i)%s", originalJavaOpts), "{{javaOpts}}");
				}
			}

			lines.set(i, line);
		}

		String str = applyTemplateModel(Joiner.on((char) Ascii.LF).join(lines), model);
		saveToFile(str, file);
	}
}
