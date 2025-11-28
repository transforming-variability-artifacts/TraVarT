package at.jku.cps.travart.core.common;

import at.jku.cps.travart.core.transformation.AbstractBenchmarkingTransformer;

public interface IBenchmarkingPlugin<T> extends IPlugin<T> {
	
	// FIXME Interface depends on abstract class
	AbstractBenchmarkingTransformer<T> getBenchmarkingTransformer();
	
	default IModelTransformer<T> getTransformer() {
		return getBenchmarkingTransformer();
	}

}
