package at.jku.cps.travart.core.benchmarking;

import java.util.List;

import com.google.common.eventbus.EventBus;

/**
 * Interface for benchmarks. A benchmark should listen on an {@link EventBus}
 * and prepare some result, which can then be retrieved over
 * {@link this#getResults()}. The result type is generic.
 * 
 * Use {@link AbstractBenchmark} as a superclass for sensible defaults in your
 * custom benchmark implementation.
 * 
 * @param <T>
 *            Type of the benchmark result
 */
public interface IBenchmark<T> {

	void activateBenchmark(EventBus bus);

	// Use List interface to allow indexed access
	public List<T> getResults();

	default List<String> getResultsHeader() {
		return List.of(getId());
	}

	public String getId();

}
