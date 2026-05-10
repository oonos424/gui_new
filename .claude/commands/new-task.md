---
description: Check out a new branch and set up context for a new task
---

# /new-task — Begin a New Task

Run this command when starting a new feature or fix. It collects the
information needed, prepares the branch, and confirms the working context.

## Step 1: Gather Task Information

Ask the user for the following:

1. **Task type** — `feature` or `fix`
2. **Task description** — a short phrase describing the work (e.g. "login screen layout")
3. **Ticket number** *(optional)* — e.g. `AFF-42`; skip if none

## Step 2: Derive the Branch Name

Construct the branch name as:

```
<type>/<ticket>-<slug>
```

- `<type>` is `feature` or `fix`
- `<ticket>` is the ticket number, if provided (omit if none)
- `<slug>` is the description lowercased with spaces replaced by hyphens

Examples:
- type=`feature`, ticket=`AFF-42`, description=`login screen layout` → `feature/AFF-42-login-screen-layout`
- type=`fix`, ticket=none, description=`null pointer on startup` → `fix/null-pointer-on-startup`

Show the derived branch name to the user and ask for confirmation before
proceeding. If the user wants a different name, use their version.

## Step 3: Check Working Tree

Run:

```sh
git status
```

If there are uncommitted changes, stop and tell the user. Do not stash
automatically — let the user decide how to handle them.

## Step 4: Update develop and Create Branch

Run in order:

```sh
git checkout develop
git pull origin develop
git checkout -b <branch-name>
```

Stop on any failure and report the error to the user.

## Step 5: Confirm Context

Report to the user:

- Branch created: `<branch-name>`
- Base: `develop` at commit `<short-sha>` (`git rev-parse --short HEAD`)
- Task: the description they provided

Then ask: **"What would you like to start with?"** so the session flows
directly into the work.

## Constraints

- Always branch off `develop`, not `main`
- Never proceed past Step 3 if the working tree is dirty
- Do not commit or push anything in this command
