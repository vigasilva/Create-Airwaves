# Local development dependencies

This directory is intentionally ignored by Git and is no longer required for a
normal build. `./gradlew build` resolves the compatible public development
dependencies declared in `build.gradle` automatically.

You may put experimental replacement JARs here for local testing, but they are
not read by the default build and are never copied into the output JAR.
