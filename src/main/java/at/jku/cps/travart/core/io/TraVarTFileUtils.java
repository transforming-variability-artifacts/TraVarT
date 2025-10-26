/*******************************************************************************
 * This Source Code Form is subject to the terms of the Mozilla
 * Public License, v. 2.0. If a copy of the MPL was not distributed
 * with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 *
 * Contributors:
 *     @author Kevin Feichtinger
 *
 * Implements a helper for obtaining files from the file system.
 *
 * Copyright 2023 Johannes Kepler University Linz
 * LIT Cyber-Physical Systems Lab
 * All rights reserved
 *******************************************************************************/
package at.jku.cps.travart.core.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class TraVarTFileUtils {

	private TraVarTFileUtils() {

	}

	public static Set<Path> getPathSet(final Path path, final String extension) throws IOException {
		return getPathSetForLevel(path, extension, 1);
	}

	public static Set<Path> getPathSetForLevel(final Path path, final String extension, final int level)
			throws IOException {
		try (Stream<Path> stream = Files.walk(path, level)) {
			// Return a sorted list of paths, ensure that the order of parsing is deterministic in batch mode
			return stream.filter(Files::isRegularFile)
					.filter(f -> f.getFileName().toString().endsWith(extension)).sorted().collect(Collectors.toCollection(LinkedHashSet::new));
		} catch (IOException e) {
			throw new IOException(e);
		}
	}
}
