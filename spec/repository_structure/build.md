# Build System

## Overview

The project uses **Gradle** with Kotlin DSL (`build.gradle.kts`). The build
file is the authoritative source for all dependency versions and toolchain
configuration.

---

## Language and Runtime

| Concern | Value |
|---|---|
| JVM language | Java 25 |
| JVM target | 25 (Kotlin targets 24; JVM 25 runs JVM 24 bytecode without issue) |
| JavaFX | 25 |
| Build tool | Gradle 9.0 (Kotlin DSL) |
| Kotlin | 2.2.0 |
| Python (Trame backend) | 3.12+ |

---

## Java Installation

The project targets **Liberica JDK 25**, installed via Homebrew:

```sh
brew tap bell-sw/liberica
brew install bell-sw/liberica/liberica-jdk25-full
```

Liberica 25 is **not available through Coursier** (the jabba index does not
carry it). Do not attempt `cs java --jvm liberica:25`.

---

## Pre-commit Hooks

Install hooks after cloning (requires `uv` on PATH):

```sh
./setup
```

Hooks defined in `.pre-commit-config.yaml`:

| Hook | Tool | Notes |
|---|---|---|
| `ruff-check` | ruff | Python lint + autofix |
| `ruff-format` | ruff | Python format |
| `google-java-format` | `cs launch` | Java format |
| `ktfmt` | `cs launch` | Kotlin format |
| `fxml-format` | `script/fxml.py` | FXML indent + version stamp |

### `cs launch` and `--jvm`

`google-java-format` and `ktfmt` are invoked via `cs launch` (Coursier) **without
a `--jvm` flag**. They use whatever `java` is on `PATH` — expected to be the
Homebrew-installed Liberica 25.

**Do not add `--jvm=...` to the `cs launch` args.** Coursier's jabba index does
not carry Liberica for Java 25, and Temurin 25 is not the target distribution.

---

## Code Formatting (Gradle)

Formatting is also enforced at build time by **Spotless**:

| Language | Formatter | Style |
|---|---|---|
| Java | Google Java Format | Google style |
| Kotlin | ktfmt | default |

Run `./gradlew spotlessApply` to auto-format.
Run `./gradlew quickCheck` for fast static checks (formatting only, no tests).

---

## Build Tool Notes

### Gradle 9.0

Gradle 9.0 is required because:

- Gradle 8.14.x bundles Kotlin 2.0.21 (released before Java 25), whose embedded IntelliJ
  `JavaVersion.parse()` cannot handle the `25.0.3` version string.
- Gradle 9.0 bundles Kotlin 2.2 which handles Java 25 version strings correctly.

### Shadow plugin

`com.github.johnrengelman.shadow` is intentionally **absent** from the plugins block. It is
not Gradle 9.0-compatible at version 8.1.1, and packaging is deferred per the project plan.
Add it back (with a Gradle-9-compatible version) when fat-JAR packaging is needed.

### Configuration cache

`org.gradle.configuration-cache=false` in `gradle.properties` until the Checker Framework
Gradle plugin and any other plugins are verified compatible with Gradle 9.0's configuration
cache requirements.

---

## Key Dependencies

### GUI framework

| Dependency | Source |
|---|---|
| JavaFX (base, graphics, controls, fxml, web) | `org.openjfx.javafxplugin` Gradle plugin |

`javafx.swing` is **excluded** — it was only needed for the Swing/VTK interop
bridge in the old project. `javafx.web` is required for the WebView that hosts
the Trame frontend.

### Core libraries

| Library | Purpose |
|---|---|
| `info.picocli:picocli` | CLI argument parsing |
| `io.reactivex.rxjava2:rxjava` + `rxjavafx` | Reactive event streams |
| `com.github.mwiede:jsch` | SSH/SFTP for remote execution |
| `com.zaxxer:nuprocess` | Non-blocking subprocess I/O |
| `net.objecthunter:exp4j` | Expression evaluation |
| `com.google.code.gson:gson` | JSON serialization |
| Monaco Editor (npm) | Embedded code editor for subroutines |

### Null safety

| Tool | Role |
|---|---|
| `org.checkerframework:checker` | Compile-time null checking (annotation processor) |
| `org.checkerframework:checker-qual` | `@Nullable`, `@NonNull` annotations |

### Dropped from reference project

| Library | Reason |
|---|---|
| VTK Java wrapper (`vtk.jar` + JNI DLLs) | Replaced by Python VTK + Trame subprocess |
| `javax.xml.bind` | No longer needed |
| Launch4j packaging tasks | Deferred; packaging not the immediate concern |
