package infradev.lumis.lumisportalinstaller.javaserver;

import infradev.lumis.lumisportalinstaller.common.AbstractInstaller;
import infradev.lumis.lumisportalinstaller.common.PathEnum;
import infradev.lumis.lumisportalinstaller.common.inject.annotation.Logged;
import infradev.lumis.lumisportalinstaller.database.AbstractDatabase;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

/**
 * Abstract class to Java Servers.
 * 
 * @author Alexandre Ribeiro de Souza
 */
public abstract class AbstractJavaServer extends AbstractInstaller {

	@Inject
	@Named("${database.type:-MYSQL}")
	protected AbstractDatabase database;

	@Override
	protected void extract() throws IOException {
		unpackZipFile(javaserverInstallFile, PathEnum.JAVASERVER_DIR.getFile(), "", true);
	}

	@Override
	protected void configure() throws IOException {
		copyLumisportal_Files();

		if (deployAsWar) {
			createLumisportalWar();
			cleanLumisportal_Installation();
		}

		configureJavaServer();
		configureFile_Permission();
	}

	/**
	 * Copy Lumis Portal shared and embedded files to JavaServer.
	 * 
	 * @throws IOException
	 *             Throws IOException.
	 */
	protected abstract void copyLumisportal_Files() throws IOException;

	/**
	 * Configure the JavaServer.
	 * 
	 * @throws IOException
	 *             Throws IOException.
	 */
	protected abstract void configureJavaServer() throws IOException;

	/**
	 * Create lumisportal.war file.
	 * 
	 * @throws IOException
	 *             Throws IOException.
	 */
	@Logged
	protected void createLumisportalWar() throws IOException {
		File wwwDir = PathEnum.LUMISPORTAL_DIR_WWW.getFile();
		List<File> fileList = Files.fileTreeTraverser().preOrderTraversal(wwwDir).toList();

		try (ZipOutputStream zos = new ZipOutputStream(Files.asByteSink(PathEnum.LUMISPORTAL_FILE_WAR.getFile()).openBufferedStream())) {
			for (File file : fileList) {
				if (file.isFile()) {
					String fileName = getPath(file).replace(getPath(wwwDir) + "/", "");
					zos.putNextEntry(new ZipEntry(fileName));
					Files.asByteSource(file).copyTo(zos);
					zos.closeEntry();
				}
			}
		}
	}

	/**
	 * Clean the Lumis Portal installation.
	 * 
	 * @throws IOException
	 *             Throws IOException.
	 */
	@Logged
	protected void cleanLumisportal_Installation() throws IOException {
		List<String> mustExist = Lists.newArrayList("lumisdata", "lumisportal.war");

		for (File file : PathEnum.LUMISPORTAL_DIR.getFile().listFiles()) {
			if (!Iterables.contains(mustExist, file.getName())) {
				if (file.isDirectory()) {
					deleteDirectory(file);
				} else {
					file.delete();
				}
			}
		}
	}

	/**
	 * Configure execution files permission.
	 * 
	 * @throws IOException
	 *             Throws IOException.
	 */
	@Logged
	protected void configureFile_Permission() throws IOException {
		for (File file : PathEnum.JAVASERVER_BIN_DIR.getFile().listFiles()) {
			if (Files.getFileExtension(getPath(file)).equalsIgnoreCase("sh")) {
				file.setExecutable(true, false);
			}
		}
	}
}
