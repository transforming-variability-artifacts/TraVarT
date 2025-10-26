package at.jku.cps.travart.core.benchmarking;

import java.time.Instant;

/**
 * Example for a benchmarking event. Should be emitted upon creating a new feature while transforming between models.
 * 
 * This event has a custom "initialSize" field.
 */
public class TransformationBeginEvent extends AbstractBenchmarkEvent {
	
	public final int initialSize;

	public TransformationBeginEvent(Instant time, String msg, int ctx, int size) {
		super(time, msg, ctx);
		initialSize = size;
	}

}
