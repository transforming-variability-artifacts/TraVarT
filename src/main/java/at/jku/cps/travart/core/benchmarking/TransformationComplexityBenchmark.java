package at.jku.cps.travart.core.benchmarking;

import java.util.List;

import com.google.auto.service.AutoService;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import at.jku.cps.travart.core.cli.TransformCommand;

/**
 * Benchmark that counts {@link AdditionalConstraintEvent}s and
 * {@link OneToNTransformationEvent}s emitted onto the subscribed bus.
 * 
 * Should be extended with any further events that model transformation
 * complexity. The {@link com.google.auto.service.AutoService} annotation is
 * required for the automatic detection of benchmarks by the CLI, i.e.,
 * {@link TransformCommand}.
 * 
 * @see OneToNTransformationEvent
 */
@AutoService(IBenchmark.class)
public class TransformationComplexityBenchmark
		extends
			AbstractBenchmark<Integer> {

	private int hits;

	@Override
	public String getId() {
		return "complexity";
	}

	@Subscribe
	private void count(OneToNTransformationEvent event) {
		hits++;
	}

	@Subscribe
	private void count(AdditionalConstraintEvent event) {
		hits += event.getFactor();
	}

	@Override
	public void activateBenchmark(EventBus bus) {
		bus.register(this);
		this.registeredBus = bus;
	}

	@Override
	public List<Integer> getResults() {
		return List.of(hits);
	}

}
