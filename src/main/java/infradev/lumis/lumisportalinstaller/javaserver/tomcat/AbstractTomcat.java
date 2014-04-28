package infradev.lumis.lumisportalinstaller.javaserver.tomcat;

import infradev.lumis.lumisportalinstaller.common.PathEnum;
import infradev.lumis.lumisportalinstaller.common.ResourceEnum;
import infradev.lumis.lumisportalinstaller.common.inject.annotation.Logged;
import infradev.lumis.lumisportalinstaller.javaserver.AbstractJavaServer;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.io.Files;

/**
 * Abstract class to Tomcat Java Servers.
 * 
 * @author Alexandre Ribeiro de Souza
 */
public abstract class AbstractTomcat extends AbstractJavaServer {

	protected String connectorConfig = "maxPostSize=\"2097152\" URIEncoding=\"UTF-8\" redirectPort=";

	@Override
	@Logged
	protected void copyLumisportal_Files() throws IOException {
		copyDir(PathEnum.LUMISPORTAL_DIR_SHAREDLIB.getFile(), new File(PathEnum.JAVASERVER_DIR.getFile(), getSharedDir()));
		copyDir(PathEnum.LUMISPORTAL_DIR_ENDORSEDLIB.getFile(), new File(PathEnum.JAVASERVER_DIR.getFile(), getEndorsedDir()));
	}

	@Override
	protected void configureJavaServer() throws IOException {
		configureContextXml();
		configureServerXml();
		configureSetEnv();
	}

	/**
	 * Get the path of shared library directory.
	 * 
	 * @return Path to shared library directory.
	 */
	protected String getSharedDir() {
		return "lib";
	}

	/**
	 * Get the path of endorsed library directory.
	 * 
	 * @return Path to endorsed library directory.
	 */
	protected String getEndorsedDir() {
		return "endorsed";
	}

	/**
	 * Configure context file.
	 * 
	 * @throws IOException
	 *             Throws IOException.
	 */
	@Logged
	protected void configureContextXml() throws IOException {
		File contextFile = new File(String.format(PathEnum.TOMCAT_FILE_CONTEXTXML.getPath(), getContextName()));
		File contextRootFile = new File(PathEnum.LUMISPORTAL_DIR.getFile(), deployAsWar ? "lumisportal.war" : "www");

		Map<String, Object> model = Maps.newHashMap();
		model.put("datasourceName", datasourceName.replace("java:/comp/env/", ""));
		model.put("contextName", getContextPath());
		model.put("contextRoot", getPath(contextRootFile));
		model.put("lumisDataPath", getPath(PathEnum.LUMISPORTAL_DIR_LUMISDATA.getFile()));
		model.put("driverClass", database.getClassname());
		model.put("databaseURL", databaseUrl);
		model.put("databaseUsername", databaseUser);
		model.put("databasePassword", databasePassword);

		saveToFile(applyTemplateModel(ResourceEnum.TOMCAT_PORTALXML.getText(), model), contextFile);
	}

	/**
	 * Configure server.xml file.
	 * 
	 * @throws IOException
	 *             Throws IOException.
	 */
	@Logged
	protected void configureServerXml() throws IOException {
		File file = PathEnum.TOMCAT_FILE_SERVERXML.getFile();

		String hostName = InetAddress.getLocalHost().getHostName();
		int portOffSet = frameworkUrl.getPort() - 8080;
		int shutdownPort = 8005 - portOffSet;
		int ajpPort = 8009 - portOffSet;

		List<String> lines = Files.readLines(file, Charsets.UTF_8);
		for (int i = 0; i < lines.size(); i++) {
			lines.set(
					i,
					lines.get(i)
							.replace("redirectPort=", connectorConfig)
							.replace("defaultHost=\"localhost\">",
									String.format("defaultHost=\"localhost\" jvmRoute=\"%s-%d\">", hostName, portOffSet))
							.replace("8005", String.format("%d", shutdownPort)).replace("8080", String.format("%d", frameworkUrl.getPort()))
							.replace("8009", String.format("%d", ajpPort)));
		}

		saveToFile(lines, file);
	}

	/**
	 * Configure environment files.
	 * 
	 * @throws IOException
	 *             Throws IOException.
	 */
	protected void configureSetEnv() throws IOException {
		Map<String, Object> model = Maps.newHashMap();
		model.put("javaOpts", javaOpts);
		model.put("javaHome", getFormatedPath(System.getProperty("java.home")));
		model.put("lumisDataPath", PathEnum.LUMISPORTAL_DIR_LUMISDATA.getPath());

		saveToFile(applyTemplateModel(ResourceEnum.TOMCAT_SETENVBAT.getText(), model), PathEnum.TOMCAT_FILE_SETENVBAT.getFile());
		saveToFile(applyTemplateModel(ResourceEnum.TOMCAT_SETENVSH.getText(), model), PathEnum.TOMCAT_FILE_SETENVSH.getFile());
	}
}
