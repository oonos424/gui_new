---
description: Load product-spec context before the user describes a new task
---

# /from-spec — Load Spec Context

Run this at the start of a session when the upcoming task should be informed
by the product spec. This command only sets up context — the user will
describe the actual task afterwards.

## Step 1: Identify the Spec Section

If the user passed an argument, treat it as a path relative to the spec root
(e.g. `ui_spec/login.md`, `data_model/`, `PRD/`).

If no argument was given, list the top-level subdirectories of the spec root
(typically `PRD/`, `ui_spec/`, `data_model/`, `technical_design/`,
`concepts/`, `development_guidelines/`, `repository_structure/`) and ask the
user which area is relevant. Multiple selections are fine.

Argument: $ARGUMENTS

## Step 2: Resolve the Spec Root

The product spec location is recorded in personal Claude memory at
`spec_location.md`. Resolve the absolute path for the current OS from there.

- **Windows**: `G:\マイドライブ\Documents\AFFr_GUI\doc\specs\`. Always resolve
  `マイドライブ` via the wildcard approach in `spec_location.md` — shells
  mangle the kana when typed literally.
- **macOS / Linux**: if no path is recorded yet for this OS, ask the user and
  update `spec_location.md` with the answer before continuing.

If the spec root cannot be resolved, stop and tell the user.

## Step 3: Read the Material

Read the requested files. For directories, list contents first and read the
files the user points at — do not bulk-read entire trees.

Summarize back in 3–5 bullets so the user can confirm you loaded the right
context (key concepts, relevant constraints, anything surprising).

## Step 4: Await the Task

Ask: **"What's the new task?"** and wait. Do not begin implementation in this
command.

## Constraints

- Do not modify any spec files — they are read-only reference material.
- Do not start the actual task in this command; that comes next.
