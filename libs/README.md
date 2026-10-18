# Local development dependencies

This directory is intentionally ignored by Git.

To compile and launch the reconstruction locally, it must contain compatible
NeoForge 1.21.1 JARs for Create, Create: Aeronautics, Sable, Simple Voice Chat,
JEI, Create: Power Grid, Electro Energetics, and Sable Companion Common.

They are used only as development dependencies; `gradlew build` does not shade
or redistribute them in the output JAR.
