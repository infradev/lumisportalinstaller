package infradev.lumis.lumisportalinstaller.database;

import javax.inject.Named;

/**
 * MySQL database handler.
 * 
 * @author Alexandre Ribeiro de Souza
 */
@Named("MYSQL")
public class MySQLImpl extends AbstractDatabase {

	public MySQLImpl() {
		super();
		driverPattern = "mysql";
		commentPattern = "<!-- MySQL";
		classname = "com.mysql.jdbc.Driver";
		// baseUrl = "jdbc:mysql://localhost/lumisportal?characterEncoding=UTF-8&amp;zeroDateTimeBehavior=convertToNull";
	}
}
