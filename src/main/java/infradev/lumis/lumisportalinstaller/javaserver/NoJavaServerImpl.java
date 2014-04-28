package infradev.lumis.lumisportalinstaller.javaserver;

import java.io.IOException;

import javax.inject.Named;

/**
 * No Java Server handler.
 * 
 * @author Alexandre Ribeiro de Souza
 */
@Named("NONE")
public class NoJavaServerImpl extends AbstractJavaServer {

	@Override
	protected void extract() throws IOException {
	}

	@Override
	protected void configure() throws IOException {
	}

	@Override
	protected void copyLumisportal_Files() throws IOException {
	}

	@Override
	protected void configureJavaServer() throws IOException {
	}
}
