package infradev.lumis.lumisportalinstaller.common.tools;

import java.io.IOException;
import java.io.OutputStream;

import javax.inject.Inject;

import com.google.common.io.CountingOutputStream;

/**
 * A download counting output stream.
 * 
 * @author Alexandre Ribeiro de Souza
 */
public class DownloadCountingOutputStream extends OutputStream {

	@Inject
	private ProgressMeter progressMeter;

	private int total;
	private CountingOutputStream outputStream;

	public DownloadCountingOutputStream create(OutputStream out, int total) {
		this.total = total;
		this.outputStream = new CountingOutputStream(out);

		return this;
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		outputStream.write(b, off, len);
		afterWrite();
	}

	@Override
	public void write(int b) throws IOException {
		outputStream.write(b);
		afterWrite();
	}

	private void afterWrite() {
		long count = outputStream.getCount();
		String msg = String.format("  |- %d (%,.2f%%) of %d bytes downloaded", count, ((count * 1.00) / total) * 100, total);
		progressMeter.tick(msg);
	}

	@Override
	public void close() throws IOException {
		super.close();
		outputStream.close();
	}

	@Override
	public void flush() throws IOException {
		super.flush();
		outputStream.flush();
	}
}
