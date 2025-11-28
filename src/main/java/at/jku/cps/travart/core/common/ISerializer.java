/*******************************************************************************
 * This Source Code Form is subject to the terms of the Mozilla
 * Public License, v. 2.0. If a copy of the MPL was not distributed
 * with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 *
 * Contributors:
 *     @author Kevin Feichtinger
 * 	   @author Jakob Gretenkort
 *
 * The base interface for writing a variability artifact.
 *
 * Copyright 2023 Johannes Kepler University Linz
 * LIT Cyber-Physical Systems Lab
 * All rights reserved
 *******************************************************************************/
package at.jku.cps.travart.core.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import at.jku.cps.travart.core.exception.NotSupportedVariabilityTypeException;

/**
 * The interface defines a serializer, which enables the serialization of a
 * variability model of type <T>.
 *
 * @param <T> The type of model which should be serialized.
 * @author Kevin Feichtinger
 */
public interface ISerializer<T> {
	/**
	 * Serializes a variability model of type <T> and writes the serialization
     * to the given path.
	 *
	 * @param model    the model to serialize.
	 * @param filePath the path to which the file should be written.
	 * @throws IOException                          if the writing operation throws
	 *                                              any kind of error.
	 * @throws NotSupportedVariabilityTypeException if the given variability model
	 *                                              can not be serialized.
	 */
	default Path serializeToFile(T model, Path filePath) throws IOException, NotSupportedVariabilityTypeException {
        if (this.getFormat().isText()) {
            return Files.writeString(filePath, serialize(model));
        } else {
            throw new NotSupportedVariabilityTypeException("This serializer does not support text-based serialization.");
        }
    }

    /**
     * Serializes the variability model of type <T>.
     * Only supported if this serializer's format is text-based.
     *
     * @param model the model to serialize.
     * @return The serialized model in the format supported by this serializer.
     * @throws NotSupportedVariabilityTypeException if the given variability
     *                                              model cannot be serialized.
     */
    String serialize(T model) throws NotSupportedVariabilityTypeException;

	/**
	 * The format this serializer uses.
	 *
	 * @return the format this serializer uses.
	 */
	Format getFormat();

	/**
	 * The file extension this writer uses.
	 * Legacy method.
     *
	 * @return the file extension this writer uses.
	 */
	default String getFileExtension() {
        return getFormat().extension();
    }
}
