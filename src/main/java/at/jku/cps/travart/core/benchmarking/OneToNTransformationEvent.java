package at.jku.cps.travart.core.benchmarking;

import java.time.Instant;

/**
 * Example for a benchmarking event. Should be emitted when a transformation maps
 * one component to many; for example, in context of the Kconfig plugin, when OR groups
 * are matched to two respective config symbols.
 * 
 * This event has no own fields.
 */
public class OneToNTransformationEvent extends AbstractBenchmarkEvent {

	public OneToNTransformationEvent(Instant time, String msg, int ctx) {
		super(time, msg, ctx);
	}

}