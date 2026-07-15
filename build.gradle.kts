// Intentionally empty.
//
// This project is built with the Kotlin Toolchain (Amper engine), not Gradle.
// All dependency versions live in `gradle/libs.versions.toml`, which the
// Toolchain consumes natively as its `$libs.*` catalog.
//
// This file exists so that GitHub Dependabot's `package-ecosystem: gradle`
// integration discovers the project: Dependabot's detector requires either a
// `build.gradle(.kts)` or `settings.gradle(.kts)` at the configured directory
// before it will scan `gradle/libs.versions.toml` for updatable dependencies.
// See `.github/dependabot.yml`.
