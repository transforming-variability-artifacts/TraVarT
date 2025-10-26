package at.jku.cps.travart.core.benchmarking;

import java.time.Instant;

/**
 * Example for a benchmarking event. Should be emitted upon creating a new feature while transforming between models.
 * 
 * This event has no own fields.
 */
public class FeatureDeletedEvent extends AbstractBenchmarkEvent {

	public FeatureDeletedEvent(Instant time, String msg, int ctx) {
		super(time, msg, ctx);
	}

}
