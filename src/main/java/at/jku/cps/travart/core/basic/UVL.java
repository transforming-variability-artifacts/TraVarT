/*******************************************************************************
 * This Source Code Form is subject to the terms of the Mozilla
 * Public License, v. 2.0. If a copy of the MPL was not distributed
 * with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 *
 * Contributors:
 *     @author Kevin Feichtinger
 *     @author Prankur Agarwal
 * 	   @author Jakob Gretenkort
 *
 * An implementation of the universal variability language.
 *
 * Copyright 2023 Johannes Kepler University Linz
 * LIT Cyber-Physical Systems Lab
 * All rights reserved
 *******************************************************************************/
package at.jku.cps.travart.core.basic;

import at.jku.cps.travart.core.FeatureModelStatistics;
import at.jku.cps.travart.core.common.IDeserializer;
import at.jku.cps.travart.core.common.ILanguage;
import at.jku.cps.travart.core.common.ISerializer;
import at.jku.cps.travart.core.common.IStatistics;
import at.jku.cps.travart.core.io.UVLDeserializer;
import at.jku.cps.travart.core.io.UVLSerializer;
import de.vill.model.FeatureModel;

/**
 * An implementation of the universal variability language, providing
 */
public class UVL implements ILanguage<FeatureModel> {

    @Override
    public IDeserializer<FeatureModel> getDeserializer() {
        return new UVLDeserializer();
    }

    @Override
    public IStatistics<FeatureModel> getStatistics() {
        return new FeatureModelStatistics();
    }

    @Override
    public ISerializer<FeatureModel> getSerializer() {
        return new UVLSerializer();
    }

    @Override
    public String getName() {
        return "Universal Variability Language";
    }

    @Override
    public String getAbbreviation(){
        return "UVL";
    }

    @Override
    public Iterable<String> getSupportedFileExtensions() {
        return getDeserializer().fileExtensions();
    }
}
