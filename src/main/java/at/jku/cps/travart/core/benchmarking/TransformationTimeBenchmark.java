package at.jku.cps.travart.core.benchmarking;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.auto.service.AutoService;
import com.google.common.eventbus.Subscribe;

@AutoService(IBenchmark.class)
public class TransformationTimeBenchmark extends AbstractBenchmark<Long> {
	
	private Instant startedAt;
	private Instant end;
	
	@Subscribe
	private void startOfTransformation(TransformationBeginEvent event) {
		log("Just recieved: TransformationBeginEvent = " + event.getDetails() + ", size: " + event.initialSize);
		startedAt = event.getTimestamp();
	}
	
	@Subscribe
	private void endOfTransformation(TransformationEndEvent event) {
		if (event.intermediate) return;
		// FIXME React if currentSize != event.finalSize?
		end = event.getTimestamp();
		registeredBus.unregister(this);
	}

	@Override
	public List<Long> getResults() {
		return List.of(TimeUnit.MICROSECONDS.convert(Duration.between(startedAt, end).abs()));
	}

	@Override
	public String getId() {
		return "transformationTime";
	}

}
