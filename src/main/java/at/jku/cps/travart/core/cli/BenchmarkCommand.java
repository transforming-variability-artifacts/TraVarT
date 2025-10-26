package at.jku.cps.travart.core.cli;

import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import at.jku.cps.travart.core.benchmarking.IBenchmark;
import picocli.CommandLine.Command;

@Command(name = "benchmark", version = "0.0.1", description = "Lists all known benchmarks. Currently supports only built-in benchmarks.")
public class BenchmarkCommand implements Callable<Integer> {

	@Override
	public Integer call() throws Exception {
		ServiceLoader<IBenchmark> allBenchmarks = ServiceLoader
				.load(IBenchmark.class);
		List<String> benchmarkNames = allBenchmarks.stream()
				.map(e -> e.get().getId()).collect(Collectors.toList());
		System.out.println(
				"Benchmarks reported by ServiceLoader: " + benchmarkNames);
		return 0;
	}

}
