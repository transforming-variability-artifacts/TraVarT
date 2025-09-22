/*******************************************************************************
 * This Source Code Form is subject to the terms of the Mozilla
 * Public License, v. 2.0. If a copy of the MPL was not distributed
 * with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 *
 * Contributors:
 *     @author Kevin Feichtinger
 *
 * Base implementation for optimizing the core model.
 *
 * Copyright 2023 Johannes Kepler University Linz
 * LIT Cyber-Physical Systems Lab
 * All rights reserved
 *******************************************************************************/
package at.jku.cps.travart.core.optimize;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.logicng.formulas.FormulaFactory;

import at.jku.cps.travart.core.common.IModelOptimizer;
import at.jku.cps.travart.core.factory.impl.CoreModelFactory;
import at.jku.cps.travart.core.helpers.TraVarTUtils;
import de.vill.model.Feature;
import de.vill.model.FeatureModel;
import de.vill.model.Group;
import de.vill.model.constraint.Constraint;
import de.vill.model.constraint.LiteralConstraint;

public class DefaultCoreModelOptimizer implements IModelOptimizer<FeatureModel> {

	private static final CoreModelFactory factory = CoreModelFactory.getInstance();
	private static DefaultCoreModelOptimizer instance;

	private DefaultCoreModelOptimizer() {

	}

	public static DefaultCoreModelOptimizer getInstance() {
		if (instance == null) {
			instance = new DefaultCoreModelOptimizer();
		}
		return instance;
	}

	private static void fixFalseOptionalFeaturesByFeatureGroupConstraints(final FeatureModel fm,
			final Feature feature) {
		final Set<Feature> children = TraVarTUtils.getChildren(feature);
		for (final Feature child : children) {
			fixFalseOptionalFeaturesByFeatureGroupConstraints(fm, child);
		}
		// if there is a requires constraint in the feature model between parent and
		// child, we can remove the constraint and make the child mandatory
		for (final Feature childFeature : children) {
			final de.vill.model.constraint.Constraint requiredConstraint = factory.createImplicationConstraint(
					factory.createLiteralConstraint(TraVarTUtils.getFeatureName(feature)),
					factory.createLiteralConstraint(TraVarTUtils.getFeatureName(childFeature)));
			if (TraVarTUtils.hasOwnConstraint(fm, requiredConstraint)) {
				TraVarTUtils.setGroup(fm, childFeature, feature, Group.GroupType.MANDATORY);
				TraVarTUtils.removeOwnConstraint(fm, requiredConstraint);
			}
		}
	}

	private static void fixFalseOptionalFeaturesByConstraints(final FeatureModel fm)
			throws ReflectiveOperationException {
		final List<de.vill.model.constraint.Constraint> toDelete = new ArrayList<>();
		for (final de.vill.model.constraint.Constraint constr : TraVarTUtils.getOwnConstraints(fm)) {
			final de.vill.model.constraint.Constraint cnf = TraVarTUtils.buildConstraintFromFormula(
					TraVarTUtils.buildFormulaFromConstraint(constr, new FormulaFactory()).cnf());

			if (TraVarTUtils.isRequires(constr)) {
				final de.vill.model.constraint.Constraint left = TraVarTUtils.getLeftConstraint(cnf);
				final de.vill.model.constraint.Constraint right = TraVarTUtils.getRightConstraint(cnf);
				if (left != null && right != null && TraVarTUtils.isNegativeLiteral(left)
						&& TraVarTUtils.isLiteral(right)) {
					final Feature leftFeature = TraVarTUtils.getFeature(fm,
							((LiteralConstraint) left.getConstraintSubParts().get(0)).getLiteral());
					final Feature rightFeature = TraVarTUtils.getFeature(fm, ((LiteralConstraint) right).getLiteral());

					if (TraVarTUtils.isInGroup(rightFeature, Group.GroupType.MANDATORY)) {
						toDelete.add(constr);
					} else if (TraVarTUtils.isInGroup(leftFeature, Group.GroupType.MANDATORY) // &&
					// TraVarTUtils.isInGroup(rightFeature,
					// Group.GroupType.OPTIONAL)
					) {
						TraVarTUtils.setGroup(fm, rightFeature, rightFeature.getParentFeature(),
								Group.GroupType.MANDATORY);
						toDelete.add(constr);
					}
				}
			}
		}
		toDelete.forEach(fm.getOwnConstraints()::remove);
	}

	// TODO: check - Is this method correct? Is it necessary?!
	private static void fixFalseOptionalFeaturesByAbstractFeatureGroup(final FeatureModel fm, final Feature feature) {
		final Set<Feature> children = TraVarTUtils.getChildren(feature);
		for (final Feature child : children) {
			fixFalseOptionalFeaturesByAbstractFeatureGroup(fm, child);
		}
		if (!children.isEmpty() && TraVarTUtils.isAbstract(feature)
//				&& !TraVarTUtils.isAbstract(feature.getParentFeature())
				&& (TraVarTUtils.checkGroupType(feature, Group.GroupType.OR)
						|| TraVarTUtils.checkGroupType(feature, Group.GroupType.ALTERNATIVE))) {
			// abstract features where all child features are mandatory are also mandatory
			boolean childMandatory = true;
			for (final Feature childFeature : children) {
				childMandatory = childMandatory && TraVarTUtils.checkGroupType(childFeature, Group.GroupType.MANDATORY);
			}
			if (childMandatory) {
				TraVarTUtils.setGroup(fm, feature, feature.getParentFeature(), Group.GroupType.MANDATORY);
			}
		}
	}

	private static void transformConstraintsToAlternativeGroup(final FeatureModel fm, final Feature feature) {
		final Set<Feature> children = TraVarTUtils.getChildren(feature);
		final Set<de.vill.model.constraint.Constraint> relevantExcludesConstraints = new HashSet<>();
		for (final Feature childFeature : children) {
			transformConstraintsToAlternativeGroup(fm, childFeature);
			final Set<Feature> otherChildren = new HashSet<>(children);
			otherChildren.remove(childFeature);
			for (final Feature other : otherChildren) {
				final de.vill.model.constraint.Constraint constraint = factory.createImplicationConstraint(
						factory.createLiteralConstraint(TraVarTUtils.getFeatureName(childFeature)),
						factory.createNotConstraint(
								factory.createLiteralConstraint(TraVarTUtils.getFeatureName(other))));
				// TODO: toStringEquals: hack for now but should be removed as soon library
				// properly overrieds equals hashcode
				if (TraVarTUtils.hasOwnConstraint(fm, constraint)
						|| toStringEquals(fm.getOwnConstraints(), constraint)) {
					relevantExcludesConstraints.add(constraint);
				}
			}
		}
		if (isAlternativeGroup(children, relevantExcludesConstraints)) {
			children.forEach(c -> TraVarTUtils.setGroup(fm, c, feature, Group.GroupType.ALTERNATIVE));
			// TODO: toStringEquals: hack for now but should be removed as soon library
			// properly overrieds equals hashcode; code in the next line is the replacement
			relevantExcludesConstraints.forEach(c -> removeExcludesConstraint(fm.getOwnConstraints(), c));
			// relevantExcludesConstraints.forEach(c -> TraVarTUtils.removeOwnConstraint(fm,
			// c));
		}
	}

	private static void removeExcludesConstraint(final List<Constraint> ownConstraints, final Constraint constraint) {
		final Optional<Constraint> constr = ownConstraints.stream()
				.filter(oc -> oc.toString().equals(constraint.toString())).findFirst();
		constr.ifPresent(ownConstraints::remove);
	}

	private static boolean toStringEquals(final List<Constraint> ownConstraints, final Constraint constraint) {
		return ownConstraints.stream().anyMatch(oc -> oc.toString().equals(constraint.toString()));
	}

	private static boolean isAlternativeGroup(final Set<Feature> children,
			final Set<de.vill.model.constraint.Constraint> relevantExcludesConstraints) {
		return !relevantExcludesConstraints.isEmpty()
				&& children.size() * (children.size() - 1) <= relevantExcludesConstraints.size();
	}

	@Override
	public void optimize(final FeatureModel fm, final STRATEGY level) {
		// find mandatory features within feature groups
		fixFalseOptionalFeaturesByFeatureGroupConstraints(fm, TraVarTUtils.getRoot(fm));
		// find mandatory features within abstract feature groups
		fixFalseOptionalFeaturesByAbstractFeatureGroup(fm, TraVarTUtils.getRoot(fm));
		// find alternative groups
		transformConstraintsToAlternativeGroup(fm, TraVarTUtils.getRoot(fm));
		// find mandatory features within requires constraints
		try {
			fixFalseOptionalFeaturesByConstraints(fm);
		} catch (final ReflectiveOperationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			// remove unnecessary requires constraints
			removeUnnecessaryRequiresConstraints(fm);
		} catch (final ReflectiveOperationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// find duplicated Constraints and remove one of them
		fixDuplicatedConstraints(fm);
	}

	private void removeUnnecessaryRequiresConstraints(final FeatureModel fm) throws ReflectiveOperationException {
		final Iterator<de.vill.model.constraint.Constraint> iterator = TraVarTUtils.getOwnConstraints(fm).iterator();
		while (iterator.hasNext()) {
			final de.vill.model.constraint.Constraint constr = iterator.next();
			if (TraVarTUtils.isRequires(constr)) {
				final de.vill.model.constraint.Constraint cnf = TraVarTUtils.buildConstraintFromFormula(
						TraVarTUtils.buildFormulaFromConstraint(constr, new FormulaFactory()).cnf());
				final de.vill.model.constraint.Constraint right = TraVarTUtils.getRightConstraint(cnf);
				if (right != null && TraVarTUtils.isLiteral(right)) {
					final Feature rightFeature = TraVarTUtils.getFeature(fm, ((LiteralConstraint) right).getLiteral());
					if (TraVarTUtils.isInGroup(rightFeature, Group.GroupType.MANDATORY)) {
						iterator.remove();
					}
				}
			}
		}
	}

	private void fixDuplicatedConstraints(final FeatureModel fm) {
		TraVarTUtils.getOwnConstraints(fm).removeIf(constraint -> TraVarTUtils.getOwnConstraints(fm).stream()
				.filter(oc -> oc.toString().equals(constraint.toString())).count() > 1);
	}
}
