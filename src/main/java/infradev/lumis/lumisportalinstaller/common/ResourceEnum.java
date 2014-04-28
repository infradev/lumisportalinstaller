package infradev.lumis.lumisportalinstaller.common;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

/**
 * Resources enum.
 * 
 * @author Alexandre Ribeiro de Souza
 */
public enum ResourceEnum {

	LUMISPORTALINSTALLER_PROPERTIES_SAMPLE("lumis/lumisportal/installer/lumisportalinstaller.properties"),
	LUMISPORTAL_LUMISHIBERNATECFGXML("lumis/lumisportal/installer/lumisportal/lumishibernate.cfg.xml.datasource"),
	TOMCAT_PORTALXML("lumis/lumisportal/installer/javaserver/tomcat/portal.xml"),
	TOMCAT_SETENVBAT("lumis/lumisportal/installer/javaserver/tomcat/setenv.bat"),
	TOMCAT_SETENVSH("lumis/lumisportal/installer/javaserver/tomcat/setenv.sh"),
	JBOSS5_LUMISPORTALDSXML("lumis/lumisportal/installer/javaserver/tomcat/jboss5/lumisportal-ds.xml"),
	JBOSS_JBOSSDOMAINEE_PART("lumis/lumisportal/installer/javaserver/jboss/jboss-domain-ee.config"),
	JBOSS_JBOSSDOMAINSECURITY_PART("lumis/lumisportal/installer/javaserver/jboss/jboss-domain-security.config"),
	JBOSS_SYSTEMPROPERTIES_PART("lumis/lumisportal/installer/javaserver/jboss/system-properties.config"),
	JBOSS_DATASOURCE_PART("lumis/lumisportal/installer/javaserver/jboss/datasource.config"),
	JBOSS_DRIVER_PART("lumis/lumisportal/installer/javaserver/jboss/driver.config"),
	JBOSS_DEPLOYMENT_PART("lumis/lumisportal/installer/javaserver/jboss/deployment.config"),
	JBOSS_SERVERGROUPS_PART("lumis/lumisportal/installer/javaserver/jboss/server-groups.config"),
	JBOSS_SERVERS_PART("lumis/lumisportal/installer/javaserver/jboss/servers.config");

	private final String path;

	private ResourceEnum(String path) {
		this.path = path;
	}

	public URL getResource() {
		return Resources.getResource(path);
	}

	public String getText() throws IOException {
		return Resources.toString(getResource(), Charsets.UTF_8);
	}

	public List<String> getList() throws IOException {
		return Resources.readLines(getResource(), Charsets.UTF_8);
	}
}
