---
description: Ensure tests are in place and all CI checks pass before opening a PR
---

# /prepare-pr — Prepare for Pull Request

Run this command when a feature or fix is behaviorally confirmed and ready for
review. It mirrors the CI pipeline (`.github/workflows/pr-ci.yml`) so that the
PR passes on the first push.

## Step 1: Fix Formatting

Run:

```sh
./gradlew spotlessApply
```

This auto-corrects any formatting issues so that the subsequent `spotlessCheck`
CI step will not fail.

## Step 2: Review Test Coverage

Read the changes on the current branch (`git diff main...HEAD`) and evaluate
whether tests adequately cover:

- Important logic added or changed
- Any regression-prone behaviour
- Non-trivial edge cases

If coverage is insufficient, write the missing tests now before proceeding.
Follow the project layout: test files live alongside source files under
`src/test/`.

## Step 3: Run CI Checks Locally

Run each check in order and stop on the first failure:

```sh
./gradlew compileJava
./gradlew spotlessCheck
./gradlew nullCheck
./gradlew test
```

If any step fails, fix the issue and re-run that step before continuing.

## Step 4: Report Results

Summarise to the user:

- Which checks passed
- Whether any tests were added in Step 2 (and what they cover)
- Any issues found and how they were resolved
- Confirmation that the branch is ready to push and open a PR

## Constraints

- Do not open the PR automatically — leave that to the user or `/commit`
- Do not skip `nullCheck`; Checker Framework nullness errors block CI
- If `spotlessApply` produced changes, stage and commit them before opening the
  PR (use `/commit`)
