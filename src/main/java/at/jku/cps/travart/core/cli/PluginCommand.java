/*******************************************************************************
 * This Source Code Form is subject to the terms of the Mozilla
 * Public License, v. 2.0. If a copy of the MPL was not distributed
 * with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 *
 * Contributors:
 *     @author Kevin Feichtinger
 *
 * Command line tool command to list all available plugins.
 *
 * Copyright 2023 Johannes Kepler University Linz
 * LIT Cyber-Physical Systems Lab
 * All rights reserved
 *******************************************************************************/
package at.jku.cps.travart.core.cli;

import java.util.concurrent.Callable;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import at.jku.cps.travart.core.common.IPlugin;
import at.jku.cps.travart.core.helpers.TraVarTPluginManager;
import picocli.CommandLine.Command;

@Command(name = "plugin", version = "0.0.1", description = "Lists the available plugins for the TraVarT command.")
public class PluginCommand implements Callable<Integer> {

	private static final Logger LOGGER = LogManager.getLogger(PluginCommand.class);

	@Override
	public Integer call() throws Exception {
		LOGGER.debug("Check for installed plugins...");
		TraVarTPluginManager.startPlugins();
		if (!TraVarTPluginManager.getAvailablePlugins().isEmpty()) {
			LOGGER.debug(String.format("Plugins found: %s", TraVarTPluginManager.getAvailablePlugins().size()));
			System.out.println(String.format("Installed plugins: %s", toPluginNameString()));
			// FIXME Avoid magic string, use AutoService to provide UVL transformer as if it were a plugin?
			System.out.println(
					"Languages provided by TraVarT...\n- at.jku.cps.travart.core[de.vill.uvl-parser] Universal Variability Language v1.0-SNAPSHOT");
			return 0;
		}
		LOGGER.error("No installed plugins could be found. Place them into the `plugins` folder next to the executable!");
		return -1;
	}

	private static String toPluginNameString() {
		StringBuilder builder = new StringBuilder();
		builder.append("\n");
		for (IPlugin plugin : TraVarTPluginManager.getAvailablePlugins().values()) {
			builder.append("- ").append(toPluginString(plugin)).append("\n");
		}
		return builder.toString();
	}

	private static String toPluginString(final IPlugin plugin) {
		StringBuilder builder = new StringBuilder();
		builder.append(plugin.getId());
		builder.append(", version ").append(plugin.getVersion());
		builder.append(", supports file type: ").append(plugin.getName());
		return builder.toString();
	}

}
