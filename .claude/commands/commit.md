---
description: Stage changes, confirm commit message, then commit
---

# /commit — Confirm and Commit

## Step 1: Inspect Current State

Run `git status` and `git diff` (including staged changes) to understand what
has changed. Also run `git log --oneline -5` to see recent commit messages and
match the style.

## Step 2: Draft Commit Content

Identify which files should be staged. Exclude:
- `.DS_Store` and other OS noise
- Files that likely contain secrets (`.env`, credentials)
- Large generated artifacts not meant for version control

Draft a commit message:
- First line: short imperative summary (≤ 72 characters)
- Body (if needed): 1–3 sentences on *why*, not *what*. Wrap at 72 characters.

## Step 3: Confirm with the User

Present:
1. The list of files that will be staged
2. The full commit message

Then ask:

> "Approve as-is, or should I revise?"

Wait for the user's response. Apply any requested revisions and re-confirm if
the changes are significant.

## Step 4: Commit

After approval:
1. Stage the approved files by name (avoid `git add -A` or `git add .`)
2. Create the commit with the approved message, ending with:
   `Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>`
3. Run `git status` to verify success and report back.

## Constraints

- Never skip hooks (`--no-verify`)
- Never amend a published commit
- Never force-push
- If a pre-commit hook fails, fix the issue and create a new commit — do not
  retry with `--no-verify`
