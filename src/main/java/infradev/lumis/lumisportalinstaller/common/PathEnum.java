package infradev.lumis.lumisportalinstaller.common;

import java.io.File;

/**
 * Paths enum.
 *
 * @author Alexandre Ribeiro de Souza
 */
public enum PathEnum {

	LUMISPORTAL_DIR("/lumisportal"),
	LUMISPORTAL_DIR_LUMISDATA("/lumisportal/lumisdata"),
	LUMISPORTAL_DIR_SETUP("/lumisportal/setup"),
	LUMISPORTAL_DIR_WWW("/lumisportal/www"),
	LUMISPORTAL_DIR_CONTEXTLIB("/lumisportal/www/WEB-INF/lib"),
	LUMISPORTAL_DIR_ENDORSEDLIB("/lumisportal/lib/endorsed"),
	LUMISPORTAL_DIR_SHAREDLIB("/lumisportal/lib/shared"),
	LUMISPORTAL_DIR_STATICLUMIS("/lumisportal/www/lumis"),
	LUMISPORTAL_FILE_LUMISHIBERNATECFGXML("/lumisportal/lumisdata/config/lumishibernate.cfg.xml"),
	LUMISPORTAL_FILE_LUMISPORTALCONFIGXML("/lumisportal/lumisdata/config/lumisportalconfig.xml"),
	LUMISPORTAL_FILE_WAR("/lumisportal/lumisportal.war"),
	LUMISPORTAL_FILE_WEBXML("/lumisportal/www/WEB-INF/web.xml"),
	LUMISPORTAL_FILE_JBOSSWEBXML("/lumisportal/www/WEB-INF/jboss-web.xml"),
	HTDOCS_DIR("/htdocs"),
	HTDOCS_DIR_LUMIS("/htdocs/lumis"),
	JAVASERVER_DIR("/JAVASERVER"),
	JAVASERVER_BIN_DIR("/JAVASERVER/bin"),
	TOMCAT_FILE_CONTEXTXML("/JAVASERVER/conf/Catalina/localhost/%s.xml"),
	TOMCAT_FILE_SERVERXML("/JAVASERVER/conf/server.xml"),
	TOMCAT_FILE_SETENVBAT("/JAVASERVER/bin/setenv.bat"),
	TOMCAT_FILE_SETENVSH("/JAVASERVER/bin/setenv.sh"),
	JBOSS_DIR_MODULES("/JAVASERVER/modules"),
	JBOSS_FILE_MYSQLDRIVERMODULEXML("/JAVASERVER/modules/lumis/jdbc/driver/mysql/main/module.xml"),
	JBOSS_FILE_STANDALONEWAR("/JAVASERVER/standalone/deployments/%s.war"),
	JBOSS_FILE_DOMAINWAR("/JAVASERVER/domain/data/content/%s/%s/content"),
	JBOSS_FILE_PROFILEXML("/JAVASERVER/%1$s/configuration/%1$s.xml"),
	JBOSS_FILE_DOMAINHOSTXML("/JAVASERVER/domain/configuration/host.xml"),
	JBOSS_FILE_PROFILECONF("/JAVASERVER/bin/%s.conf");

	private final String path;
	private final String homeDir;

	private static String javaserverType = "";

	private PathEnum(String path) {
		this.path = path;
		homeDir = System.getProperty("user.dir", ".") + "/lumis";
	}

	public String getPath() {
		return (homeDir + path.replace("JAVASERVER", javaserverType.toLowerCase())).replace('\\', '/');
	}

	public File getFile() {
		return new File(getPath());
	}

	public static void setJavaserverType(String value) {
		javaserverType = value;
	}
}
