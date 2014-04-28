package infradev.lumis.lumisportalinstaller.common.tools;

/**
 * A progress meter controller.
 *
 * @author Alexandre Ribeiro de Souza
 */
public class ProgressMeter {

	private long lastSecond;
	private int seconds;

	public int getSeconds() {
		return seconds;
	}

	public void setSeconds(int seconds) {
		this.seconds = seconds;
	}

	public ProgressMeter() {
		this.seconds = 10;
	}

	public void tick(String msg) {
		long now = System.currentTimeMillis();

		if (now - lastSecond >= seconds * 1000) {
			lastSecond = now;

			Log.info(msg);
		}
	}
}
