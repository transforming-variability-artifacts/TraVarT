package at.jku.cps.travart.core.benchmarking;

import org.slf4j.event.Level;

import com.google.common.eventbus.EventBus;

/**
 * Interface for classes that emit onto {@link EventBus}es. While it is not
 * specified through type constraints, by contract, classes that implement
 * interface should use {@link IBenchmarkingEvent}.
 */
public interface IEmitting {

	/**
	 * Set given bus as the bus to emit for this event-emitting object.
	 * 
	 * @param bus
	 */
	void setBus(EventBus bus);

	/**
	 * Get currently set event bus.
	 * 
	 * @return
	 */
	EventBus getBus();

	/**
	 * Set verbosity of this event-emitting object. Uses levels specified by
	 * {@link org.slf4j.event.Level}, use {@link Level.intToLevel} if you prefer
	 * specifying log levels over ints.
	 * 
	 * @param level
	 */
	void setVerbosity(Level level);

	/**
	 * Get currently set level of verbosity.
	 * 
	 * @return
	 */
	Level getVerbosity();

	/**
	 * Trigger a mute event, emitting a {@link MuteEvent} onto the set
	 * {@link EventBus}.of this object.
	 */
	void triggerMuteEvent();

	/**
	 * Trigger an unmute event, emitting a {@link UnmuteEvent} onto the set
	 * {@link EventBus}.of this object.
	 */
	void triggerUnmuteEvent();

}
