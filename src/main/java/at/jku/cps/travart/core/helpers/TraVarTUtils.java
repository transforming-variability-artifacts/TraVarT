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
 * Implements a helper to build and maintain the core model.
 *
 * Copyright 2023 Johannes Kepler University Linz
 * LIT Cyber-Physical Systems Lab
 * All rights reserved
 *******************************************************************************/
package at.jku.cps.travart.core.helpers;

import static at.jku.cps.travart.core.transformation.DefaultModelTransformationProperties.ABSTRACT_ATTRIBUTE;
import static at.jku.cps.travart.core.transformation.DefaultModelTransformationProperties.HIDDEN_ATTRIBUTE;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;

import org.logicng.formulas.FType;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Or;

import at.jku.cps.travart.core.common.IConfigurable;
import at.jku.cps.travart.core.common.ILanguage;
import at.jku.cps.travart.core.common.IStatistics;
import at.jku.cps.travart.core.transformation.DefaultModelTransformationProperties;
import de.vill.main.UVLModelFactory;
import de.vill.model.Attribute;
import de.vill.model.Feature;
import de.vill.model.FeatureModel;
import de.vill.model.Group;
import de.vill.model.Group.GroupType;
import de.vill.model.constraint.AndConstraint;
import de.vill.model.constraint.Constraint;
import de.vill.model.constraint.EquivalenceConstraint;
import de.vill.model.constraint.ImplicationConstraint;
import de.vill.model.constraint.LiteralConstraint;
import de.vill.model.constraint.NotConstraint;
import de.vill.model.constraint.OrConstraint;
import de.vill.model.constraint.ParenthesisConstraint;

public class TraVarTUtils {
	private static final UVLModelFactory factory = new UVLModelFactory();
	private static final FormulaFactory formulaFactory = new FormulaFactory();

	private TraVarTUtils() {
	}

	/**
	 * Splits the given String by the given delimiter.
	 *
	 * @param toSplit   the String to split.
	 * @param delimiter the delimiter to split the String.
	 * @return a array of Strings containing the elements of the original String.
	 */
	public static String[] splitString(final String toSplit, final String delimiter) {
		return Arrays.stream(toSplit.split(delimiter)).map(String::trim).filter(s -> !s.isEmpty() && !s.isBlank())
				.toArray(String[]::new);
	}

	/**
	 * Creates a Set of names of the selected IConfigurable names.
	 *
	 * @param samples - the samples of the feature model.
	 * @return A Set of name sets of the configuration samples.
	 */
	public static Set<Set<String>> createConfigurationNameSet(final Set<Map<IConfigurable, Boolean>> samples) {
		Objects.requireNonNull(samples);
		final Set<Set<String>> configurations = new HashSet<>();
		for (final Map<IConfigurable, Boolean> sample : samples) {
			final Set<String> sampleNames = new HashSet<>();
			for (final Map.Entry<IConfigurable, Boolean> sampleEntry : sample.entrySet()) {
				if (Boolean.TRUE.equals(sampleEntry.getValue())) {
					sampleNames.add(sampleEntry.getKey().getName());
				}
			}
			configurations.add(sampleNames);
		}
		return configurations;
	}

	/**
	 * Recursively builds a featureMap starting from the passed root.
	 *
	 * @param feature the root of the tree
	 * @return A map of all features in the tree with their names as keys
	 */
	public static Map<String, Feature> getFeatureMapFromRoot(final Feature feature) {
		Objects.requireNonNull(feature);
		final Map<String, Feature> featureMap = new HashMap<>();
		// add self
		featureMap.put(feature.getFeatureName(), feature);
		if (feature.getChildren().isEmpty()) {
			return featureMap;
		}
		final List<Feature> childFeatures = feature.getChildren().stream().flatMap(g -> g.getFeatures().stream())
				.collect(Collectors.toList());
		// add all children
		for (final Feature childFeature : childFeatures) {
			featureMap.putAll(getFeatureMapFromRoot(childFeature));
		}
		return featureMap;
	}

	/**
	 * Returns {@code true} if the given feature does not have a parent feature and
	 * is a root.
	 *
	 * @param feature the feature to be checked
	 * @return {@code true} if the given feature does not have a parent feature and
	 *         is a root, otherwise {@code false}.
	 */
	public static boolean isRoot(final Feature feature) {
		return !hasParentFeature(feature);
	}

	/**
	 * Returns {@code true} if the given feature does specify a parent feature.
	 *
	 * @param feature the feature to check.
	 * @return {@code true} if the given feature does specify a parent feature,
	 *         otherwise {@code false}.
	 */
	public static boolean hasParentFeature(final Feature feature) {
		return getParentFeature(feature) != null;
	}

	/**
	 * Returns the parent feature of the the given feature.
	 *
	 * @param feature the feature to return the parent from.
	 * @return the parent feature of the the given feature.
	 */
	public static Feature getParentFeature(final Feature feature) {
		return Objects.requireNonNull(feature).getParentFeature();
	}

	/**
	 * Sets the given feature to be the root of the feature model.
	 *
	 * @param fm      the feature model to set the new root.
	 * @param feature the new root feature.
	 */
	public static void setRoot(final FeatureModel fm, final Feature feature) {
		fm.setRootFeature(feature);
	}

	/**
	 * Returns the root feature of the given feature model.
	 *
	 * @param fm the feature model to return the root feature from.
	 * @return the root feature of the feature model.
	 */
	public static Feature getRoot(final FeatureModel fm) {
		return Objects.requireNonNull(fm).getRootFeature();
	}

	/**
	 * Returns if the given feature model has a root feature specified or not.
	 *
	 * @param fm the feature model to check if it has a root feature specified.
	 * @return {@code true} if the given feature model has a root feature, otherwise
	 *         {@code false}.
	 */
	public static boolean hasRoot(final FeatureModel fm) {
		return Objects.requireNonNull(fm).getRootFeature() != null;
	}

	/**
	 * returns the {@link List} of own {@link Constraint}s of the given feature
	 * model.
	 *
	 * @param fm the feature model from which the own {@link Constraint}s are
	 *           returned.
	 * @return A {@link List} of own {@link Constraint}s of the feature model.
	 */
	public static List<Constraint> getOwnConstraints(final FeatureModel fm) {
		return Objects.requireNonNull(fm).getOwnConstraints();
	}

	/**
	 * Adds the given constraint as an own {@link Constraint} to the given feature
	 * model.
	 *
	 * @param fm         the feature model to which the constraint is added.
	 * @param constraint the constraint to add.
	 */
	public static void addOwnConstraint(final FeatureModel fm, final Constraint constraint) {
		getOwnConstraints(fm).add(Objects.requireNonNull(constraint));
	}

	public static void addOwnConstraints(final FeatureModel fm, final Collection<Constraint> constraints) {
		for (final Constraint c : constraints) {
			getOwnConstraints(fm).add(Objects.requireNonNull(c));
		}
	}

	/**
	 * Removes the given constraint from the given feature model.
	 *
	 * @param fm         the feature model from which the constraint is removed.
	 * @param constraint the constraint to remove.
	 */
	public static void removeOwnConstraint(final FeatureModel fm, final Constraint constraint) {
		getOwnConstraints(fm).remove(Objects.requireNonNull(constraint));
	}

	/**
	 * Returns true if the given constraint is contained in the feature model own
	 * constraints, otherwise false.
	 *
	 * @param fm         the feature model to check if it contains this own
	 *                   constraint.
	 * @param constraint the constraint to check.
	 * @return true if the given constraint is contained in the feature model own
	 *         constraints, otherwise false
	 */
	public static boolean hasOwnConstraint(final FeatureModel fm, final Constraint constraint) {
		return getOwnConstraints(fm).contains(Objects.requireNonNull(constraint));
	}

	/**
	 * returns the {@link List} of {@link LiteralConstraint}s of the given feature
	 * model.
	 *
	 * @param fm the feature model from which the {@link LiteralConstraint}s are
	 *           returned.
	 * @return A {@link List} of {@link LiteralConstraint}s of the feature model
	 */
	public static List<LiteralConstraint> getLiteralConstraints(final FeatureModel fm) {
		return Objects.requireNonNull(fm).getLiteralConstraints();
	}

	/**
	 * Adds the given constraint as an {@link LiteralConstraint} to the given
	 * feature model.
	 *
	 * @param fm         the feature model to which the constraint is added.
	 * @param constraint the literal constraint to add.
	 */
	public static void addLiteralConstraint(final FeatureModel fm, final LiteralConstraint constraint) {
		getLiteralConstraints(fm).add(Objects.requireNonNull(constraint));
	}

	/**
	 * Removes the given {@link LiteralConstraint} from the given feature model.
	 *
	 * @param fm         the feature model from which the constraint is removed.
	 * @param constraint the constraint to remove.
	 */
	public static void removeLiteralConstraint(final FeatureModel fm, final LiteralConstraint constraint) {
		getLiteralConstraints(fm).remove(Objects.requireNonNull(constraint));
	}

	/**
	 * Returns true if the given {@link LiteralConstraint} is contained in the
	 * feature model own constraints, otherwise false.
	 *
	 * @param fm         the feature model to check if it contains this
	 *                   {@link LiteralConstraint}.
	 * @param constraint the constraint to check.
	 * @return true if the given {@link LiteralConstraint} is contained in the
	 *         feature model own constraints, otherwise false
	 */
	public static boolean hasOwnConstraint(final FeatureModel fm, final LiteralConstraint constraint) {
		return getLiteralConstraints(fm).contains(Objects.requireNonNull(constraint));
	}

	/**
	 * returns the {@link List} of feature {@link Constraint}s of the given feature
	 * model.
	 *
	 * @param fm the feature model from which the feature {@link Constraint}s are
	 *           returned.
	 * @return A {@link List} of feature {@link Constraint}s of the feature model
	 */
	public static List<Constraint> getFeatureConstraints(final FeatureModel fm) {
		return Objects.requireNonNull(fm).getFeatureConstraints();
	}

	/**
	 * Adds the given constraint as a feature {@link Constraint} to the given
	 * feature model.
	 *
	 * @param fm         the feature model to which the constraint is added.
	 * @param constraint the constraint to add.
	 */
	public static void addFeatureConstraint(final FeatureModel fm, final Constraint constraint) {
		getFeatureConstraints(fm).add(Objects.requireNonNull(constraint));
	}

	public static void addFeatureConstraints(final FeatureModel fm, final Collection<Constraint> constraints) {
		for (final Constraint c : constraints) {
			getFeatureConstraints(fm).add(Objects.requireNonNull(c));
		}
	}

	/**
	 * Removes the given feature {@link Constraint} from the given feature model.
	 *
	 * @param fm         the feature model from which the constraint is removed.
	 * @param constraint the constraint to remove.
	 */
	public static void removeFeatureConstraint(final FeatureModel fm, final Constraint constraint) {
		getFeatureConstraints(fm).remove(Objects.requireNonNull(constraint));
	}

	/**
	 * Returns true if the given feature {@link Constraint} is contained in the
	 * feature model own constraints, otherwise false.
	 *
	 * @param fm         the feature model to check if it contains this feature
	 *                   {@link Constraint}.
	 * @param constraint the constraint to check.
	 * @return true if the given feature {@link Constraint} is contained in the
	 *         feature model own constraints, otherwise false
	 */
	public static boolean hasFeatureConstraint(final FeatureModel fm, final Constraint constraint) {
		return getFeatureConstraints(fm).contains(Objects.requireNonNull(constraint));
	}

	/**
	 * returns the {@link List} of global {@link Constraint}s of the given feature
	 * model.
	 *
	 * @param fm the feature model from which the global {@link Constraint}s are
	 *           returned.
	 * @return A {@link List} of global {@link Constraint}s of the feature model
	 */
	public static List<Constraint> getGlobalConstraints(final FeatureModel fm) {
		return Objects.requireNonNull(fm).getConstraints();
	}

	/**
	 * Adds the given constraint as a global {@link Constraint} to the given feature
	 * model.
	 *
	 * @param fm         the feature model to which the constraint is added.
	 * @param constraint the constraint to add.
	 */
	public static void addGlobalConstraint(final FeatureModel fm, final Constraint constraint) {
		getGlobalConstraints(fm).add(Objects.requireNonNull(constraint));
	}

	public static void addGlobalConstraints(final FeatureModel fm, final Collection<Constraint> constraints) {
		for (final Constraint c : constraints) {
			getGlobalConstraints(fm).add(Objects.requireNonNull(c));
		}
	}

	/**
	 * Removes the given global {@link Constraint} from the given feature model.
	 *
	 * @param fm         the feature model from which the constraint is removed.
	 * @param constraint the constraint to remove.
	 */
	public static void removeGlobalConstraint(final FeatureModel fm, final Constraint constraint) {
		getGlobalConstraints(fm).remove(Objects.requireNonNull(constraint));
	}

	/**
	 * Returns true if the given {@link Constraint} is contained in the feature
	 * model global constraints, otherwise false.
	 *
	 * @param fm         the feature model to check if it contains this global
	 *                   constraint.
	 * @param constraint the constraint to check.
	 * @return true if the given constraint is contained in the feature model global
	 *         constraints, otherwise false
	 */
	public static boolean hasGlobalConstraint(final FeatureModel fm, final Constraint constraint) {
		return getGlobalConstraints(fm).contains(Objects.requireNonNull(constraint));
	}

	/**
	 * This function recursively translates a propositional logic formula from the
	 * data model used in the de.neominik.uvl library to the data model used in the
	 * logicng library, to facilitate the later translation to pure variants
	 * relations. Does not allow expression constraints.
	 *
	 * @param constraint the current node of the propositional formula
	 * @param factory    The FormulaFactory required to create formulas for logicng
	 * @return the logic formula parsed for the logicng library
	 */
	public static Formula buildFormulaFromConstraint(final Constraint constraint, final FormulaFactory factory) {
		Objects.requireNonNull(constraint);
		Objects.requireNonNull(factory);
		Formula term = null;
		if (constraint instanceof ImplicationConstraint) {
			term = factory.implication(
					buildFormulaFromConstraint(((ImplicationConstraint) constraint).getLeft(), factory),
					buildFormulaFromConstraint(((ImplicationConstraint) constraint).getRight(), factory));
		} else if (constraint instanceof EquivalenceConstraint) {
			term = factory.equivalence(
					buildFormulaFromConstraint(((EquivalenceConstraint) constraint).getLeft(), factory),
					buildFormulaFromConstraint(((EquivalenceConstraint) constraint).getRight(), factory));
		} else if (constraint instanceof AndConstraint) {
			term = factory.and(buildFormulaFromConstraint(((AndConstraint) constraint).getLeft(), factory),
					buildFormulaFromConstraint(((AndConstraint) constraint).getRight(), factory));
		} else if (constraint instanceof OrConstraint) {
			term = factory.or(buildFormulaFromConstraint(((OrConstraint) constraint).getLeft(), factory),
					buildFormulaFromConstraint(((OrConstraint) constraint).getRight(), factory));
		} else if (constraint instanceof NotConstraint) {
			term = factory.not(buildFormulaFromConstraint(((NotConstraint) constraint).getContent(), factory));
		} else if (constraint instanceof ParenthesisConstraint) {
			term = buildFormulaFromConstraint(((ParenthesisConstraint) constraint).getContent(), factory);
		} else {
			term = factory.literal(((LiteralConstraint) constraint).getLiteral(), true);
		}
		return term;
	}

	/**
	 * Translates a Formula back to a constraint format.
	 *
	 * @param formula the formula in logicNG format
	 * @return the same formula represented by UVLs Constraint hierarchy.
	 */
	public static Constraint buildConstraintFromFormula(final Formula formula) {
		Objects.requireNonNull(formula);
		// replace negation sign for uvl parser to recognize it.
		return factory.parseConstraint(formula.toString().replace("~", "!"));
	}

	/**
	 * Recursively counts all literals in a given constraint tree.
	 *
	 * @param constraint The constraint to count the literals from
	 * @return the number of literals
	 */
	public static int countLiterals(final Constraint constraint) {
		Objects.requireNonNull(constraint);
		if (constraint instanceof LiteralConstraint) {
			return 1;
		}
		int i = 0;
		for (final Constraint subconst : constraint.getConstraintSubParts()) {
			i += countLiterals(subconst);
		}
		return i;
	}

	/**
	 * Iterate over the {@code createConfigurationNameSet} configurations and find
	 * the common feature names of the configurations
	 *
	 * @param samples - the samples of the feature model.
	 * @return A Set of name sets of the configuration samples.
	 */
	public static Set<String> getCommonConfigurationNameSet(final Set<Map<IConfigurable, Boolean>> samples) {
		final Set<Set<String>> configurations = createConfigurationNameSet(samples);
		final Iterator<Set<String>> iterator = configurations.iterator();
		Set<String> element = iterator.next();
		iterator.remove();
		final Set<String> commonNames = new HashSet<>(element);
		while (iterator.hasNext()) {
			element = iterator.next();
			commonNames.retainAll(element);
		}
		return commonNames;
	}

	/**
	 * Returns the name of the given feature.
	 *
	 * @param feature the feature from which the name is returned.
	 * @return the name of the feature.
	 */
	public static String getFeatureName(final Feature feature) {
		return Objects.requireNonNull(feature).getFeatureName();
	}

	/**
	 * adds the given feature to a feature model
	 *
	 * @param fm      the feature model to which the feature is added.
	 * @param feature the feature to add.
	 */
	public static void addFeature(final FeatureModel fm, final Feature feature) {
		Objects.requireNonNull(fm).getFeatureMap().put(getFeatureName(feature), feature);
	}

	/**
	 * returns the feature given by the id from the feature model as an Optional
	 * feature.
	 *
	 * @param fm the feature model in which the feature is hopefully present.
	 * @param id the feature name.
	 * @return returns an Optional feature, of the feature.
	 */
	public static Optional<Feature> getFeatureOptional(final FeatureModel fm, final String id) {
		return Optional.ofNullable(fm.getFeatureMap().get(id));
	}

	/**
	 * returns the feature given by the id from the feature model if available,
	 * otherwise <code>null</code>.
	 *
	 * @param fm the feature model in which the feature is hopefully present.
	 * @param id the feature name.
	 * @return returns the feature identified by id, otherwise null.
	 */
	public static Feature getFeature(final FeatureModel fm, final String id) {
		return getFeatureMapFromRoot(Objects.requireNonNull(fm).getRootFeature()).get(id);
	}

	/**
	 * returns the features of the given feature model.
	 *
	 * @param fm the feature model from which the features are returned.
	 * @return returns the features of the feature model.
	 */
	public static Collection<Feature> getFeatures(final FeatureModel fm) {
		return Objects.requireNonNull(fm).getFeatureMap().values();
	}

	/**
	 * Removes the given feature from a feature model.
	 *
	 * @param fm      the feature model from which the feature is removed.
	 * @param feature the feature to remove.
	 */
	public static void removeFeature(final FeatureModel fm, final Feature feature) {
		if (!isRoot(feature)) {
			Objects.requireNonNull(feature).getParentGroup().getFeatures().remove(feature);
		}
		Objects.requireNonNull(fm).getFeatureMap().remove(feature.getFeatureName());
	}

	/**
	 * checks if a feature is the child of another one.
	 *
	 * @param child  the feature to check
	 * @param parent the proposed parent feature
	 * @return boolean if child is - in fact - a child of parent
	 */
	public static boolean isParentOf(final Feature child, final Feature parent) {
		final Feature c = Objects.requireNonNull(child);
		final Feature p = Objects.requireNonNull(parent);
		if (TraVarTUtils.isRoot(c)) {
			return false;
		}
		return getParentFeature(c).equals(p);
	}

	/**
	 * Check if the given Feature should be translated into an Enumeration Type
	 * Decision for the DecisionModel transformation
	 *
	 * @param feature the feature to check
	 * @return boolean if feature should be enumeration decision
	 */
	public static boolean isEnumerationType(final Feature feature) {
		return feature.getChildren().stream().anyMatch(group -> (group.GROUPTYPE.equals(GroupType.ALTERNATIVE)
				|| group.GROUPTYPE.equals(GroupType.OR) || group.GROUPTYPE.equals(GroupType.GROUP_CARDINALITY)));
	}

	/**
	 * checks if the passed feature has the Abstract-attribute.
	 *
	 * @param feature the feature to check
	 * @return {@code true} if the given feature model is abstract, otherwise
	 *         {@code false}.
	 */
	public static boolean isAbstract(final Feature feature) {
		return feature.getAttributes().get(ABSTRACT_ATTRIBUTE) != null
				&& Boolean.parseBoolean(feature.getAttributes().get(ABSTRACT_ATTRIBUTE).getValue().toString());
	}

	/**
	 * adds or removes the Abstract-attribute to/from the feature
	 *
	 * @param feature    the feature to add/remove the Abstract-attribute from.
	 * @param isAbstract specifies if the Abstract-attribute is added or removed.
	 */
	public static void setAbstract(final Feature feature, final boolean isAbstract) {
		if (isAbstract) {
			feature.getAttributes().put(ABSTRACT_ATTRIBUTE, new Attribute<>(ABSTRACT_ATTRIBUTE, Boolean.TRUE));
		} else {
			feature.getAttributes().remove(ABSTRACT_ATTRIBUTE);
		}
	}

	/**
	 * Returns {@code true} if the given feature contains an attribute with the
	 * given name, otherwise {@code false}.
	 *
	 * @param feature                      the feature to search for the attribute.
	 * @param cardinalityConcernIdentifier the attribute name to search for.
	 * @return {@code true} if the given feature contains an attribute with the
	 *         given name, otherwise {@code false}.
	 */
	public static boolean containsAttribute(final Feature feature, final String cardinalityConcernIdentifier) {
		return Objects.requireNonNull(feature).getAttributes().containsKey(cardinalityConcernIdentifier);
	}

	/**
	 * checks if the passed feature model is an extended feature model. If the given
	 * feature model does not specify a root feature, the method returns
	 * {@code false} {@link #hasRoot(FeatureModel)}.
	 *
	 * @param fm the feature model to check
	 * @return {@code true} if the given feature model is an extended feature model,
	 *         otherwise {@code false}.
	 */
	public static boolean isExtendedFeatureModel(final FeatureModel fm) {
		if (!hasRoot(fm)) {
			return false;
		}
		return getRoot(fm).getAttributes().get(DefaultModelTransformationProperties.EXTENDED_FEATURE_MODEL) != null
				&& Boolean.parseBoolean(getRoot(fm).getAttributes().get(ABSTRACT_ATTRIBUTE).getValue().toString());
	}

	/**
	 * adds or removes the Extended-attribute to/from the root feature of the
	 * feature model. If the given feature model does not specify a root feature,
	 * the method executes without any effect {@link #hasRoot(FeatureModel)}.
	 *
	 * @param fm         the feature model to add/remove the Extended-attribute
	 *                   from.
	 * @param isExtended specifies if the Extended-attribute is added or removed.
	 */
	public static void setExtendedFeatureModel(final FeatureModel fm, final boolean isExtended) {
		if (hasRoot(fm)) {
			if (isExtended) {
				getRoot(fm).getAttributes().put(DefaultModelTransformationProperties.EXTENDED_FEATURE_MODEL,
						new Attribute<>(DefaultModelTransformationProperties.EXTENDED_FEATURE_MODEL, Boolean.TRUE));
			} else {
				getRoot(fm).getAttributes().remove(DefaultModelTransformationProperties.EXTENDED_FEATURE_MODEL);
			}
		}
	}

	/**
	 * checks if the passed feature has the Hidden-attribute
	 *
	 * @param feature the feature to check
	 * @return true hidden attribute is present, false otherwise.
	 */
	public static boolean isHidden(final Feature feature) {
		return feature.getAttributes().get(HIDDEN_ATTRIBUTE) != null
				&& Boolean.parseBoolean(feature.getAttributes().get(HIDDEN_ATTRIBUTE).getValue().toString());
	}

	/**
	 * adds or removes the Hidden-attribute to/from the feature
	 *
	 * @param feature    the feature to add/remove the Hidden-attribute from.
	 * @param isAbstract specifies if the Hidden-attribute is added or removed.
	 */
	public static void setHidden(final Feature feature, final boolean isHidden) {
		if (isHidden) {
			feature.getAttributes().put(HIDDEN_ATTRIBUTE, new Attribute<>(HIDDEN_ATTRIBUTE, Boolean.TRUE));
		} else {
			feature.getAttributes().remove(HIDDEN_ATTRIBUTE);
		}
	}

	/**
	 * returns true if the given feature is mandatory, otherwise false. Root
	 * features are mandatory by default. {@link #isRoot(Feature)}.
	 *
	 * @param feature the feature to be checked
	 * @return true if the feature is mandatory, otherwise false.
	 */
	public static boolean isMandatory(final Feature feature) {
		return isRoot(feature) || feature.getParentGroup().GROUPTYPE.equals(GroupType.MANDATORY);
	}

	/**
	 * get the value of a given attribute key
	 *
	 * @param feature       the feature from which to extract the attribute value
	 * @param attributeName the key for the attribute
	 * @return the value of the attribute
	 */
	public static Object getAttributeValue(final Feature feature, final String attributeName) {
		return feature.getAttributes().get(attributeName) == null ? null
				: feature.getAttributes().get(attributeName).getValue();
	}

	/**
	 * Adds an attribute to the given feature. The key-value pair is defined by the
	 * parameters <code>key</code> and <code>values</code>.
	 *
	 * @param feature the feature to add the attribute to.
	 * @param key     the key of the attribute.
	 * @param value   the value of the attribute
	 */
	public static <T> void addAttribute(final Feature feature, final String key, final T value) {
		Objects.requireNonNull(feature).getAttributes().put(key, new Attribute<>(key, value));
	}

	/**
	 * Adds an attribute to the given feature. The key-value pair is defined by the
	 * parameters <code>key</code> and <code>values</code>.
	 *
	 * @param <T>     the type of the attribute.
	 * @param feature the feature to add the attribute to.
	 * @param key     the key of the attribute.
	 * @param value   the value of the attribute
	 */
	public static <T> void addAttribute(final Feature feature, final String key, final Collection<T> values) {
		Objects.requireNonNull(feature).getAttributes().put(key, new Attribute<>(key, values));
	}

	/**
	 * get a list of all child features of a feature. This pools features from ALL
	 * groups
	 *
	 * @param feature the feature from which to get all children
	 * @return a List of all Features
	 */
	public static Set<Feature> getChildren(final Feature feature) {
		final Set<Feature> children = new HashSet<>();
		Objects.requireNonNull(feature).getChildren().forEach(group -> children.addAll(group.getFeatures()));
		return children;
	}

	/**
	 * checks if the given feature has any child features.
	 *
	 * @param feature the feature to check if it has children.
	 * @return a {@code true} if the given feature has children, otherwise
	 *         {@code false}.
	 */
	public static boolean hasChildren(final Feature feature) {
		return !getChildren(feature).isEmpty();
	}

	/**
	 * get a list of all child features from a given group type. This pools features
	 * from all groups of the given type.
	 *
	 * @param feature   the feature from which to get the children
	 * @param grouptype the group type from which the children are
	 * @return a Set of all features of the given group type
	 */
	public static Set<Feature> getChildren(final Feature feature, final Group.GroupType grouptype) {
		final Set<Feature> children = new HashSet<>();
		for (final Group group : feature.getChildren()) {
			if (group.GROUPTYPE.equals(grouptype)) {
				children.addAll(group.getFeatures());
			}
		}
		return children;
	}

	/**
	 * Checks if the given feature is contained within a group of a given type.
	 *
	 * @param feature   the feature to check
	 * @param groupType the assumed/proposed {@link Group.GroupType GrouptType}
	 * @return {@code true} if the given feature contains a group of the given type,
	 *         otherwise {@code false}.
	 */
	public static boolean checkGroupType(final Feature feature, final Group.GroupType groupType) {
		return hasParentFeature(feature)
				&& Objects.requireNonNull(feature).getParentGroup().GROUPTYPE.equals(groupType);
	}

	/**
	 * checks whether the constraint is complex. A constraint counts as complex if
	 * it's depth is greater than 2.
	 *
	 * @param constraint the constraint to check
	 * @return true if the constraint is complex, false otherwise
	 */
	public static boolean isComplexConstraint(final Constraint constraint) {
		final Formula f = TraVarTUtils.buildFormulaFromConstraint(Objects.requireNonNull(constraint), formulaFactory);
		return Objects.requireNonNull(f).stream().anyMatch(subf -> !subf.isAtomicFormula());
	}

	/**
	 * Detects if the given constraint is a Requires-Constraint. A constraint like
	 * this could be of the form A => B A => B | C !A | B | C
	 *
	 * @param constraint The constraint the check
	 * @return Boolean if constraint is requires-constraint
	 */
	public static boolean isRequires(final Constraint constraint) {
		final Formula formula = TraVarTUtils.buildFormulaFromConstraint(Objects.requireNonNull(constraint),
				formulaFactory);
		final Formula cnfFormula = formula.cnf();
		return cnfFormula instanceof Or && TraVarTUtils.countNegativeFormulaLiterals(cnfFormula) == 1
				&& TraVarTUtils.countPositiveFormulaLiterals(cnfFormula) > 0;
	}

	/**
	 * Detects if the given constraint is a RequiresForAll-Constraint. A constraint
	 * like this could be of the form (A & B) => C !A | !B | C
	 *
	 * @param constraint The constraint to check
	 * @return boolean if constraint is a RequiresForAll-constraint
	 */
	public static boolean isRequiredForAllConstraint(final Constraint constraint) {
		final Constraint constr = Objects.requireNonNull(constraint);
		final Formula formula = TraVarTUtils.buildFormulaFromConstraint(constr, formulaFactory);
		final Formula cnfFormula = formula.cnf();
		return cnfFormula instanceof Or && TraVarTUtils.countPositiveFormulaLiterals(cnfFormula) == 1
				&& TraVarTUtils.countNegativeFormulaLiterals(cnfFormula) > 1;
	}

	/**
	 * counts all positive literals in a given formula
	 *
	 * @param formula the formula to check
	 * @return the amount of positive literals as long
	 */
	public static int countPositiveFormulaLiterals(final Formula formula) {
		Objects.requireNonNull(formula);
		return countFormulaLiterals(formula, false);
	}

	/**
	 * counts all negative literals in a given formula
	 *
	 * @param formula the formula to check
	 * @return the amount of negative literals as long
	 */
	public static int countNegativeFormulaLiterals(final Formula formula) {
		Objects.requireNonNull(formula);
		return countFormulaLiterals(formula, true);
	}

	/**
	 * private helper method for counting positive and negative literals
	 *
	 * @param formula the formula to count literals of
	 * @param negated true to search for negated literals, false to check for
	 *                positive ones
	 * @return amount of found literals as long
	 */
	private static int countFormulaLiterals(final Formula formula, final boolean negated) {
		Objects.requireNonNull(formula);
		if (negated) {
			return (int) formula.literals().stream().filter(lit -> !lit.phase()).count();
		}

		return (int) formula.literals().stream().filter(Literal::phase).count();
	}

	/**
	 * Gets the first negative {@code Literal} that is within the {@code Constraint}
	 *
	 * @param constraint the constraint from which to get the first negated literal
	 *                   from
	 * @return The negated Literal as NotConstraint
	 */
	public static Constraint getFirstNegativeLiteral(final Constraint constraint) {
		Objects.requireNonNull(constraint);
		if (isNegativeLiteral(constraint)) {
			return constraint;
		}
		for (final Constraint child : constraint.getConstraintSubParts()) {
			final Constraint childsFirstNegative = getFirstNegativeLiteral(child);
			if (childsFirstNegative != null && isNegativeLiteral(childsFirstNegative)) {
				return childsFirstNegative;
			}
		}
		return null;
	}

	/**
	 * Gets the first positive Literal in a Constraint.
	 *
	 * @param constraint the constraint from which to get the literal from
	 * @return the first found positive literal
	 */
	public static Constraint getFirstPositiveLiteral(final Constraint constraint) {
		Objects.requireNonNull(constraint);
		if (isPositiveLiteral(constraint)) {
			return constraint;
		}
		for (final Constraint child : constraint.getConstraintSubParts()) {
			if (!isNegativeLiteral(child)) {
				final Constraint childsFirstPositive = getFirstPositiveLiteral(child);
				if (childsFirstPositive != null && isPositiveLiteral(childsFirstPositive)) {
					return childsFirstPositive;
				}
			}
		}
		return null;
	}

	/**
	 * checks if the constraint is an excludes constraint. If a constraint is
	 * deconstructed into a CNF form, and all literals are negative it is an
	 * excludes constraint. In all other cases it's not.
	 *
	 * @param constraint a constraint of arbitrary form.
	 * @return true if constraint is an exludes constraint, false otherwise
	 */
	public static boolean isExcludes(final Constraint constraint) {
		Objects.requireNonNull(constraint);
		Formula formula = buildFormulaFromConstraint(constraint, formulaFactory);
		formula = formula.cnf();
		final List<Literal> positiveLiterals = formula.literals().stream().filter(Literal::phase)
				.collect(Collectors.toList());
		final List<Literal> negativeLiterals = formula.literals().stream().filter(lit -> !lit.phase())
				.collect(Collectors.toList());
		return formula.type().equals(FType.OR) && positiveLiterals.isEmpty() && !negativeLiterals.isEmpty();
	}

	/**
	 * Checks whether the passed constraint is a negated Literal
	 *
	 * @param constraint the constraint to check
	 * @return true if given constraint is a negated Literal, else false
	 */
	public static boolean isNegativeLiteral(final Constraint constraint) {
		Objects.requireNonNull(constraint);
		return constraint instanceof NotConstraint
				&& ((NotConstraint) constraint).getContent() instanceof LiteralConstraint;
	}

	/**
	 * Checks whether the passed constraint is a Literal
	 *
	 * @param constraint the constraint to check
	 * @return true if given constraint is a Literal, else false
	 */
	public static boolean isPositiveLiteral(final Constraint constraint) {
		Objects.requireNonNull(constraint);
		return constraint instanceof LiteralConstraint;
	}

	/**
	 * How deep does the rabbit hole go? Checks the depth of the given constraint.
	 *
	 * @param constraint the constraint to check
	 * @return the highest depth of the constraint as long
	 */
	public static int getMaxDepth(final Constraint constraint) {
		Objects.requireNonNull(constraint);
		int count = 1;
		for (final Constraint child : constraint.getConstraintSubParts()) {
			final int childCount = getMaxDepth(child) + 1;
			if (childCount > count) {
				count = childCount;
			}
		}
		return count;
	}

	/**
	 * if the given constraint can have a right sub-constraint, it returns
	 * it.Otherwise null.
	 *
	 * @param constraint the constraint from which to get the right part of
	 * @return the right sub-constraint
	 * @throws ReflectiveOperationException
	 */
	public static Constraint getRightConstraint(final Constraint constraint) throws ReflectiveOperationException {
		Objects.requireNonNull(constraint);
		final List<Method> methods = Arrays.asList(constraint.getClass().getMethods());
		final Optional<Method> getRightMethod = methods.stream().filter(m -> m.getName().equals("getRight")).findAny();
		if (getRightMethod.isPresent()) {
			try {
				return (Constraint) getRightMethod.get().invoke(constraint);
			} catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new ReflectiveOperationException("Passed constraint does not have a getRight-method.", e);
			}
		}
		return null;
	}

	/**
	 * if the given constraint can have a left sub-constraint, it returns it.
	 * Otherwise null.
	 *
	 * @param constraint the constraint from which to get the left part of
	 * @return the left sub-constraint
	 * @throws ReflectiveOperationException
	 */
	public static Constraint getLeftConstraint(final Constraint constraint) throws ReflectiveOperationException {
		Objects.requireNonNull(constraint);
		final List<Method> methods = Arrays.asList(constraint.getClass().getMethods());
		final Optional<Method> getLeftMethod = methods.stream().filter(m -> m.getName().equals("getLeft")).findAny();
		if (getLeftMethod.isPresent()) {
			try {
				return (Constraint) getLeftMethod.get().invoke(constraint);
			} catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new ReflectiveOperationException("Passed constraint does not have a getLeft-method.", e);
			}
		}
		return null;
	}

	/**
	 * Finds all features in a features that do not have a parent are therefore
	 * roots.
	 *
	 * @param features Collection containing all features of an UVL model
	 * @return A list of features that don't have a parent
	 */
	private static List<Feature> findRoots(final Collection<Feature> features) {
		Objects.requireNonNull(features);
		// return all features with no parent
		return features.stream().filter(f -> !TraVarTUtils.hasParentFeature(f)).collect(Collectors.toList());
	}

	/**
	 * Returns a valid feature tree for an UVL model. If the feature model has more
	 * than one root, they are united under one single virtual root.
	 *
	 * @param fm       The feature model to add a root
	 * @param rootName Name of the artificial root
	 * @return name of the root feature
	 */
	public static String deriveFeatureModelRoot(final FeatureModel fm, final String rootName) {
		Objects.requireNonNull(fm);
		Objects.requireNonNull(rootName);
		final List<Feature> roots = findRoots(TraVarTUtils.getFeatures(fm));
		if (roots.isEmpty()) {
			return null;
		}
		if (roots.size() > 1) {
			// artificial root - abstract and hidden
			final Feature artificialRoot = new Feature(rootName);
			artificialRoot.getAttributes().put(ABSTRACT_ATTRIBUTE, new Attribute<>(ABSTRACT_ATTRIBUTE, true));
			artificialRoot.getAttributes().put("hidden", new Attribute<>("hidden", true));
			// add property to identify the virtual root
			artificialRoot.getAttributes().put("ARTIFICIAL_MODEL_NAME", new Attribute<>("ARTIFICIAL_MODEL_NAME",
					DefaultModelTransformationProperties.ARTIFICIAL_MODEL_NAME));
			final Group mandatoryGroup = new Group(GroupType.MANDATORY);
			mandatoryGroup.getFeatures().addAll(roots);
			artificialRoot.addChildren(mandatoryGroup);
			TraVarTUtils.setRoot(fm, artificialRoot);
			return rootName;
		}
		final Feature root = roots.get(0);
		TraVarTUtils.setRoot(fm, root);
		return root.getFeatureName();
	}

	/**
	 * Tests if the given constraint is a constraint that requires a single
	 * constraint for a single other one
	 *
	 * @param constraint the constraint to thest
	 * @return boolean signaling if the constraint is a single feature that requires
	 *         another.
	 */
	public static boolean isSingleFeatureRequires(final Constraint constraint) {
		Objects.requireNonNull(constraint);
		final Formula formula = TraVarTUtils.buildFormulaFromConstraint(constraint, formulaFactory);
		return formula instanceof Or && countNegativeFormulaLiterals(formula) == 1
				&& countPositiveFormulaLiterals(formula) == 1;
	}

	/**
	 * Checks whether the passed constraint is a excludes constraint where exactly
	 * two features exclude each other. The constraint can have one of these two
	 * forms: 1. !A | !B 2. A => !B
	 *
	 * @param constraint the constraint to be checked
	 * @return true if the constraint has one of the given structures, else false
	 */
	public static boolean isSingleFeatureExcludes(final Constraint constraint) {
		Objects.requireNonNull(constraint);
		if (constraint instanceof OrConstraint) {
			final OrConstraint orConstraint = (OrConstraint) constraint;
			return isNegativeLiteral(orConstraint.getLeft()) && isNegativeLiteral(orConstraint.getRight());
		}
		if (constraint instanceof ImplicationConstraint) {
			final ImplicationConstraint implConstraint = (ImplicationConstraint) constraint;
			return isPositiveLiteral(implConstraint.getLeft()) && isNegativeLiteral(implConstraint.getRight());
		}
		return false;
	}

	/**
	 * Checks whether a negated literal exists in the constraint
	 *
	 * @param constraint the constraint to check
	 * @return true if there is a negated literal, else false
	 */
	public static boolean hasNegativeLiteral(final Constraint constraint) {
		Objects.requireNonNull(constraint);
		return countNegativeLiterals(constraint) > 0;
	}

	/**
	 * Counts the amount of negative literals within a constraint
	 *
	 * @param constraint the constraint to inspect
	 * @return the amount of found negative literals as long
	 */
	public static int countNegativeLiterals(final Constraint constraint) {
		Objects.requireNonNull(constraint);
		final Formula formula = buildFormulaFromConstraint(constraint, formulaFactory);
		return countNegativeFormulaLiterals(formula);
	}

	/**
	 * Checks whether a positive literal exists in the constraint
	 *
	 * @param constraint the constraint to check
	 * @return true if there is a positive literal, else false
	 */
	public static boolean hasPositiveLiteral(final Constraint constraint) {
		Objects.requireNonNull(constraint);
		return countPositiveLiterals(constraint) > 0;
	}

	/**
	 * Counts the amount of positive literals within a constraint
	 *
	 * @param constraint the constraint to inspect
	 * @return the amount of found positive literals as long
	 */
	public static int countPositiveLiterals(final Constraint constraint) {
		Objects.requireNonNull(constraint);
		final Formula formula = buildFormulaFromConstraint(constraint, formulaFactory);
		return countPositiveFormulaLiterals(formula);
	}

	/**
	 * get all contained negative literals within a constraint as a {@link Set Set}
	 *
	 * @param constraint the constraint from which to get the literals
	 * @return all negated Literals within the constraint as a {@link Set Set}
	 */
	public static Set<Constraint> getNegativeLiterals(final Constraint constraint) {
		Objects.requireNonNull(constraint);
		final Set<Constraint> literals = new HashSet<>();
		if (constraint instanceof NotConstraint
				&& ((NotConstraint) constraint).getContent() instanceof LiteralConstraint) {
			literals.add(((NotConstraint) constraint).getContent());
			return literals;
		}
		if (constraint instanceof ParenthesisConstraint) {
			literals.addAll(getNegativeLiterals(((ParenthesisConstraint) constraint).getContent()));
		}
		for (final Constraint subConst : constraint.getConstraintSubParts()) {
			literals.addAll(getNegativeLiterals(subConst));
		}

		return literals;
	}

	/**
	 * get all contained literals within a constraint as a {@link Set Set}
	 *
	 * @param constraint the constraint from which to get the literals
	 * @return all Literals within the constraint as a {@link Set Set}
	 */
	public static Set<Constraint> getLiterals(final Constraint constraint) {
		Objects.requireNonNull(constraint);
		final Set<Constraint> literals = new HashSet<>();
		if (constraint instanceof LiteralConstraint) {
			literals.add(constraint);
			return literals;
		}
		if (constraint instanceof ParenthesisConstraint) {
			literals.addAll(getLiterals(((ParenthesisConstraint) constraint).getContent()));
		}
		for (final Constraint subConst : constraint.getConstraintSubParts()) {
			literals.addAll(getLiterals(subConst));
		}
		return literals;
	}

	/**
	 * Checks whether the given constraint is a literal. Also includes negated ones.
	 *
	 * @param constraint the constraint to check
	 * @return true if a (negated) Literal, else false
	 */
	public static boolean isLiteral(final Constraint constraint) {
		Objects.requireNonNull(constraint);
		return constraint instanceof LiteralConstraint || isNegativeLiteral(constraint);
	}

	/**
	 * Returns a set of {@link LiteralConstraint}s of all positive, i.e., not
	 * negated, literals in the given constraints.
	 *
	 * @param constraint the constraint to be checked
	 * @return a set of {@link LiteralConstraint}s of all positive, i.e., not
	 *         negated, literals in the given constraints
	 */
	public static Set<Constraint> getPositiveLiterals(final Constraint constraint) {
		Objects.requireNonNull(constraint);
		final Set<Constraint> literals = new HashSet<>();
		if (constraint instanceof LiteralConstraint) {
			literals.add(constraint);
			return literals;
		}
		if (constraint instanceof ParenthesisConstraint) {
			literals.addAll(getPositiveLiterals(((ParenthesisConstraint) constraint).getContent()));
		}
		for (final Constraint subConst : constraint.getConstraintSubParts()) {
			literals.addAll(getPositiveLiterals(subConst));
		}
		return literals;
	}

	/**
	 * Checks if the given feature is in the specified group of the parent feature.
	 * The method returns false if the feature is a root feature
	 * {@link #isRoot(Feature)}.
	 *
	 * @param feature   the feature to check.
	 * @param grouptype the grouptype in which the feature should be in the parent
	 *                  feature.
	 * @return true if the feature is in a group of the parent feature, otherwise
	 *         false.
	 */
	public static boolean isInGroup(final Feature feature, final Group.GroupType grouptype) {
		return !isRoot(Objects.requireNonNull(feature)) && feature.getParentGroup().GROUPTYPE.equals(grouptype);
	}

	/**
	 * Moves the given feature from one Group to a group of specified type under the
	 * given parent feature.
	 *
	 * @param fm        the feature model containing all features
	 * @param feature   the feature to move
	 * @param parent    the new parent feature
	 * @param groupType the {@link Group.GroupType GroupType} to add feature to
	 */
	public static void setGroup(final FeatureModel fm, final Feature feature, final Feature parent,
			final GroupType groupType) {
		Objects.requireNonNull(fm);
		Objects.requireNonNull(feature);
		Objects.requireNonNull(parent);
		if (feature.getParentGroup() != null) {
			feature.getParentGroup().getFeatures().remove(feature);
			// NPE here: When the feature is removed from its group, getParentGroup returns null
			// Short-circuited to bypass exception
			if (false && feature.getParentGroup().getFeatures().isEmpty()) {
				getParentFeature(feature).getChildren().remove(feature.getParentGroup());
			}
		}
		final Optional<Group> optGroup = parent.getChildren().stream().filter(g -> g.GROUPTYPE.equals(groupType))
				.findFirst();
		Group group = null;
		if (optGroup.isPresent()) {
			group = optGroup.get();
		} else {
			group = new Group(groupType);
			group.setParentFeature(parent);
			parent.addChildren(group);
		}
		group.getFeatures().add(feature);
		feature.setParentGroup(group);
		TraVarTUtils.addFeature(fm, parent);
		TraVarTUtils.addFeature(fm, feature);
		// Ensure that features do not disappear after being added to a group
		assert (getFeatureMapFromRoot(fm.getRootFeature()).values().contains(feature));
	}

	/**
	 * Adds a feature to the given group grouptype of the parent feature. If no such
	 * group is available yet, the group is created.
	 *
	 * @param fm        the feature model to work on.
	 * @param feature   the feature which should be added to the group.
	 * @param parent    the parent feature to which the group is added.
	 * @param groupType the type of the group.
	 */
	public static void addToGroup(final FeatureModel fm, final Feature feature, final Feature parent,
			final GroupType groupType, final int index) {
		Group group = getGroup(parent, groupType, index);
		if (group == null) {
			// Dead code? getGroup never returns null!
			group = new Group(groupType);
			parent.addChildren(group);
		}
		group.getFeatures().add(feature);
		TraVarTUtils.addFeature(fm, parent);
	}

	/**
	 * Returns {@code true} if the given feature specifies a group of type
	 * grouptype.
	 *
	 * @param feature   the feature to check.
	 * @param groupType the grouptype to search for in the feature.
	 * @return {@code true} if the given feature specifies a group of type
	 *         grouptype. otherwise {@code false}.
	 */
	public static boolean hasGroup(final Feature feature, final GroupType groupType) {
		return Objects.requireNonNull(feature).getChildren().stream().anyMatch(g -> groupType.equals(g.GROUPTYPE));
	}

	/**
	 * Returns the number of groups the given feature specifies of the given group
	 * type.
	 *
	 * @param feature   the feature to check.
	 * @param groupType the grouptype to search for in the feature.
	 * @return the number of instances of the given group type in the feature
	 */
	public static int countGroup(final Feature feature, final GroupType groupType) {
		// Safe cast, as the size of a list is limited by Integer.MAX_VALUE
		return (int) Objects.requireNonNull(feature).getChildren().stream().filter(g -> groupType.equals(g.GROUPTYPE))
				.count();
	}

	/**
	 * Returns a group of type groupType from the given feature. If the feature
	 * specifies multiple groups of the given type, the one with the given index
	 * returned. If the feature does not specify a group of the given type, the
	 * method creates one and returns it.
	 * @see{{@link #hasGroup(Feature, GroupType)}}.
	 *
	 * @param feature   the feature to return the group of type grouptype from.
	 * @param groupType the groupType to search for in the feature.
	 * @return a group of type groupType, either from the given feature or a new
	 *         one.
	 */
	public static Group getGroup(final Feature feature, final GroupType groupType, final int index) {
		final List<Group> groups = feature.getChildren().stream().filter(g -> groupType.equals(g.GROUPTYPE))
				.collect(Collectors.toList());
		if (!groups.isEmpty()) {
			return groups.get(index);
		}
		return null;
	}
	
	// Allow index-less getting of groups, especially right after using addGroup
	public static Group getLastGroup(final Feature feature, final GroupType groupType) {
		final List<Group> groups = feature.getChildren().stream().filter(g -> groupType.equals(g.GROUPTYPE))
				.collect(Collectors.toList());
		if (!groups.isEmpty()) {
			return groups.getLast();
		}
		return null;
	}

	// TODO Add method that returns all groups of a specific type

	/**
	 * Adds a new group to the parent feature of type groupType, which contains the
	 * features.
	 *
	 * @param fm        the feature model to work on.
	 * @param features  the features which should be added to the group.
	 * @param parent    the parent feature to which the group is added.
	 * @param groupType the type of the group.
	 */
	// Return the index of the newly added group?
	public static int addGroup(final FeatureModel fm, final Collection<Feature> features, final Feature parent,
			final GroupType groupType) {
		// Current max index for given groupType
		final int index = TraVarTUtils.countGroup(parent, groupType);
		final Feature p = Objects.requireNonNull(parent);
		final Group group = new Group(groupType);
		group.getFeatures().addAll(features);
		p.addChildren(group);
		TraVarTUtils.addFeature(fm, parent);
		return index + 1;
	}

	/**
	 * Adds a new group to the parent feature of type grouptype.
	 *
	 * @param fm        the feature model to work on.
	 * @param parent    the parent feature to which the group is added.
	 * @param groupType the type of the group.
	 */
	public static void addGroup(final FeatureModel fm, final Feature parent, final GroupType groupType) {
		final Feature p = Objects.requireNonNull(parent);
		final Group group = new Group(groupType);
		p.addChildren(group);
		TraVarTUtils.addFeature(fm, parent);
	}

	/**
	 * Gets the logger for the particular class with a log file name
	 *
	 * @param className   the name of the class for which the logger is needed
	 * @param logFileName the name of the log file by which it is supposed to be
	 *                    saved
	 */
	public static Logger getSimpleLogger(final String className, final String logFileName)
			throws SecurityException, IOException {
		final Logger logger = Logger.getLogger(className);
		final FileHandler fh = new FileHandler(String.format("%s.log", logFileName));
		fh.setFormatter(new SimpleFormatter());
		logger.addHandler(fh);
		return logger;
	}
	
}