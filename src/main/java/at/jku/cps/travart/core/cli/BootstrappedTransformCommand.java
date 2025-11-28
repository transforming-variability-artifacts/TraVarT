package at.jku.cps.travart.core.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import at.jku.cps.travart.core.basic.UVL;
import at.jku.cps.travart.core.common.IDeserializer;
import at.jku.cps.travart.core.common.IModelTransformer;
import at.jku.cps.travart.core.common.ISerializer;
import at.jku.cps.travart.core.exception.NotSupportedVariabilityTypeException;
import at.jku.cps.travart.core.exception.TransformationException;
import at.jku.cps.travart.core.io.TraVarTFileUtils;
import de.vill.model.FeatureModel;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

// TODO Implement features from main Transform command also in BootstrappedTransformCommand
@Command(name = "transform", version = "0.0.1", 
	description = "Transforms the given variability artifacts into another type. "
		+ "When run over a plugin, target type is limited to the transformer provided by the plugin; "
		+ "i.e., from/to UVL from/to the format supported by the plugin.")
public class BootstrappedTransformCommand implements Callable<Integer> {
	private static final Logger LOGGER = LogManager.getLogger(TransformCommand.class);

	private static final String CORE_MODEL_UVL = "UVL";

	@SuppressWarnings("unused")
	private static String toStringList(final Iterable<String> fileExtensions) {
		StringBuilder builder = new StringBuilder();
		builder.append("{ ");
		for (String extension : fileExtensions) {
			builder.append(extension).append(",");
		}
		builder.deleteCharAt(builder.lastIndexOf(","));
		builder.append(" }");
		return builder.toString();
	}

	@Parameters(index = "0", description = "The source path to the variability artifact to transform. Folders are not supported.")
	private Path sourcePath;

	@Parameters(index = "1", description = "The output path to which the variability artifact is transformed.")
	private Path targetPath;
	
	@Option(names = {
			"--from-uvl"}, description = "If given, UVL will be set as the source type, i.e., the model will be transformed from UVL into the plugin's target type.")
	private boolean fromUvl = false;

	private IDeserializer bootstrappedDeserializer;
	private ISerializer bootstrappedSerializer;
	private final IModelTransformer bootstrappedTransformer;
	
	private final UVL uvlFacility = new UVL();

	public BootstrappedTransformCommand(IDeserializer bDeserializer, ISerializer bSerializer,
			IModelTransformer bTransformer) {
		this.bootstrappedDeserializer = bDeserializer;
		this.bootstrappedSerializer = bSerializer;
		this.bootstrappedTransformer = bTransformer;
	}

	@Override
	public Integer call() throws Exception {
		// Replace deserializer/serializer based on fromUvl
		if (fromUvl) {
			bootstrappedDeserializer = uvlFacility.getDeserializer();
		} else {
			bootstrappedSerializer = uvlFacility.getSerializer();
		}
		// Run transformations
		LOGGER.debug("Starting transformation of variability artifacts...");
		try {
			if (Files.isRegularFile(sourcePath)) {
				return transformSingleFile(sourcePath);
			}
			return transformDirectory();
		} catch (IOException | NotSupportedVariabilityTypeException ex) {
			LOGGER.error("Error while handling files...");
			LOGGER.error(ex.toString());
			throw new TransformationException(ex);
		}
	}
	
	private Integer transformDirectory() throws IOException, NotSupportedVariabilityTypeException {
		Set<Path> files = new TreeSet<>(Comparator.comparingLong((Path p) -> {
		try {
			return Files.size(p);
		} catch (IOException e) {
			return Long.MAX_VALUE;
		}}).thenComparing(Comparator.comparing(Path::toString)));
		int counter = 0;
		LOGGER.debug(String.format("Collect files of type %s...", toStringList(bootstrappedDeserializer.fileExtensions())));
		for (Object elem : bootstrappedDeserializer.fileExtensions()) {
			String extension = (String) elem;
			Set<Path> filesFound = TraVarTFileUtils.getPathSet(sourcePath, extension);
			files.addAll(filesFound);
			LOGGER.debug(String.format("%d files with extension %s found...", filesFound.size(), extension));
		}
		LOGGER.debug(String.format("%d files to transform...", files.size()));
		for (Path file : files) {
			int result = transformSingleFile(file);
			if (result != 0) {
				LOGGER.error(String.format("Error during transformation of file %s...", file.getFileName()));
				return result;
			}
		}
		return 0;
	}

	private Integer transformSingleFile(final Path file) throws IOException, NotSupportedVariabilityTypeException {
		Object deserializedModel = bootstrappedDeserializer.deserializeFromFile(sourcePath);
		
		Object transformedModel = null;
		if (fromUvl) {
			transformedModel = bootstrappedTransformer.transform((FeatureModel) deserializedModel);
		} else {
			transformedModel = bootstrappedTransformer.transform(deserializedModel);
		}
	
		bootstrappedSerializer.serializeToFile(transformedModel, sourcePath);
		
		return 0;
	}

}
