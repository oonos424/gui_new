---
description: Record a WorkLog entry for the just-completed task
---

# /worklog — Record Task Completion

You are about to write a WorkLog entry summarizing the task that was just completed in this session.

## Step 1: Determine Target Location

WorkLog entries live in `log/worklog/` organized by month:

```
log/worklog/
├── README.md
├── 2026-05.md
├── 2026-06.md
└── ...
```

Determine the current month and target file (e.g., `log/worklog/2026-05.md`). If the file does not exist, create it with a top-level heading: `# WorkLog — May 2026`.

## Step 2: Draft the Entry

Based on the conversation in this session, draft an entry using this template:

```markdown
## YYYY-MM-DD — [Task ID or Short Title]

### What was done
[One or two sentences describing the change.]

### Decisions
- [Choice made and why. Skip this section if no notable decisions.]

### Problems and resolutions
- [Issue encountered and how it was resolved. Skip if none.]

### Spec updates
- [Files in log/specs/ that were updated as a result. "None" if no spec updates needed.]

### Open questions
- [Anything deferred to a later task. Skip if none.]

### Notes for future sessions
- [Gotchas, advice, things future sessions should know. Skip if none.]
```

Fill in only the sections that apply to this task. Omit empty sections rather than leaving them blank.

## Step 3: Confirm Before Writing

Show the drafted entry to the user. Ask:

> "Here is the drafted WorkLog entry. Approve as-is, or should I revise?"

Wait for approval. Apply any revisions requested.

## Step 4: Append to the WorkLog File

After approval, append the entry to the target month file. Insert a blank line before it if the file already has content.

## Step 5: Verify Spec Synchronization

If the entry's "Spec updates" section says "None" but significant code or behavior changed in this session, ask the user:

> "This task changed code/behavior but no spec updates are recorded. Should we update the relevant specs before closing this task?"

This enforces the spec/code co-evolution discipline.

## Constraints

- Do not invent decisions or problems that didn't actually occur in the session. Empty sections are better than fabricated content.
- Keep the entry compact. 10–30 lines total is the target.
- Do not duplicate content that belongs in specs (current state of the system). The WorkLog records change, not state.
- If the session involved multiple distinct tasks, ask the user whether to record one entry covering all, or separate entries per task.
