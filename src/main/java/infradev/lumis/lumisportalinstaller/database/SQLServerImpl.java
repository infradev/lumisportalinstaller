package infradev.lumis.lumisportalinstaller.database;

import javax.inject.Named;

/**
 * SQL Server database handler.
 * 
 * @author Alexandre Ribeiro de Souza
 */
@Named("SQLSERVER")
public class SQLServerImpl extends AbstractDatabase {

	public SQLServerImpl() {
		super();
		driverPattern = "jtds";
		commentPattern = "<!-- MS SQL Server";
		classname = "net.sourceforge.jtds.jdbc.Driver";
		// baseUrl = "jdbc:jtds:sqlserver://localhost:1433;databaseName=lumisportal;instanceName=LUMIS;useLOBs=false";
	}
}
