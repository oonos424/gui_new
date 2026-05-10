---
description: Load legacy-GUI code context before the user describes a new task
---

# /from-legacy — Load Legacy Code Context

Run this at the start of a session when the upcoming task should be informed
by how the legacy AFFr GUI implemented the same area. This command only sets
up context — the user will describe the actual task afterwards.

## Step 1: Resolve the Legacy GUI Root

The legacy GUI path is recorded per-OS in two parallel places:

- Personal Claude memory: `legacy_gui_location.md`
- Repo (shared with Cursor): `.cursor/rules/legacy_gui.mdc`

Look up the absolute path for the current OS. If the entry is still
`_not yet recorded_`:

1. Ask the user for the absolute path on this OS.
2. Update both files above with the answer.
3. Then continue.

If the path contains non-ASCII segments (e.g. a Google Drive `マイドライブ`
mount), apply the wildcard-resolution approach from `spec_location.md`.

## Step 2: Identify the Code Area

If the user passed an argument, treat it as a path relative to the legacy
root (e.g. `src/.../LoginController.java`, `src/ui/`).

If no argument was given, list the top-level structure of the legacy root and
ask which area is relevant. Multiple selections are fine.

Argument: $ARGUMENTS

## Step 3: Read the Code

Read the requested files. For directories, list contents first and read what
the user points at — do not bulk-read entire trees.

Summarize back in 3–5 bullets: how the area is organized, the key classes
or functions, and any notable patterns or quirks. This confirms you loaded
the right context.

## Step 4: Await the Task

Ask: **"What's the new task?"** and wait. Do not begin implementation in this
command.

## Constraints

- The legacy codebase is read-only reference material — do not modify files
  there.
- Do not start the actual task in this command; that comes next.
