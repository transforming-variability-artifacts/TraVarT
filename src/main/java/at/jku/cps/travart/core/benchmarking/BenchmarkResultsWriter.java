package at.jku.cps.travart.core.benchmarking;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ObjectArrays;

/***
 * Wrapper object around Apache Commons' {@link CSVPrinter} for writing
 * benchmarking results into a CSV file for further processing/analysis with
 * tools like R or Gnuplot.
 * 
 * Instances of this class are immutable once they are initialized. Please use a
 * new writer object for each transformation session.
 * 
 * @see CSVFormat
 */
public class BenchmarkResultsWriter {

	private static final Logger LOGGER = LogManager
			.getLogger(BenchmarkResultsWriter.class);

	private final String[] defaultHeaders = {"fileName", "targetType", "result",
			"deserializationTime"};
	/*
	 * Need additional member for format as it cannot be retrieved from the
	 * printer once the printer is instantiated
	 */
	private final CSVFormat benchmarkResultsFormat;
	private final CSVPrinter csvPrinter;
	private final Path resultsFile;

	public Path getResultsFile() {
		return resultsFile;
	}

	public BenchmarkResultsWriter(List<IBenchmark> benchmarks, Path targetFile)
			throws IOException {
		resultsFile = targetFile;
		boolean exists = targetFile.toFile().exists();
		if (exists) {
			LOGGER.debug(
					"Benchmark writeback file already exists, the transformer will skip already benchmarked files.");
		}
		String[] benchmarkColumns = (String[]) benchmarks.stream()
				.<String>flatMap(e -> e.getResultsHeader().stream())
				.toArray(String[]::new);
		var benchmarkResultsFormatBuilder = CSVFormat.DEFAULT.builder()
				.setHeader(ObjectArrays.concat(defaultHeaders, benchmarkColumns,
						String.class));
		benchmarkResultsFormat = exists
				? benchmarkResultsFormatBuilder.setSkipHeaderRecord(true).get()
				: benchmarkResultsFormatBuilder.get();
		csvPrinter = new CSVPrinter(new FileWriter(targetFile.toString(), true),
				benchmarkResultsFormat);
	}

	public void writeResults(Map<String, Object> dict) throws IOException {
		List<Object> sortedResults = new LinkedList<>();

		for (String column : benchmarkResultsFormat.getHeader()) {
			sortedResults.add(dict.get(column));
		}

		sortedResults.removeAll(Collections.singleton(null));

		csvPrinter.printRecord(sortedResults);
	}

	public void dispose() throws IOException {
		csvPrinter.close();
	}

	public List<String> getRecordTemplate() {
		return Arrays.asList(benchmarkResultsFormat.getHeader());
	}

	// Users should check before inserting duplicates with this method
	public boolean entryAlreadyExists(String column, String key,
			Path targetFile) {
		try (FileReader r = new FileReader(targetFile.toString())) {
			List<CSVRecord> records = benchmarkResultsFormat.parse(r)
					.getRecords();
			LOGGER.debug("Benchmark writeback file already has "
					+ records.size() + " entries!");
			for (CSVRecord rec : records) {
				if (key.equals(rec.get(column))) {
					return true;
				}
			}
		} catch (IOException e) {
			return false;
		}
		return false;
	}
}
