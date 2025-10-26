package at.jku.cps.travart.core.benchmarking;

import java.time.Instant;

/**
 * Example for a benchmarking event. Should be emitted when a constraint is
 * added to the resulting model without a respective "source" constraint.
 * 
 * This event has no own fields.
 */
public class AdditionalConstraintEvent extends AbstractBenchmarkEvent {
	
	private int factor;

	public AdditionalConstraintEvent(Instant time, String msg, int ctx, int times) {
		super(time, msg, ctx);
		this.factor = times;
	}

	public int getFactor() {
		return factor;
	}

}