/*******************************************************************************
 * This Source Code Form is subject to the terms of the Mozilla
 * Public License, v. 2.0. If a copy of the MPL was not distributed
 * with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 *
 * Contributors:
 *     @author Kevin Feichtinger
 *
 * The class provides access to statistical information of the pivot model.
 *
 * Copyright 2023 Johannes Kepler University Linz
 * LIT Cyber-Physical Systems Lab
 * All rights reserved
 *******************************************************************************/
package at.jku.cps.travart.core;

import java.util.Objects;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import at.jku.cps.travart.core.common.IStatistics;
import at.jku.cps.travart.core.helpers.TraVarTUtils;
import de.vill.model.Feature;
import de.vill.model.FeatureModel;
import de.vill.model.Group;

public final class FeatureModelStatistics implements IStatistics<FeatureModel> {

	private static FeatureModelStatistics instance;

	private FeatureModelStatistics() {
	}

	public static FeatureModelStatistics getInstance() {
		if (instance == null) {
			instance = new FeatureModelStatistics();
		}
		return instance;
	}

	@Override
	public int getVariabilityElementsCount(final FeatureModel fm) {
		return TraVarTUtils.getFeatureMapFromRoot(fm.getRootFeature()).size();
	}

	@Override
	public int getConstraintsCount(final FeatureModel fm) {
		return TraVarTUtils.getFeatureConstraints(fm).size() + TraVarTUtils.getGlobalConstraints(fm).size()
				+ TraVarTUtils.getLiteralConstraints(fm).size() + TraVarTUtils.getOwnConstraints(fm).size();
	}

	@Override
	public void logModelStatistics(final Logger logger, final FeatureModel fm) {
		logger.log(Level.INFO, "Root Name: {}", fm.getRootFeature().getFeatureName());
		logger.log(Level.INFO, "#Features: {}", getVariabilityElementsCount(fm));
		logger.log(Level.INFO, "#Abstract Features: {}", countAbstractFeatures(fm));
		logger.log(Level.INFO, "#Mandatory Features: {}", countMandatoryFeatures(fm));
		logger.log(Level.INFO, "#Optional Features: {}", countOptionalFeatures(fm));
		logger.log(Level.INFO, "#Or groups: {}", countOrGroups(fm));
		logger.log(Level.INFO, "#Xor groups: {}", countXorGroups(fm));
		logger.log(Level.INFO, "#Constraints: {}", getConstraintsCount(fm));
		logger.log(Level.INFO, "Tree height: {}", computeFMHeight(TraVarTUtils.getRoot(fm)));
	}

	private static int countAbstractFeatures(final FeatureModel fm) {
		return (int) TraVarTUtils.getFeatures(fm).stream().filter(TraVarTUtils::isAbstract).count();
	}

	private static int countMandatoryFeatures(final FeatureModel fm) {
		return (int) TraVarTUtils.getFeatures(fm).stream().filter(TraVarTUtils::isMandatory).count();
	}

	private static int countOptionalFeatures(final FeatureModel fm) {
		return (int) TraVarTUtils.getFeatures(fm).stream()
				.filter(f -> TraVarTUtils.checkGroupType(f, Group.GroupType.OPTIONAL)).count();
	}

	private static int countOrGroups(final FeatureModel fm) {
		return countGroupType(fm, Group.GroupType.OR);
	}

	private static int countXorGroups(final FeatureModel fm) {
		return countGroupType(fm, Group.GroupType.ALTERNATIVE);
	}

	private static int countGroupType(final FeatureModel fm, final Group.GroupType grouptype) {
		int count = 0;
		for (Feature feature : TraVarTUtils.getFeatures(fm)) {
			count += TraVarTUtils.countGroup(feature, grouptype);
		}
		return count;
	}

	private static int computeFMHeight(final Feature feature) {
		if (!TraVarTUtils.hasChildren(feature)) {
			return 0;
		}
		int maxDepth = Integer.MIN_VALUE;
		for (Feature child : TraVarTUtils.getChildren(feature)) {
			int depth = computeFMHeight(child);
			if (maxDepth < depth) {
				maxDepth = depth;
			}
		}
		return 1 + maxDepth;
	}
}
