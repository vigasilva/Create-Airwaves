# Create: Airwaves — reconstructed source

> Decompiled code from Create: Airwaves, analyzed and restructured by AI using Codex to make it human-readable and editable.

This is a community reconstruction made from the released `airwaves-1.21.1-0.1.1-atm10.jar`. It is not an official upstream source release and must not be represented as one.

The reconstruction preserves recovered class and method names where available, uses a normal NeoForge 1.21.1 Gradle layout, and keeps third-party development dependencies outside the Git repository.

## Build

Requirements: Java 21 and internet access on the first Gradle run. Compatible
development dependencies are resolved from public Maven repositories
automatically; no JARs need to be copied into `libs/`.

```bash
./gradlew build
```

The build output is written to `build/libs/`. It does not embed Create, Aeronautics, Sable, voice chat, JEI, or power-system dependencies.

## Licensing and attribution

The original JAR declares `CC BY-NC-SA 4.0`. This reconstructed derivative is kept under that same attribution/non-commercial/share-alike basis. Before any public release, preserve clear attribution to the original Create: Airwaves author and verify that the intended publication complies with the original license and the licenses of all included assets.

## Repository status

The reconstructed source and verified build are published on GitHub. The
release includes both the reconstructed artifact and the supplied original
archive, clearly labelled as separate files.
