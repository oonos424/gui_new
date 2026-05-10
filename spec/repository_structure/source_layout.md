# Source Layout

## Overview

All Java source lives under `src/main/java/` and is rooted at the `affr` top-level package.
Resources (FXML, CSS, property bundles) live under `src/main/resources/` mirroring the same
package paths.

---

## Java Package Hierarchy

```
affr/
├── app/                    ← JavaFX application layer
│   ├── AFFrMain            Entry point (Application subclass)
│   ├── AppConfig           Immutable CLI-argument record
│   ├── NavigationService   Loads screens; owns Stage transitions
│   └── top/
│       └── TopController   FXML controller for the Project Browser screen
│
├── fx/                     ← JavaFX ViewModel layer (no widget imports)
│   └── viewmodel/
│       └── top/
│           ├── TopCategory  Enum of top-level navigation modes
│           └── TopViewModel Observable categories + selected-category property
│
├── project/                ← Domain model for projects and their items
│   ├── ProjectItem         Sealed interface for all item types inside a project
│   ├── AFFrProject         Domain object for a project (owns ObservableList<ProjectItem>)
│   ├── AFFrCalculation     First concrete ProjectItem — a single CFD calculation
│   ├── AFFrCalProperty     Persistent calculation metadata (persisted in .affr_property)
│   ├── AFFrCalculationModel Physics model selection (persisted in .mode)
│   ├── CalculationStatus   Enum of calculation lifecycle states
│   ├── ComprsModel         Enum: Compressible | Incompressible
│   ├── SteadyModel         Enum: Steady | Unsteady
│   ├── TurbModel           Enum: LES | RANS | DNS | NO
│   ├── ExtraModel          Enum of optional physics extensions (VOF, Cavitation, …)
│   └── ProjectLoader       Loads an AFFrProject and its items from disk
│
└── util/                   ← Shared utilities (no JavaFX scene-graph imports)
    ├── i18n/
    │   └── I18n            ResourceBundle accessor; observable locale property
    └── prefs/
        └── UserPreferences Reads/writes ~/.affr/preferences.properties
```

---

## Layering Rules

Dependencies flow **downward only**:

```
affr.app  →  affr.fx  →  affr.project  →  affr.util  →  (JDK / JavaFX observable APIs only)
```

| Layer | May import from | Must NOT import from |
|---|---|---|
| `affr.app` | `affr.fx`, `affr.project`, `affr.util`, JDK, JavaFX | — |
| `affr.fx` | `affr.project`, `affr.util`, JDK, JavaFX | `affr.app` |
| `affr.project` | `affr.util`, JDK, JavaFX observable APIs | `affr.app`, `affr.fx` |
| `affr.util` | JDK, JavaFX observable APIs | `affr.app`, `affr.fx`, `affr.project` |

`affr.util` sub-packages may use `javafx.beans.property` (observable values) because these are
pure data-binding primitives with no scene-graph dependency. They must not reference
`javafx.scene.*` or any other scene-graph type.

---

## Resources

Resource files mirror the Java package path under `src/main/resources/`:

| Resource | Path |
|---|---|
| Top screen FXML | `src/main/resources/affr/app/top/TopController.fxml` |
| Top screen CSS | `src/main/resources/affr/app/top/top.css` |
| i18n bundles | `src/main/resources/affr/util/i18n/messages.properties` |
|               | `src/main/resources/affr/util/i18n/messages_ja.properties` |

---

## Application Home Directory

The application stores per-user data under `~/.affr/`:

| File | Purpose |
|---|---|
| `~/.affr/preferences.properties` | User preferences (language, future settings) |

The `UserPreferences` class (`affr.util.prefs`) owns all reads and writes to this directory.
`UserPreferences.APP_DIR` is the single source of truth for the path.

---

## Design Decisions

### `ProjectItem` — sealed extension point for project content

`ProjectItem` is a **sealed interface** that represents a single unit of work inside a project.
All concrete item types are listed in `permits`; the compiler enforces exhaustive handling at every
`switch` site.

`ProjectItem` declares three universal accessors that all item types must supply:

| Method | Meaning |
|---|---|
| `name()` | Display name (e.g. `"cal_01"`) |
| `path()` | Absolute path to the item's directory on disk |
| `date()` | ISO-8601 last-modification date; empty string if unknown |

`date()` is a universal contract because every item type has a meaningful last-modification date
(written when the item is saved or a run completes), and this field drives the default sort order
in the item list.

**Current permitted types:**

| Type | Description |
|---|---|
| `AFFrCalculation` | A single CFD calculation (mesh + settings + execution history) |

**Anticipated future types** (not yet implemented; each will require a new `permits` entry,
a marker-file check in `ProjectLoader`, and a new `case` in every switch over `ProjectItem`):

| Type | Description |
|---|---|
| `MeshGeneratorItem` | A mesh-generation job |
| `OptimizerItem` | An optimisation study |
| `SurrogateModelItem` | A surrogate modelling task |
| `ResultAnalysisItem` | A result post-processing or analysis job |
