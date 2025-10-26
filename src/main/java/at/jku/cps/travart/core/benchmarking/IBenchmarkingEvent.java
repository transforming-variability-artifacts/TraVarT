package at.jku.cps.travart.core.benchmarking;

import java.time.Instant;

/**
 * Interface for benchmarking events. Such events can be emitted by
 * transformers, the content of the event is generic (T), but each event must
 * have an {@link Instant} timestamp, a message (this can be the message content
 * itself, if T is String), and some integer context identifier.
 * 
 * For sensible defaults for timestamps and event messages, directly extend the
 * AbstractBenchmarkEvent class instead of using this interface.
 * 
 * @param <T>
 *            Type of the event content
 */
public interface IBenchmarkingEvent<T> {

	/**
	 * Return short description of this event. Might not be personalized.
	 * 
	 * @return
	 */
	String getMessage();

	/**
	 * Return content/details of this event. Returns generic type T.
	 * 
	 * @return
	 */
	T getDetails();

	/**
	 * Return timestamp of event. Should correspond to the {@link Instant} at
	 * which this event was emitted.
	 * 
	 * @return
	 */
	Instant getTimestamp();

	/**
	 * Returns integer context identifier of this event. Can be, for example, a
	 * session ID or the hash code of the emitting plugin/transformer.
	 * 
	 * @return
	 */
	int getContext();

}
