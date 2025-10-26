package at.jku.cps.travart.core.transformation;

import java.time.Instant;

import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.slf4j.event.Level;

import com.google.common.eventbus.EventBus;

import at.jku.cps.travart.core.FeatureModelStatistics;
import at.jku.cps.travart.core.benchmarking.IBenchmarkingEvent;
import at.jku.cps.travart.core.benchmarking.IEmitting;
import at.jku.cps.travart.core.benchmarking.MuteEvent;
import at.jku.cps.travart.core.benchmarking.TransformationBeginEvent;
import at.jku.cps.travart.core.benchmarking.TransformationEndEvent;
import at.jku.cps.travart.core.benchmarking.UnmuteEvent;
import at.jku.cps.travart.core.common.IModelTransformer;
import at.jku.cps.travart.core.common.IStatistics;
import at.jku.cps.travart.core.exception.NotSupportedVariabilityTypeException;
import de.vill.model.FeatureModel;

public abstract class AbstractBenchmarkingTransformer<T> implements IEmitting, IModelTransformer<T> {
	
	protected EventBus bus;
	protected Level verbosity;

	@Override
	final public FeatureModel transform(T model, String modelName, STRATEGY strategy, boolean intermediate)
			throws NotSupportedVariabilityTypeException {
		FeatureModel transformationResult;
		post(new TransformationBeginEvent(Instant.now(), modelName, model.hashCode(), getTargetStatistics().getVariabilityElementsCount(model)), Level.INFO);
		try {
			transformationResult = transformInner(model, modelName, strategy);
		} catch (Exception e) {
			post(new TransformationEndEvent(Instant.now(), modelName, model.hashCode(), 0, false, intermediate), Level.INFO);				
			throw e;
		}
		post(new TransformationEndEvent(Instant.now(), modelName, model.hashCode(), FeatureModelStatistics.getInstance().getVariabilityElementsCount(transformationResult), true, intermediate), Level.INFO);
		FeatureModelStatistics.getInstance().logModelStatistics(LogManager.getLogger(), transformationResult);
		return transformationResult;
	}

	@Override
	final public T transform(FeatureModel model, String modelName, STRATEGY strategy, boolean intermediate)
			throws NotSupportedVariabilityTypeException {
		T transformationResult;
		post(new TransformationBeginEvent(Instant.now(), modelName, model.hashCode(), FeatureModelStatistics.getInstance().getVariabilityElementsCount(model)), Level.INFO);
		try {
			transformationResult = transformInner(model, modelName, strategy);
		} catch (Exception e) {
			post(new TransformationEndEvent(Instant.now(), modelName, model.hashCode(), 0, false, intermediate), Level.INFO);
			throw e;
		}
		post(new TransformationEndEvent(Instant.now(), modelName, model.hashCode(), getTargetStatistics().getVariabilityElementsCount(transformationResult), true, intermediate), Level.INFO);
		getTargetStatistics().logModelStatistics(LogManager.getLogger(), transformationResult);
		return transformationResult;
	}
	
	public abstract T transformInner(FeatureModel model, String modelName, STRATEGY strategy)
			throws NotSupportedVariabilityTypeException;
	
	public abstract FeatureModel transformInner(T model, String modelName, STRATEGY strategy)
			throws NotSupportedVariabilityTypeException;

	@Override
	public void setBus(EventBus bus) {
		this.bus = bus;
	}

	@Override
	public EventBus getBus() {
		return bus;
	}

	@Override
	public void setVerbosity(Level level) {
		this.verbosity = level;
	}

	@Override
	public Level getVerbosity() {
		return verbosity;
	}
	
	@Override
	public void triggerUnmuteEvent() {
		if (Objects.nonNull(bus)) {
			bus.post(new UnmuteEvent(Instant.now(), "", this.hashCode()));
		}
	}
	
	@Override
	public void triggerMuteEvent() {
		if (Objects.nonNull(bus)) {
			bus.post(new MuteEvent(Instant.now(), "", this.hashCode()));
		}
	}
	
	// FIXME Somehow avoid code duplication, this needs to be implemented in the actual transformer logic as well?
	private void post(IBenchmarkingEvent<?> event, Level visibility) {
		if (Objects.nonNull(bus) && (this.verbosity.compareTo(visibility) >= 0)) {
			bus.post(event);
		}
	}
	
	public abstract IStatistics<T> getTargetStatistics();

}
