package at.jku.cps.travart.core.benchmarking;

import java.time.Instant;

/**
 * Mute event for disabling logging for subscribed benchmarks. See
 * {@link AbstractBenchmark#log(String)} for the default implementation for
 * handling the mute flag in benchmarks.
 * 
 * @see UnmuteEvent
 */
public class MuteEvent extends AbstractBenchmarkEvent {

	public MuteEvent(Instant time, String msg, int ctx) {
		super(time, msg, ctx);
	}

}
