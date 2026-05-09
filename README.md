# AFFr GUI

A CFD desktop application built with Java 25 + JavaFX 25.

The full product spec (PRD, UI spec, data model, technical design) lives outside
this repository. See the project owner for its location. Project-scoped specs
live in `spec/`.

---

## Environment Setup

### 1. Install Liberica JDK 25

```sh
brew tap bell-sw/liberica
brew install bell-sw/liberica/liberica-jdk25-full
```

Liberica 25 is the authoritative JVM for this project. Do not use Coursier's
jabba index (`cs java --jvm liberica:25`); it does not include this version.

### 2. Install pre-commit hooks

```sh
./setup
```

This installs `uv` and configures pre-commit hooks. Hooks use `uv run` for
Python tools and `cs launch` for Java/Kotlin formatters.

---

## Key Commands

| Task | Command |
|---|---|
| Build and run | `./gradlew run` |
| Auto-format | `./gradlew spotlessApply` |
| Fast static checks | `./gradlew quickCheck` |
| Run tests | `./gradlew test` |

---

## Development Workflow

### 1. Implement

Work on the feature or fix. During development, use `./gradlew quickCheck` for
fast feedback (compile + static analysis, no full test run).

### 2. Confirm behaviour

Run the application (`./gradlew run`) and verify the feature works as intended.
Tests are not required at this stage — confirm behaviour first.

### 3. Prepare for PR

Once behaviour is confirmed, run the `/prepare-pr` Claude command. It will:

1. Fix formatting with `./gradlew spotlessApply`
2. Review and fill in missing test coverage
3. Run the full local CI sequence:
   - `./gradlew compileJava`
   - `./gradlew spotlessCheck`
   - `./gradlew nullCheck`
   - `./gradlew test`
4. Report results and confirm the branch is ready to push

### 4. Commit and open PR

Use `/commit` to stage and commit, then open a pull request. The PR CI
pipeline (`.github/workflows/pr-ci.yml`) runs the same checks as Step 3.

---

## CI Pipeline

The PR CI (`.github/workflows/pr-ci.yml`) runs on every non-draft pull request:

| Step | Command |
|---|---|
| Compile | `./gradlew compileJava` |
| Formatting | `./gradlew spotlessCheck` |
| Nullness | `./gradlew nullCheck` |
| Tests | `./gradlew test` |

All steps must pass before a PR can be merged.

---

## Claude Commands

| Command | Purpose |
|---|---|
| `/prepare-pr` | Fix formatting, add missing tests, run full CI checks locally |
| `/commit` | Stage changes, confirm commit message, then commit |
| `/worklog` | Record a WorkLog entry for the just-completed task |
