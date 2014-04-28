package infradev.lumis.lumisportalinstaller.javaserver.jboss;

import javax.inject.Named;

/**
 * JBoss AS 7.1 Java Server handler.
 * 
 * @author Alexandre Ribeiro de Souza
 */
@Named("JBOSSAS71")
public class JBossAS71Impl extends AbstractJBoss {

	public JBossAS71Impl() {
		lumisportalmoduleDirPath = "lib/jboss71/modules";
	}
}
