package infradev.lumis.lumisportalinstaller;

import java.io.IOException;
import java.util.Properties;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 *
 * @author Alexandre Ribeiro de Souza
 */
public class InstallerTest {

	public InstallerTest() {
	}

	@BeforeClass
	public static void setUpClass() {
	}

	@AfterClass
	public static void tearDownClass() {
	}

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}

	/**
	 * Test of install method, of class Installer.
	 *
	 * @throws java.io.IOException
	 *             Throws IOException.
	 */
	@org.junit.Test
	public void testInstall() throws IOException {
		System.out.println("install");
		Properties properties = new Properties();
		properties.setProperty("lumisportal.installFile", "/Users/alexandre/Downloads/install/lumisportal/lumisportal_7.1.1.140331.zip");
		properties.setProperty("lumisportal.extractDoc", "false");
		properties.setProperty("lumisportal.frameworkUrl", "http://localhost:8080");
		properties.setProperty("javaserver.type", "TOMCAT7");
		properties.setProperty("javaserver.installFile", "http://central.maven.org/maven2/org/apache/tomcat/tomcat/7.0.50/tomcat-7.0.50.zip");
		properties.setProperty("javaserver.javaopts", "-Xms1024m -Xmx1024m -XX:MaxPermSize=256m");
		properties.setProperty("javaserver.deployAsWar", "false");
		properties.setProperty("database.mysqldriverFile", "http://central.maven.org/maven2/mysql/mysql-connector-java/5.1.30/mysql-connector-java-5.1.30.jar");
		properties.setProperty("database.user", "lumis");
		properties.setProperty("database.password", "lumisEIP");
		properties.setProperty("database.url", "jdbc:mysql://localhost/lumisportal?characterEncoding=UTF-8&zeroDateTimeBehavior=convertToNull");
		// properties.setProperty("database.url", "jdbc:oracle:thin:@localhost:1521:orcl");
		// properties.setProperty("database.url", "jdbc:jtds:sqlserver://localhost:1433;databaseName=lumisportal;instanceName=LUMIS;useLOBs=false");

		Installer.install(properties);

		Assert.assertTrue("The test case is a prototype.", true);
	}

}
