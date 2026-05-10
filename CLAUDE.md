# Agent Instructions

This file gives Claude Code (and other AI agents) the context needed to work
effectively in this repository.

---

## Project

AFFr GUI rewrite — a CFD desktop application built with Java 25 + JavaFX 25.
The external product spec lives outside this repository (ask the project owner
for its location). Project-scoped build and environment specs live in `spec/`
in this repository.

---

## Environment Setup

### Java

Install **Liberica JDK 25** via Homebrew before doing anything else:

```sh
brew tap bell-sw/liberica
brew install bell-sw/liberica/liberica-jdk25-full
```

Liberica 25 is **not in Coursier's jabba index** — do not use `cs java --jvm
liberica:25`. System Java (the Homebrew installation) is the authoritative JVM
for this project.

### Python / pre-commit hooks

Install `uv`, then run:

```sh
./setup
```

This installs pre-commit hooks. Hooks use `uv run` for Python tools and
`cs launch` (Coursier) for Java/Kotlin formatters. The `cs launch` invocations
do **not** pass `--jvm`; they rely on system Java.

---

## Key Commands

| Task | Command |
|---|---|
| Build and run | `./gradlew run` |
| Auto-format | `./gradlew spotlessApply` |
| Fast static checks | `./gradlew quickCheck` |
| Run tests | `./gradlew test` |

---

## Spec Documents

Project-scoped specs are in `spec/`:

| File | Covers |
|---|---|
| `spec/repository_structure/build.md` | Build system, dependencies, Java install, pre-commit hooks |

The full product spec (PRD, UI spec, data model, technical design) lives
outside this repository. Read it for domain and feature context; write
project-scoped decisions to `spec/`.

---

## Testing Workflow (GUI Development)

- Do not require full test implementation immediately after every small implementation task.
- Prioritize making the requested behavior work first; tests follow once behavior is confirmed.
- After each implementation, run lightweight checks where practical: `./gradlew quickCheck`, a build check, or note what was manually verified.
- If the user requests changes after reviewing the behavior, update the implementation before writing detailed tests.
- Add or update formal automated tests before preparing a PR or final handoff.
- At PR stage, ensure important logic, regressions, and non-trivial behavior are covered.
- Never skip tests entirely — defer detailed test creation until behavior is confirmed, not permanently.

---

## WorkLog

Record completed work using the `/worklog` slash command. Entries are appended
to `log/worklog/YYYY-MM.md`.
