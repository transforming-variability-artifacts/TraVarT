/*******************************************************************************
 * This Source Code Form is subject to the terms of the Mozilla
 * Public License, v. 2.0. If a copy of the MPL was not distributed
 * with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 *
 * Contributors:
 *     @author Kevin Feichtinger
 *     @author Prankur Agarwal
 *
 * The base interface for obtaining statistical data of a variability artifact.
 *
 * Copyright 2023 Johannes Kepler University Linz
 * LIT Cyber-Physical Systems Lab
 * All rights reserved
 *******************************************************************************/
package at.jku.cps.travart.core.common;

import org.apache.logging.log4j.Logger;

/**
 * This base interface defines methods to derive statistics of a given
 * variability model of type <T>.
 *
 * @param <T> The type of the variability model.
 * @author Prankur Agarwal
 * @author Kevin Feichtinger
 */
public interface IStatistics<T> {
	/**
	 * Returns the number of variability unit elements, e.g., features, decisions in
	 * the given variability model.
	 *
	 * @param model the model from which the unit of elements should be counted.
	 * @return the number of variability elements.
	 */
	int getVariabilityElementsCount(T model);

	/**
	 * Returns the number of constraints in the given variability model.
	 *
	 * @param model the model from which the constraints should be counted.
	 * @return the number of constraints.
	 */
	int getConstraintsCount(T model);

	/**
	 * Logs the model statistics.
	 *
	 * @param logger the logger for the class
	 * @param model  the model from which the constraints should be counted.
	 */
	void logModelStatistics(final Logger logger, final T model);

}
