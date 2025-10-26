package at.jku.cps.travart.core.benchmarking;

import java.time.Instant;

/**
 * Event to be emitted when a transformation process has ended.
 * 
 * "finalSize" is the size of the resulting model (number of features/feature-equivalent elements).
 * "success" is set to true if the transformation ended through completion. If the
 * transformation had to be aborted, emit this event with `success` set to false or
 * emit TransformationAbortedEvent.
 */
public class TransformationEndEvent extends AbstractBenchmarkEvent {
	
	public final int finalSize;
	public final boolean success;
	public final boolean intermediate;

	public TransformationEndEvent(Instant time, String msg, int ctx, int size, boolean status, boolean intermediate) {
		super(time, msg, ctx);
		this.finalSize  = size;
		this.success = status;
		this.intermediate = intermediate;
	}

}
