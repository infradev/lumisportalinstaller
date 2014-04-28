package infradev.lumis.lumisportalinstaller.common.inject.module;

import infradev.lumis.lumisportalinstaller.Installer;
import infradev.lumis.lumisportalinstaller.common.inject.annotation.Logged;
import infradev.lumis.lumisportalinstaller.common.inject.interceptor.LoggingMethodInterceptor;

import java.util.Properties;

import javax.inject.Named;

import org.eclipse.sisu.wire.ParameterKeys;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;

/**
 * Main Guice module of the project.
 *
 * @author Alexandre Ribeiro de Souza
 */
@Named
public class MainModule extends AbstractModule {

	@Override
	protected void configure() {
		Properties properties = Installer.properties;

		String defaultDatabaseUrl = "jdbc:mysql://localhost/lumisportal?characterEncoding=UTF-8&zeroDateTimeBehavior=convertToNull";
		String databaseType = properties.getProperty("database.url", defaultDatabaseUrl).split(":")[1].toUpperCase();
		if (databaseType.equals("JTDS")) {
			databaseType = "SQLSERVER";
		}

		properties.setProperty("database.type", databaseType);
		properties.setProperty("isWindows", String.valueOf(System.getProperty("os.name").toLowerCase().contains("win")));

		bind(ParameterKeys.PROPERTIES).toInstance(properties);
		bindInterceptor(Matchers.any(), Matchers.annotatedWith(Logged.class), new LoggingMethodInterceptor());
	}
}
