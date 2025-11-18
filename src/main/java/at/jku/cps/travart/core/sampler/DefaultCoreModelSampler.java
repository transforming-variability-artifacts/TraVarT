/*******************************************************************************
 * This Source Code Form is subject to the terms of the Mozilla
 * Public License, v. 2.0. If a copy of the MPL was not distributed
 * with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 *
 * Contributors:
 *     @author Kevin Feichtinger
 *
 * Base implementation for sampling the core model.
 *
 * Copyright 2023 Johannes Kepler University Linz
 * LIT Cyber-Physical Systems Lab
 * All rights reserved
 *******************************************************************************/
package at.jku.cps.travart.core.sampler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import at.jku.cps.travart.core.common.IConfigurable;
import at.jku.cps.travart.core.common.ISampler;
import at.jku.cps.travart.core.exception.NotSupportedVariabilityTypeException;
import de.ovgu.featureide.fm.core.analysis.cnf.LiteralSet;
import de.ovgu.featureide.fm.core.analysis.cnf.formula.FeatureModelFormula;
import de.ovgu.featureide.fm.core.analysis.cnf.generator.configuration.twise.TWiseConfigurationGenerator;
import de.ovgu.featureide.fm.core.base.FeatureUtils;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.impl.DefaultFeatureModelFactory;
import de.ovgu.featureide.fm.core.base.impl.MultiFeatureModelFactory;
import de.ovgu.featureide.fm.core.configuration.Configuration;
import de.ovgu.featureide.fm.core.configuration.ConfigurationPropagator;
import de.ovgu.featureide.fm.core.configuration.Selection;
import de.ovgu.featureide.fm.core.init.LibraryManager;
import de.ovgu.featureide.fm.core.io.uvl.UVLFeatureModelFormat;
import de.ovgu.featureide.fm.core.job.LongRunningWrapper;
import de.vill.model.FeatureModel;

public class DefaultCoreModelSampler implements ISampler<FeatureModel> {

	static {
		LibraryManager.registerLibrary(DefaultLibrary.getInstance());
	}

	private static final int INVALID_COUNT = 10;

	private FeatureModel lastFm;

	private Set<Map<IConfigurable, Boolean>> samples;
	private Set<Map<IConfigurable, Boolean>> invalidSamples;

	private final Random rand = new Random();

	@Override
	public Set<Map<IConfigurable, Boolean>> sampleValidConfigurations(final FeatureModel fm)
			throws NotSupportedVariabilityTypeException {
		if (lastFm != fm) {
			init(fm);
		}
		return samples;
	}

	@Override
	public Set<Map<IConfigurable, Boolean>> sampleValidConfigurations(final FeatureModel fm, final long maxNumber)
			throws NotSupportedVariabilityTypeException {
		if (lastFm != fm) {
			init(fm);
		}
		// return only the first maxNumber elements if necessary
		return samples.size() > maxNumber ? samples.stream().limit(maxNumber).collect(Collectors.toSet()) : samples;
	}

	@Override
	public Set<Map<IConfigurable, Boolean>> sampleInvalidConfigurations(final FeatureModel fm)
			throws NotSupportedVariabilityTypeException {
		if (lastFm != fm) {
			init(fm);
		}
		return invalidSamples;
	}

	@Override
	public Set<Map<IConfigurable, Boolean>> sampleInvalidConfigurations(final FeatureModel fm, final long maxNumber)
			throws NotSupportedVariabilityTypeException {
		if (lastFm != fm) {
			init(fm);
		}
		return invalidSamples.size() > maxNumber ? invalidSamples.stream().limit(maxNumber).collect(Collectors.toSet())
				: invalidSamples;
	}

	private void init(final FeatureModel fm) {
		IFeatureModel featureIdeFm = new MultiFeatureModelFactory().create();
		toFeatureIdeFm(fm, featureIdeFm);
		samples = sample(featureIdeFm);
		invalidSamples = sampleInvalid(featureIdeFm, samples);
	}

	private void toFeatureIdeFm(final FeatureModel fm, final IFeatureModel featureIdeFm) {
		UVLFeatureModelFormat format = new UVLFeatureModelFormat();
		format.read(featureIdeFm, fm.toString());
	}

	private Set<Map<IConfigurable, Boolean>> sample(final IFeatureModel fm) {
		List<List<String>> configurations = findConfigurations(fm);
		Set<Map<IConfigurable, Boolean>> configurables = new HashSet<>();
		for (final List<String> fmSample : configurations) {
			Map<IConfigurable, Boolean> config = new HashMap<>();
			for (String selectedFeature : fmSample) {
				IConfigurable configurable = new IConfigurable() {
					boolean configured;

					@Override
					public void setConfigured(final boolean selected) {
						this.configured = selected;
					}

					@Override
					public boolean isConfigured() {
						return configured;
					}

					@Override
					public String getName() {
						return selectedFeature;
					}
				};
				configurable.setConfigured(true);
				config.put(configurable, true);
			}
			configurables.add(config);
		}
		return configurables;
	}

	private List<List<String>> findConfigurations(final IFeatureModel fm) {
		FeatureModelFormula formula = new FeatureModelFormula(fm);
		List<LiteralSet> configs = LongRunningWrapper.runMethod(new TWiseConfigurationGenerator(formula.getCNF(), 3));
		List<List<String>> configurations = new ArrayList<>(configs.size());
		for (LiteralSet sample : configs) {
			List<String> names = formula.getCNF().getVariables().convertToString(sample);
			configurations.add(names);
		}
		return configurations;
	}

	private Set<Map<IConfigurable, Boolean>> sampleInvalid(final IFeatureModel fm,
			final Set<Map<IConfigurable, Boolean>> samples) {
		invalidSamples = new HashSet<>();
		for (Map<IConfigurable, Boolean> sample : samples) {
			for (int count = 0; count < INVALID_COUNT; count++) {
				int featureSwitch = rand.nextInt(sample.size());
				Map<IConfigurable, Boolean> invalid = new HashMap<>();
				int i = 0;
				for (Entry<IConfigurable, Boolean> entry : sample.entrySet()) {
					IConfigurable key = entry.getKey();
					Boolean value = entry.getValue();
					if (i == featureSwitch) {
						key.setConfigured(!key.isConfigured());
						value = !value;
					}
					invalid.put(key, value);
					i++;
				}
				// the randomly changed configuration my be valid so do not add it to the
				// invalid samples
				if (!verifySampleAsFeatureIde(fm, invalid)) {
					invalidSamples.add(invalid);
				}
			}
		}
		return invalidSamples;
	}

	private boolean verifySampleAsFeatureIde(final IFeatureModel fm, final Map<IConfigurable, Boolean> sample) {
		FeatureModelFormula formula = new FeatureModelFormula(fm);
		Configuration config = new Configuration(formula);
		for (Entry<IConfigurable, Boolean> entry : sample.entrySet()) {
			IFeature feature = FeatureUtils.getFeature(fm, entry.getKey().getName());
			if (feature != null) {
				String featureName = FeatureUtils.getName(feature);
				if (Boolean.TRUE.equals(entry.getValue())) {
					config.setManual(featureName, Selection.SELECTED);
				} else {
					config.setManual(featureName, Selection.UNSELECTED);
				}
			}
		}
		ConfigurationPropagator prop = new ConfigurationPropagator(formula, config);
		return LongRunningWrapper.runMethod(prop.isValidNoHidden());
	}

	@Override
	public boolean verifySampleAs(final FeatureModel fm, final Map<IConfigurable, Boolean> sample)
			throws NotSupportedVariabilityTypeException {
		IFeatureModel featureIdeFm = new DefaultFeatureModelFactory().create();
		toFeatureIdeFm(fm, featureIdeFm);
		return verifySampleAsFeatureIde(featureIdeFm, sample);
	}

}
