/*******************************************************************************
 * This Source Code Form is subject to the terms of the Mozilla
 * Public License, v. 2.0. If a copy of the MPL was not distributed
 * with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 *
 * Contributors:
 *     @author Kevin Feichtinger
 *
 * Command line tool start point.
 *
 * Copyright 2023 Johannes Kepler University Linz
 * LIT Cyber-Physical Systems Lab
 * All rights reserved
 *******************************************************************************/
package at.jku.cps.travart.core.cli;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import at.jku.cps.travart.core.helpers.TraVarTPluginManager;
import picocli.CommandLine; // Re-implement CLI with Commons CLI framework?
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

@Command(name = "travart", subcommands = {TransformCommand.class,
		PluginCommand.class,
		BenchmarkCommand.class}, mixinStandardHelpOptions = true, version = "0.0.1", description = "TraVarT main command to transform and validate variability artifacts.")
public class TraVarTCommand {

	static {
		TraVarTPluginManager.startPlugins();
	}

	@Option(names = {"-v",
			"--verbose"}, scope = ScopeType.INHERIT, description = "Enable verbose log information during execution.")
	public void setVerbose(final boolean[] verbose) {
		Configurator
				.setRootLevel(verbose.length > 0 ? Level.DEBUG : Level.INFO);
	}

	public static void main(final String[] args) {
		String[] arg = {"-h"};
		if (args.length != 0) {
			arg = args;
		}
		int exitCode = new CommandLine(new TraVarTCommand()).execute(arg);
		TraVarTPluginManager.stopPlugins();
		System.exit(exitCode);
	}
}
