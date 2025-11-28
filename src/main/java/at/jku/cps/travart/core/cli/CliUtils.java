package at.jku.cps.travart.core.cli;

import at.jku.cps.travart.core.common.IDeserializer;
import at.jku.cps.travart.core.common.IModelTransformer;
import at.jku.cps.travart.core.common.ISerializer;
import picocli.CommandLine;

public class CliUtils {

	/**
	 * Provides a generic CLI for plugins. The respective plugin utilizing this
	 * static method should provide the transformer to be bootstrapped.
	 * 
	 * @param args Pass through the parameters received by the plugin CLI
	 * @param bootstrappedTransformer Transformer provided by the calling plugin
	 * @param bootstrappedSerializer Serializer provided by the calling plugin
	 * @param bootstrappedDeserializer Deserializer provided by the calling plugin
	 * @return
	 */
	public static int cliPluginDelegate(String[] args, IModelTransformer bootstrappedTransformer,
			ISerializer bootstrappedSerializer, IDeserializer bootstrappedDeserializer) {
		int exitCode = new CommandLine(new BootstrappedTransformCommand(bootstrappedDeserializer, bootstrappedSerializer, bootstrappedTransformer)).execute(args);
		return exitCode;
	}
}
