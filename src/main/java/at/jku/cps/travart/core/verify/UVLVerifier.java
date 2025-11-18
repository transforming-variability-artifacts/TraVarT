/*******************************************************************************
 * This Source Code Form is subject to the terms of the Mozilla
 * Public License, v. 2.0. If a copy of the MPL was not distributed
 * with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 *
 * Contributors:
 *     @author Dario Romano
 *
 * Implements various utilities to verify transformation/optimization results
 *
 * Copyright 2024 Johannes Kepler University Linz
 * LIT Cyber-Physical Systems Lab
 * All rights reserved
 *******************************************************************************/
package at.jku.cps.travart.core.verify;

import static at.jku.cps.travart.core.verify.LogicOperator.NOT;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.logging.log4j.util.Strings;
import org.logicng.formulas.CType;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;
import org.logicng.io.parsers.FormulaParser;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;
import org.logicng.solvers.MaxSATSolver;

import at.jku.cps.travart.core.exception.VerificationException;
import de.vill.model.Feature;
import de.vill.model.FeatureModel;
import de.vill.model.Group;
import de.vill.model.constraint.Constraint;

public final class UVLVerifier {
	private static final Logger logger = Logger.getLogger(UVLVerifier.class.getName());

	/**
	 * forbid public constructor because this is purely a static utility class
	 */
	private UVLVerifier() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * verifies if the two passed UVL models are logically equivalent and therefore
	 * have the same configuration space. Requires that features in the models have
	 * the same names.
	 * 
	 * @param fm1 The first model
	 * @param fm2 the second model
	 * @return returns true if the verification was successful. Throws a
	 *         VerificationException if the verification failed containing the logic
	 *         formulas for both models, and a configuration that leads to an
	 *         un-equal configuration state for both models.
	 */
	public static boolean verify(FeatureModel fm1, FeatureModel fm2) throws VerificationException {
		if (!equals(fm1, fm2)) {
			FormulaFactory ff = new FormulaFactory();
			Formula formulaModel1 = getModelsAsFormula(ff, fm1);
			Formula formulaModel2 = getModelsAsFormula(ff, fm2);
			Formula equalityFormula = ff.not(ff.equivalence(formulaModel1, formulaModel2));
			MaxSATSolver solver = MaxSATSolver.msu3(ff);
			solver.addHardFormula(equalityFormula);
			solver.solve();

			throw new VerificationException("Verification failed.\n", formulaModel1, formulaModel2,
					solver.model().toString());
		}
		return true;
	}

	/**
	 * Checks two UVL models for equality. This should guarantee the same
	 * configuration space. Both models need to have an identical feature-set.
	 * 
	 * @param fm1 The first UVL model
	 * @param fm2 The second UVL model to compare it with
	 * @return true if models have same config space, false if not
	 */
	public static boolean equals(FeatureModel fm1, FeatureModel fm2) {
		FormulaFactory ff = new FormulaFactory();
		Formula formulaModel1 = getModelsAsFormula(ff, fm1);
		Formula formulaModel2 = getModelsAsFormula(ff, fm2);

		return formulaModel1.isEquivalentTo(formulaModel2);
	}

	/**
	 * Transforms a number of UVL constraints into a set of logicNG formula objects.
	 * 
	 * @param ff          the formulafactory used to create the formulas (needs to
	 *                    be identical for all formulas that need to be compared
	 *                    with the result of this method)
	 * @param constraints the uvl constraint objects to be translated.
	 * @return a set of formulas to be processed with logicNG
	 */
	public static Set<Formula> uvlConstraintstoFormulas(FormulaFactory ff, Collection<Constraint> constraints) {
		return constraints.stream().map(c -> c.toString(false, Strings.EMPTY)).map(c -> c.replace(NOT.uvl(), NOT.ng()))
				.map(c -> UVLVerifier.parseUVLconstraintAsFormula(ff, c)).filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}

	/**
	 * Transforms the model into a logic formula to be processes with logicNG
	 * 
	 * @param ff the formulafactory used to create the formulas (needs to be
	 *           identical for all formulas that need to be compared with the result
	 *           of this method)
	 * @param fm the uvl model from which to get the constraints
	 * @return a logic formula that represents the uvl model
	 */
	public static Formula getModelsAsFormula(FormulaFactory ff, FeatureModel fm) {
		Formula formula = ff.and(uvlConstraintstoFormulas(ff, fm.getConstraints()));
		return ff.equivalence(ff.literal(fm.getRootFeature().getFeatureName(), true),
				ff.and(formula, getFormulaFromUVLTree(ff, fm)));
	}

	/**
	 * parses a logicNG formula from an UVL-constraint represented by string s.
	 * Requires a pre-processing step where all negation signs "!" from uvl have
	 * been replaces by "~" needed by the parser.
	 * 
	 * @param ff the formulafactory used to create the formulas (needs to be
	 *           identical for all formulas that need to be compared with the result
	 *           of this method)
	 * @param s  the pre-processes constraint-string
	 * @return A formula object representing the constraints
	 */
	public static Formula parseUVLconstraintAsFormula(FormulaFactory ff, String s) {
		final FormulaParser parser = new PropositionalParser(ff);
		try {
			return parser.parse(s);
		} catch (ParserException e) {
			logger.warning("Verifier has failed to parse the following UVL constraint:\n" + s);
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Generates a logic formula that represents the feature-tree of a UVL model
	 * 
	 * @param ff the formulafactory used to create the formulas (needs to be
	 *           identical for all formulas that need to be compared with the result
	 *           of this method)
	 * @param fm the uvl model from which to generate the formula
	 * @return
	 */
	public static Formula getFormulaFromUVLTree(FormulaFactory ff, FeatureModel fm) {
		return ff.and(fm.getFeatureMap().values().stream().flatMap(f -> f.getChildren().stream())
				.map(g -> getGroupAsFormula(ff, g)).toList());
	}

	/**
	 * Transforms a list of features to their respective variables for a logicNG
	 * formula
	 * 
	 * @param ff       the formulafactory used to create the formulas (needs to be
	 *                 identical for all formulas that need to be compared with the
	 *                 result of this method)
	 * @param features the features to be converted to variables
	 * @return a collection of variables corresponding to the passed features
	 */
	private static Collection<Variable> literalsFromFeatures(FormulaFactory ff, Collection<Feature> features) {
		return features.stream().map(f -> ff.variable(f.getFeatureName())).toList();
	}

	/**
	 * Converts a group to a logicNG formula
	 * 
	 * @param ff the formulafactory used to create the formulas (needs to be
	 *           identical for all formulas that need to be compared with the result
	 *           of this method)
	 * @param g  the group to be changed into a logic formula
	 * @return a logicNG formula representing the logic encoded in a group
	 */
	private static Formula getGroupAsFormula(FormulaFactory ff, Group g) {
		Collection<Variable> literals = literalsFromFeatures(ff, g.getFeatures());
		Literal parentLiteral = ff.literal(g.getParentFeature().getFeatureName(), true);
		switch (g.GROUPTYPE) {
		case OR:
			return ff.and(ff.implication(ff.or(literals), parentLiteral), ff.or(literals));
		case ALTERNATIVE:
			return ff.and(ff.implication(ff.or(literals), parentLiteral), ff.exo(literals));
		case MANDATORY:
			return ff.implication(parentLiteral, ff.and(literals));
		case OPTIONAL:
			return ff.implication(ff.or(literals), parentLiteral);
		case GROUP_CARDINALITY:
			return ff.and(ff.implication(ff.or(literals), parentLiteral),
					ff.and(ff.cc(CType.GE, Integer.valueOf(g.getLowerBound()), literals),
							ff.cc(CType.LE, Integer.valueOf(g.getUpperBound()), literals)));
		default:
			return ff.verum();
		}
	}

}
