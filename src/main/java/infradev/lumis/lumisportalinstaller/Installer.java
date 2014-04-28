package infradev.lumis.lumisportalinstaller;

import infradev.lumis.lumisportalinstaller.common.PathEnum;
import infradev.lumis.lumisportalinstaller.common.ResourceEnum;
import infradev.lumis.lumisportalinstaller.common.tools.Log;
import infradev.lumis.lumisportalinstaller.database.AbstractDatabase;
import infradev.lumis.lumisportalinstaller.javaserver.AbstractJavaServer;
import infradev.lumis.lumisportalinstaller.lumisportal.AbstractLumisPortal;

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.sisu.EagerSingleton;
import org.eclipse.sisu.space.SpaceModule;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.WireModule;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.inject.Guice;

/**
 * Installer class.
 *
 * @author Alexandre Ribeiro de Souza
 */
@Named
@EagerSingleton
public final class Installer {

	public final static String VERSION = "1.0.0";

	public final static Properties properties = new Properties();

	private final Map<String, AbstractDatabase> databases;
	private final Map<String, AbstractJavaServer> javaservers;
	private final AbstractLumisPortal lumisportal;
	private final String databaseType;
	private final String javaserverType;

	@Inject
	public Installer(Map<String, AbstractDatabase> databases, Map<String, AbstractJavaServer> javaservers, AbstractLumisPortal lumisportal,
			@Named("${database.type:-MYSQL}") String databaseType, @Named("${javaserver.type:-TOMCAT7}") String javaserverType) {
		this.databases = databases;
		this.javaservers = javaservers;
		this.lumisportal = lumisportal;
		this.databaseType = databaseType;
		this.javaserverType = javaserverType;

		run();
	}

	public void run() {
		validateTypes();

		String separator = Strings.repeat("-", 52);
		Log.info(separator);
		Log.info(String.format("Starting Lumis Portal Installer %s", VERSION));
		Log.info(separator);

		executeInstall();

		Log.info("");
		Log.info(separator);
		Log.info(String.format("Finished Lumis Portal Installer %s", VERSION));
		Log.info(separator);
	}

	private void validateTypes() {
		boolean hasError = false;

		PathEnum.setJavaserverType((String) javaserverType);
		if (!javaservers.containsKey(javaserverType)) {
			Log.error(String.format("%s is not an valid javaserver type of:", javaserverType));
			for (String str : javaservers.keySet()) {
				Log.error(String.format(" - %s", str));
			}
			Log.error("");
			hasError = true;
		}

		if (!databases.containsKey(databaseType)) {
			Log.error(String.format("%s is not an valid database type of:", databaseType));
			for (String str : databases.keySet()) {
				Log.error(String.format(" - %s", str));
			}
			Log.error("");
			hasError = true;
		}

		if (hasError) {
			System.exit(2);
		}
	}

	private void executeInstall() {
		try {
			lumisportal.install();
			javaservers.get(javaserverType).install();
		} catch (IOException e) {
			Log.error(e.getLocalizedMessage());
		}
	}

	public static void main(String[] args) {
		Properties loadedProperties = new Properties();

		try {
			if (args.length > 0) {
				if (args[0].equals("-sample")) {
					createPropertiesSample();
				}
				loadedProperties.load(new FileInputStream(args[0]));
			}
		} catch (IOException e) {
		}

		while (!Installer.verifyProperties(loadedProperties)) {
		}
		Installer.install(loadedProperties);
	}

	public static void install(Properties properties) {
		Installer.properties.putAll(properties);

		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		Guice.createInjector(
				new WireModule( // auto-wires unresolved dependencies
				new SpaceModule( // scans and binds @Named components
				new URLClassSpace(classloader) // abstracts class/resource finding
				)));
	}

	private static boolean verifyProperties(Properties properties) {
		Console console = System.console();

		Map<String, String> defaultProperties = Maps.newLinkedHashMap();
		defaultProperties.put("lumisportal.installFile", "] - default = [./lumisportal_7.1.1.140331.zip");
		defaultProperties.put("lumisportal.extractDoc", "] - default = [false");
		defaultProperties.put("lumisportal.frameworkUrl", "] - default = [http://localhost:8080");
		defaultProperties.put("javaserver.type", "] - default = [TOMCAT7");
		defaultProperties.put("javaserver.installFile", "] - default = [http://central.maven.org/maven2/org/apache/tomcat/tomcat/7.0.50/tomcat-7.0.50.zip");
		defaultProperties.put("javaserver.javaopts", "] - default = [-Xms1024m -Xmx1024m -XX:MaxPermSize=256m");
		defaultProperties.put("javaserver.deployAsWar", "] - default = [false");
		defaultProperties.put("database.mysqldriverFile", "] - default = [http://central.maven.org/maven2/mysql/mysql-connector-java/5.1.30/mysql-connector-java-5.1.30.jar");
		defaultProperties.put("database.user", "] - default = [lumis");
		defaultProperties.put("database.password", "");
		defaultProperties.put("database.url", "] - default = [jdbc:mysql://localhost/lumisportal?characterEncoding=UTF-8&zeroDateTimeBehavior=convertToNull");

		Map<String, String> serverTypes = Maps.newLinkedHashMap();
		serverTypes.put("TOMCAT7", "http://central.maven.org/maven2/org/apache/tomcat/tomcat/7.0.50/tomcat-7.0.50.zip");
		serverTypes.put("TOMCAT8", "http://central.maven.org/maven2/org/apache/tomcat/tomcat/8.0.5/tomcat-8.0.5.zip");
		serverTypes.put("JBOSS5", "./jboss-5.1.0.GA.zip");
		serverTypes.put("JBOSSAS7", "http://download.jboss.org/jbossas/7.1/jboss-as-7.1.1.Final/jboss-as-7.1.1.Final.zip");
		serverTypes.put("JBOSSEAP6", "./jboss-eap-6.2.0.zip");
		serverTypes.put("WILDFLY8", "http://download.jboss.org/wildfly/8.0.0.Final/wildfly-8.0.0.Final.zip");

		System.out.println();
		System.out.println(String.format(Strings.repeat("-", 26)));
		for (Entry<String, String> entry : defaultProperties.entrySet()) {
			String propertyValue = properties.getProperty(entry.getKey(), entry.getValue());
			if (entry.getKey().equalsIgnoreCase("database.password") && !propertyValue.isEmpty()) {
				propertyValue = Strings.repeat("*", propertyValue.length());
			}
			System.out.println(String.format("%s%s: [%s]", entry.getKey(), Strings.repeat(" ", 25 - entry.getKey().length()), propertyValue));
		}
		System.out.println(String.format(Strings.repeat("-", 26)));
		System.out.println("");

		String line = console.readLine("Confirm? [Yes/No] ");
		System.out.println();
		if (line.equalsIgnoreCase("y") || line.equalsIgnoreCase("yes")) {
			return true;
		}

		System.out.println(">> Press Enter to maintain actual value.\n");

		for (Entry<String, String> entry : defaultProperties.entrySet()) {
			String spaces = Strings.repeat(" ", 25 - entry.getKey().length());
			String spaces27 = Strings.repeat(" ", 27);
			if (entry.getKey().equalsIgnoreCase("database.password")) {
				line = String.valueOf(console.readPassword(String.format("%s%s: [%s]\n%s", entry.getKey(), spaces,
						Strings.repeat("*", properties.getProperty(entry.getKey(), entry.getValue()).length()), spaces27)));
			} else if (entry.getKey().equalsIgnoreCase("javaserver.type")) {
				line = console.readLine(String.format("%s%s: [%s] - %s\n%s", entry.getKey(), spaces,
						properties.getProperty(entry.getKey(), entry.getValue()), serverTypes.keySet().toString(), spaces27));
			} else {
				line = console.readLine(String.format("%s%s: [%s]\n%s", entry.getKey(), spaces,
						properties.getProperty(entry.getKey(), entry.getValue()), spaces27));
			}

			if (!line.isEmpty()) {
				properties.put(entry.getKey(), line);
			}

			if (entry.getKey().equalsIgnoreCase("javaserver.type")) {
				if (serverTypes.containsKey(properties.getProperty(entry.getKey()))) {
					properties.put("javaserver.installFile", serverTypes.get(properties.getProperty(entry.getKey())));
				}
			}
		}

		return false;
	}

	private static void createPropertiesSample() {
		System.out.println();
		System.out.println(" *** Creating a sample of lumisportalinstaller.properties ***");
		System.out.println();

		File lumisportalinstallerPropertiesSampleFile = new File("lumisportalinstaller.properties");
		String str;
		try {
			str = ResourceEnum.LUMISPORTALINSTALLER_PROPERTIES_SAMPLE.getText();
			Files.write(str, lumisportalinstallerPropertiesSampleFile, Charsets.UTF_8);
		} catch (IOException e) {
		}

		System.exit(1);
	}
}
