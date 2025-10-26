package at.jku.cps.travart.core.benchmarking;

import java.time.Instant;

/**
 * Unmute event for re-enabling logging for subscribed benchmarks that were
 * previously muted with a {@link MuteEvent}.
 */
public class UnmuteEvent extends AbstractBenchmarkEvent {

	public UnmuteEvent(Instant time, String msg, int ctx) {
		super(time, msg, ctx);
	}

}
