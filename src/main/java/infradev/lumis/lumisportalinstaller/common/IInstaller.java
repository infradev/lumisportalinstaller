package infradev.lumis.lumisportalinstaller.common;

import java.io.IOException;

/**
 * Interface to all functionalities.
 *
 * @author Alexandre Ribeiro de Souza
 */
public interface IInstaller {

	/**
	 * Execute the installation.
	 *
	 * @throws IOException
	 *             Throws IOException.
	 */
	void install() throws IOException;
}
