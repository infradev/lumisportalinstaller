package infradev.lumis.lumisportalinstaller.common;

import infradev.lumis.lumisportalinstaller.common.inject.annotation.Logged;
import infradev.lumis.lumisportalinstaller.common.tools.DownloadCountingOutputStream;
import infradev.lumis.lumisportalinstaller.common.tools.Log;
import infradev.lumis.lumisportalinstaller.common.tools.ProgressMeter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.inject.Inject;
import javax.inject.Named;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.base.Ascii;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.inject.Injector;

/**
 * Main abstract class of the installer.
 *
 * @author Alexandre Ribeiro de Souza
 */
public abstract class AbstractInstaller implements IInstaller {

	@Inject
	protected Injector injector;

	@Inject
	@Named("${datasourceName:-java:/comp/env/jdbc/portal}")
	protected String datasourceName;
	@Inject
	@Named("${isWindows:-false}")
	protected Boolean isWindows;

	@Inject
	@Named("${lumisportal.extractDoc:-false}")
	protected Boolean extractDoc;
	@Inject
	@Named("${lumisportal.installFile:-./lumisportal_7.1.1.140331.zip}")
	protected String lumisportalInstallFile;
	@Inject
	@Named("${lumisportal.frameworkUrl:-http://localhost:8080}")
	protected URL frameworkUrl;

	@Inject
	@Named("${database.type:-MYSQL}")
	protected String databaseType;
	@Inject
	@Named("${database.mysqldriverFile:-http://central.maven.org/maven2/mysql/mysql-connector-java/5.1.30/mysql-connector-java-5.1.30.jar}")
	protected String mysqldriverFile;
	@Inject
	@Named("${database.user:-lumis}")
	protected String databaseUser;
	@Inject
	@Named("${database.password:-lumisEIP}")
	protected String databasePassword;
	@Inject
	@Named("${database.url:-jdbc:mysql://localhost/lumisportal?characterEncoding=UTF-8&zeroDateTimeBehavior=convertToNull}")
	protected String databaseUrl;

	@Inject
	@Named("${javaserver.type:-TOMCAT7}")
	protected String javaserverType;
	@Inject
	@Named("${javaserver.installFile:-http://central.maven.org/maven2/org/apache/tomcat/tomcat/7.0.50/tomcat-7.0.50.zip}")
	protected String javaserverInstallFile;
	@Inject
	@Named("${javaserver.javaopts:--Xms1024m -Xmx1024m -XX:MaxPermSize=256m}")
	protected String javaOpts;
	@Inject
	@Named("${javaserver.deployAsWar:-false}")
	protected Boolean deployAsWar;

	protected String getPath(File file) throws IOException {
		return getFormatedPath(file.getCanonicalPath());
	}

	protected String getFormatedPath(String filePath) throws IOException {
		return filePath.replace('\\', '/');
	}

	protected String getContextPath() {
		String contextPath = frameworkUrl.getPath();
		if (!contextPath.isEmpty()) {
			contextPath = contextPath.substring(1);
		}

		return contextPath;
	}

	protected String getContextName() {
		String contextName = getContextPath();

		return contextName.isEmpty() ? "ROOT" : contextName;
	}

	@Override
	@Logged
	public void install() throws IOException {
		extract();
		configure();
	}

	/**
	 * Extract the installation file.
	 *
	 * @throws IOException
	 *             Throws IOException.
	 */
	protected abstract void extract() throws IOException;

	/**
	 * Configure the installation.
	 *
	 * @throws IOException
	 *             Throws IOException.
	 */
	protected abstract void configure() throws IOException;

	/**
	 * Executes a command.
	 *
	 * @param command
	 *            Command to be executed on homeDir.
	 * @throws IOException
	 *             Throws IOException.
	 */
	public void executeCommand(String command) throws IOException {
		executeCommand(command, new File(System.getProperty("user.dir")));
	}

	/**
	 * Executes a command.
	 *
	 * @param command
	 *            Command to be executed.
	 * @param dir
	 *            Directory where the command will be executed.
	 * @throws IOException
	 *             Throws IOException.
	 */
	public void executeCommand(String command, File dir) throws IOException {
		ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
		processBuilder.directory(dir);
		Process process = processBuilder.start();

		try {
			loadStream(process.getInputStream());
			loadStream(process.getErrorStream());
			process.waitFor();
		} catch (Exception e) {
			Log.error(e.getLocalizedMessage());
		}
	}

	private void loadStream(final InputStream stream) throws Exception {
		new Thread(new Runnable() {

			@Override
			public void run() {
				Scanner sc = new Scanner(stream);
				while (sc.hasNextLine()) {
					System.out.println(sc.nextLine());
				}
				sc.close();
			}
		}).start();
	}

	/**
	 * Extracts a zip archive path to a target directory.
	 *
	 * @param archivePath
	 *            The path to zip file.
	 * @param targetDirectory
	 *            The directory where the file will be decompressed.
	 * @param ignoreMatch
	 *            Regex of ignore pattern.
	 * @param ignoreRootDir
	 *            If will ignore Root directory.
	 * @throws IOException
	 *             Throws IOException.
	 */
	public void unpackZipFile(String archivePath, File targetDirectory, String ignoreMatch, boolean ignoreRootDir) throws IOException {
		File compressedFile = (archivePath.startsWith("http") || archivePath.startsWith("ftp")) ? downloadFile(new URL(archivePath)) : new File(archivePath);
		unpackZipFile(compressedFile, targetDirectory, ignoreMatch, ignoreRootDir);
	}

	/**
	 * Extracts a zip archive to a target directory.
	 *
	 * @param archive
	 *            The zip file.
	 * @param targetDirectory
	 *            The directory where the file will be decompressed.
	 * @param ignoreMatch
	 *            Regex of ignore pattern.
	 * @param ignoreRootDir
	 *            If will ignore Root directory.
	 * @return The number of files unpacked.
	 * @throws IOException
	 *             Throws IOException.
	 */
	@Logged
	public int unpackZipFile(File archive, File targetDirectory, String ignoreMatch, boolean ignoreRootDir) throws IOException {
		ProgressMeter progressMeter = injector.getInstance(ProgressMeter.class);
		int totalFiles = 0;

		try (ZipFile zipFile = new ZipFile(archive)) {
			for (final Enumeration<? extends ZipEntry> entries = zipFile.entries(); entries.hasMoreElements();) {
				final ZipEntry entry = entries.nextElement();
				String name = entry.getName();
				totalFiles++;

				if (name.matches(ignoreMatch)) {
					continue;
				}
				if (ignoreRootDir) {
					name = name.substring(name.indexOf("/") + 1);
				}

				File path = new File(targetDirectory, name);

				if (entry.isDirectory()) {
					path.mkdirs();
				} else {
					Files.createParentDirs(path);
					Files.asByteSink(path).writeFrom(zipFile.getInputStream(entry));
				}

				String msg = String.format("  |- %d files extracted", totalFiles);
				progressMeter.tick(msg);
			}
		}

		return totalFiles;
	}

	/**
	 * Download a file from a given url.
	 *
	 * @param url
	 *            The url from where the file will be downloaded.
	 * @return The downloaded file.
	 * @throws IOException
	 *             Throws IOException.
	 */
	@Logged
	public File downloadFile(URL url) throws IOException {
		if (url == null) {
			return null;
		}

		String urlStr = url.toString();
		String fileNameNoExt = Files.getNameWithoutExtension(urlStr);
		String fileExt = Files.getFileExtension(urlStr).split("\\?")[0];
		if (fileExt.length() > 0) {
			fileExt = "." + fileExt;
		}
		String fileName = fileNameNoExt + fileExt;

		URLConnection connection = url.openConnection();
		int length = connection.getContentLength();
		String raw = connection.getHeaderField("Content-Disposition");
		if (raw != null && raw.contains("=")) {
			fileName = raw.split("=")[1];
		}

		File destinationFile = new File(new File(System.getProperty("java.io.tmpdir"), "lpi"), fileName);
		if (!destinationFile.exists()) {
			downloadToFile(url, destinationFile, length);
		}

		return destinationFile;
	}

	/**
	 * Download a file and return the downloaded file.
	 *
	 * @param url
	 *            URL from where the file will be downloaded.
	 * @param destinationFile
	 *            File destination of the download.
	 * @param length
	 *            Length of the file.
	 * @throws IOException
	 *             Throws IOException.
	 */
	@Logged
	protected void downloadToFile(URL url, File destinationFile, int length) throws IOException {
		File tempDestinationFile = new File(destinationFile.getAbsolutePath() + ".tmp");
		Files.createParentDirs(destinationFile);
		Files.createParentDirs(tempDestinationFile);

		OutputStream os = Files.asByteSink(tempDestinationFile).openBufferedStream();

		try (InputStream is = url.openStream(); OutputStream out = injector.getInstance(DownloadCountingOutputStream.class).create(os, length)) {
			ByteStreams.copy(is, out);
		}

		Files.move(tempDestinationFile, destinationFile);
	}

	/**
	 * Save a list in a file.
	 *
	 * @param lines
	 *            list to be saved.
	 * @param file
	 *            file where the list will be saved.
	 * @throws IOException
	 *             Throws IOException.
	 */
	protected void saveToFile(List<String> lines, File file) throws IOException {
		saveToFile(Joiner.on((char) Ascii.LF).join(lines), file);
	}

	/**
	 * Save a String in a file.
	 *
	 * @param str
	 *            String to be saved.
	 * @param file
	 *            file where the String will be saved.
	 * @throws IOException
	 *             Throws IOException.
	 */
	protected void saveToFile(String str, File file) throws IOException {
		if (file.exists()) {
			Files.move(file, new File(file.getAbsolutePath() + ".original"));
		} else if (!file.getParentFile().exists()) {
			Files.createParentDirs(file);
		}

		Files.write(str, file, Charsets.UTF_8);
	}

	/**
	 * Apply a model on a string.
	 *
	 * @param template
	 *            string where the model will be applied.
	 * @param scope
	 *            scope that will be applied.
	 * @return Result string of the template.
	 * @throws IOException
	 *             Throws IOException.
	 */
	protected String applyTemplateModel(String template, Map<String, Object> scope) throws IOException {
		Writer writer = new StringWriter();
		MustacheFactory mf = new DefaultMustacheFactory();
		Mustache mustache = mf.compile(new StringReader(template), "");
		mustache.execute(writer, scope).flush();

		return writer.toString();
	}

	/**
	 * Delete a directory recursively.
	 *
	 * @param dir
	 *            directory to be deleted.
	 */
	protected void deleteDirectory(File dir) {
		if (!dir.exists() || !dir.isDirectory()) {
			return;
		}

		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				deleteDirectory(file);
			}
			file.delete();
		}

		dir.delete();
	}

	/**
	 * Copy a directory content to another diretory.
	 *
	 * @param from
	 *            directory that will have it's content copied.
	 * @param to
	 *            directory where the files will be copied.
	 * @throws IOException
	 *             Throws IOException.
	 */
	protected void copyDir(File from, File to) throws IOException {
		if (!from.exists()) {
			return;
		}

		for (File file : from.listFiles()) {
			File newFile = new File(to, file.getName());

			if (file.isDirectory()) {
				copyDir(file, newFile);
			} else {
				if (!newFile.getParentFile().exists()) {
					Files.createParentDirs(newFile);
				}
				Files.copy(file, newFile);
			}
		}
	}

	/**
	 * Remove lines from the informed index till the match.
	 *
	 * @param lines
	 *            Lines to be processed.
	 * @param index
	 *            Start index.
	 * @param match
	 *            Matcher to run.
	 */
	protected void removeIndexUntilMatch(List<String> lines, int index, String match) {
		String line;

		do {
			line = lines.get(index);
			lines.remove(index);
		} while (!line.contains(match));
	}
}
