package infradev.lumis.lumisportalinstaller.database;

import javax.inject.Named;

/**
 * Oracle database handler.
 * 
 * @author Alexandre Ribeiro de Souza
 */
@Named("ORACLE")
public class OracleImpl extends AbstractDatabase {

	public OracleImpl() {
		super();
		driverPattern = "ojdbc";
		commentPattern = "<!-- Oracle";
		classname = "oracle.jdbc.driver.OracleDriver";
		// baseUrl = "jdbc:oracle:thin:@localhost:1521:orcl";
	}
}
