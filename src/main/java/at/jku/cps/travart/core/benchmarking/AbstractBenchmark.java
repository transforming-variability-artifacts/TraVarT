package at.jku.cps.travart.core.benchmarking;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.auto.service.AutoService;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * Abstract benchmark implementation with sensible defaults for the methods
 * enforced by the interface.
 * 
 * All benchmarks must be annotated with {@link AutoService} to be detected
 * automatically before transformation.
 * 
 * @param <T>
 * @see IBenchmark
 */
public abstract class AbstractBenchmark<T> implements IBenchmark<T> {

	protected EventBus registeredBus;
	protected int bitMask; // Use to filter plugins?
	private boolean muted;

	static final Logger LOGGER = LogManager.getLogger();

	@Override
	public void activateBenchmark(EventBus bus) {
		bus.register(this);
		this.registeredBus = bus;
	}

	@Subscribe
	private void mute(MuteEvent event) {
		muted = true;
	}

	@Subscribe
	private void unmute(UnmuteEvent event) {
		muted = false;
	}

	protected void log(String msg) {
		if (!muted) {
			LOGGER.info(msg);
		}
	}

}