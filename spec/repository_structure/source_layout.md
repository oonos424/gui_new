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
affr.app  →  affr.fx  →  affr.util  →  (JDK / JavaFX observable APIs only)
```

| Layer | May import from | Must NOT import from |
|---|---|---|
| `affr.app` | `affr.fx`, `affr.util`, JDK, JavaFX | — |
| `affr.fx` | `affr.util`, JDK, JavaFX | `affr.app` |
| `affr.util` | JDK, JavaFX observable APIs | `affr.app`, `affr.fx` |

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
