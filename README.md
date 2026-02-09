# TraVarT: Core library

This repository contains the TraVarT core module which can be extended with plugins. The core module itself only provides support
for UVL models, which is the pivot metamodel. Additionally, the core module has a CLI to invoke transformations, list benchmarks etc.

To get started, copy over TraVarT plugins (as JAR archives) into the `plugins` folder in this repository (if this folder does not
exist, create it, the path should be ignored by Git).

The command-line interface provides three subcommands: `plugin`, `benchmark` and `transform'. An exhaustive list of possible
options and flags for individual subcommands can be viewed by invoking the respective subcommand as a sub-subcommand of `help` i.e., `help plugin`.
If you want to start TraVarT directly off this repository (and not over a compiled JAR file independent from the repository), invoke
the Maven exection plugin over `mvn exec:java`. You can provide command-line arguments while using `exec:java` with `-Dexec.args`.

For clarity, we will shortly introduce the subcommands here:

- `plugin`: This subcommand lists currently detected plugins. There are no flags or sub-subcommands of this subcommand.
- `benchmark`: This subcommand lists currently known benchmarks. There are no flags or sub-subcommands of this subcommand.
- `transform`: This subcommand allows invocation of installed plugins to transform models. It has four mandatory parameters and several optional flags:
    - First command-line parameter should be the path to the source model (model to be transformed).
    - Second command-line parameter should be the path to the target model (this will be created by the end of the transformation).
    - Two mandatory flags are `-st` and `-tt` (or respectively `--source-type` and `--target-type`). These should correspond to the source and target model types, else transformation is not possible.
    - The flag `--benchmark` can be used to activate any number of benchmarks during transformation. The argument to this option should be comma-seperated list of benchmark names, as shown over the `benchmark` subcommand.
    - The flag `--write-benchmarks` can be used to write benchmark results automatically into some given file. The given file will be concatenated, the output format is CSV.
    - The flag `--blacklist-file` can be used to include some blacklist file while transforming. Blacklisted models (matching by name) won't be transformed.
    - The flag `--strategy` can be used to enforce a certain transformation strategy. By default, TraVarT attempts an one-way transformation. The two possible values here are `ONE\_WAY` or `ROUNDTRIP`.
    - The flag `--inplace-roundtrip` can be used alongside `--strategy=ROUNDTRIP`. In-place roundtrip transformation means that the forward transformation is immediately followed by a reverse transformation; i.e. the resulting model is in source type.
    - The flag `--no-serialize` can be used to skip serialization after transformation. This is especially useful if the user is only interested in benchmarking results. When serialization is skipped, the target model is not persisted.
    - The flag `--strict` can be used when working in batch mode (source path is a folder with multiple models). If in strict mode, transformation is aborted after the first timeout/transformation failure.
    - As just mentioned, TraVarT has a default transformation timeout; this is 5 seconds by default. It can be optionally changed to some arbitrary number of seconds over the `--timeout` option.

All subcommands also support the `--verbose` flag.

More regarding the architecture of TraVarT and related tools can be found in the publications regarding TraVarT.