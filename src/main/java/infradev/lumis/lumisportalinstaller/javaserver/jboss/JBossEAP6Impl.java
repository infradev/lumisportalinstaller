package infradev.lumis.lumisportalinstaller.javaserver.jboss;

import javax.inject.Named;

/**
 * JBoss EAP 6.x Java Server handler.
 * 
 * @author Alexandre Ribeiro de Souza
 */
@Named("JBOSSEAP6")
public class JBossEAP6Impl extends JBossAS71Impl {

	public JBossEAP6Impl() {
		originalJavaOpts = "-Xms1303m -Xmx1303m -XX:MaxPermSize=256m";
	}
}
