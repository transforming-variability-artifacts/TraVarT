package at.jku.cps.travart.core.benchmarking;

import java.util.List;

import com.google.auto.service.AutoService;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * Benchmark for source and target model size. {@link this#getResults()}
 * delivers a list of the initial size of this model (before transformation),
 * the number of counted {@link NewFeatureEvent}s since the benchmark was
 * activated, and the final size of the model (size of transformation result).
 */
@AutoService(IBenchmark.class)
public class ModelSizeBenchmark extends AbstractBenchmark<Integer> {

	private int initialSize;
	private int finalSize;
	private int diff;

	@Override
	public String getId() {
		return "modelSize"; // Used to identify the benchmark while invoking
							// over CLI
	}

	@Subscribe
	private void count(NewFeatureEvent event) {
		diff++;
	}

	@Subscribe
	private void count(FeatureDeletedEvent event) {
		diff--;
	}

	@Subscribe
	private void initialSize(TransformationBeginEvent event) {
		log("Just recieved: TransformationBeginEvent = " + event.getDetails()
				+ ", size: " + event.initialSize);
		this.initialSize = event.initialSize;
	}

	@Subscribe
	private void endOfTransformation(TransformationEndEvent event) {
		log("Just recieved: TransformationEndEvent = " + event.getDetails()
				+ ", size: " + event.finalSize);
		if (event.intermediate)
			return;
		// The name diff is a bit confusing, but the initial "blank" model has
		// size zero
		if (diff != event.finalSize) {
			LOGGER.warn(
					"Feature count doesn't match with count reported by TransformationEndEvent: %d != %d",
					diff, event.finalSize);
			LOGGER.warn(
					"This is expected if the used plugin does not emit NewFeatureEvents.");
		}
		this.finalSize = event.finalSize;
		registeredBus.unregister(this);
	}

	@Override
	public void activateBenchmark(EventBus bus) {
		bus.register(this);
		this.registeredBus = bus;
	}

	@Override
	public List<Integer> getResults() {
		return List.of(initialSize, diff, finalSize);
	}

	@Override
	public List<String> getResultsHeader() {
		return List.of("initialSize", "expectedDiff", "finalSize");
	}

}
