# Building

`sbt cli/Universal/packageBin`

The output will be in `cli/target/universal/` and will be a zip file.

# Publishing as an artifact

This is needed to publish the artifact locally for being used as a dependency in other projects.

`sbt library/publishLocal`