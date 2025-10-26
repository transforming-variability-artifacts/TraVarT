/*******************************************************************************
 * This Source Code Form is subject to the terms of the Mozilla
 * Public License, v. 2.0. If a copy of the MPL was not distributed
 * with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 *
 * Contributors:
 *     @author Prankur Agarwal
 *     @author Kevin Feichtinger
 *
 * Implements a base helper to load and manage available TraVarT plugins.
 *
 * Copyright 2023 Johannes Kepler University Linz
 * LIT Cyber-Physical Systems Lab
 * All rights reserved
 *******************************************************************************/
package at.jku.cps.travart.core.helpers;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.pf4j.DefaultPluginManager;
import org.pf4j.ManifestPluginDescriptorFinder;
import org.pf4j.PluginDescriptorFinder;
import org.pf4j.PluginManager;
import org.pf4j.RuntimeMode;

import at.jku.cps.travart.core.cli.PluginCommand;
import at.jku.cps.travart.core.common.IBenchmarkingPlugin;
import at.jku.cps.travart.core.common.IPlugin;

/**
 * This is the helper class to load, start, use, and close the available plugins
 * in the system.
 *
 * @author Prankur Agarwal
 * @author Kevin Feichtinger
 */
public final class TraVarTPluginManager {
	private static final Map<String, IPlugin> availablePlugins = new HashMap<>();
	
	private static final Logger LOGGER = LogManager.getLogger(TraVarTPluginManager.class);

	private static PluginManager plugman;

	private TraVarTPluginManager() {

	}

	/**
	 * A static function to start the available plugins in the system.
	 */
	public static void startPlugins() {
		// create the plugin manager
		LOGGER.debug("Starting plugin manager...");
		plugman = new DefaultPluginManager() {
			@Override
			protected PluginDescriptorFinder createPluginDescriptorFinder() {
				return new ManifestPluginDescriptorFinder();
			}
		};
		
		List<Path> pluginDirectories = plugman.getPluginsRoots().stream().map(e -> e.toAbsolutePath()).collect(Collectors.toList());
		LOGGER.debug("Will check these paths: " + pluginDirectories);
		// load the plugins
		plugman.loadPlugins();

		// start the plugins
		plugman.startPlugins();

		// find plugins
		findAvailablePlugins();
	}

	/**
	 * A static function to find the available plugins in the system.
	 */
	public static void findAvailablePlugins() {
		// Retrieve extensions for IPlugin extension point
		final List<IPlugin> plugins = plugman.getExtensions(IPlugin.class);
		for (final IPlugin plugin : plugins) {
			availablePlugins.put(plugin.getId(), plugin);
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static Map<String, IPlugin> getBenchmarkingPlugins() {
		// Avoid using instanceof, filter already found plugins
		final List<IBenchmarkingPlugin> benchmarkingPlugins = plugman.getExtensions(IBenchmarkingPlugin.class);
		LOGGER.debug("PF4J reports: " + benchmarkingPlugins.size() + " extensions of IBenchmarkingPlugin found");
		LOGGER.debug("TraVarTPluginManager has already registered following plugins: " + availablePlugins);
		final Map<String, IPlugin> toReturn = benchmarkingPlugins.stream().collect(Collectors.toMap(IPlugin::getName, Function.identity()));
		return Collections.unmodifiableMap(toReturn);
	}

	/**
	 * A static function to get the available plugins in the system.
	 */
	public static Map<String, IPlugin> getAvailablePlugins() {
		return Collections.unmodifiableMap(availablePlugins);
	}

	/**
	 * A static function to stop the available plugins in the system.
	 */
	public static void stopPlugins() {
		plugman.stopPlugins();
	}
}
