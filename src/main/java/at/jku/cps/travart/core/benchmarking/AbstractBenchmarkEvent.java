package at.jku.cps.travart.core.benchmarking;

import java.time.Instant;

public abstract class AbstractBenchmarkEvent implements IBenchmarkingEvent<String> {
	
	private final Instant emittedAt;
	private final String message;
	private final int context; // Additionally implement some global map of current transformation sessions
	
	/**
	 * Default super() constructor for benchmarking events.
	 * @param time Timestamp of the event being emitted, as java.time.Instant
	 * @param msg Message/details of event emission, used for logging
	 * @param ctx Context of the event, either a plugin or a session ID
	 */
	public AbstractBenchmarkEvent(Instant time, String msg, int ctx) {
		this.emittedAt = time;
		this.message = msg;
		this.context = ctx; // Implement as bit mask; match current plugin and/or session
	}

	@Override
	public String getDetails() {
		// FIXME Somehow include class name in this string
		// Cannot be implemented in abstract parent class, we need specific child class name!
		return "Emitted at " + emittedAt.toString() + ", session " + context + ":" + message;
	}
	
	// For default abstract event, event message = event details
	@Override
	public String getMessage() {
		return getDetails();
	}

	@Override
	public Instant getTimestamp() {
		return emittedAt;
	}

	@Override
	public int getContext() {
		return context;
	}

}
