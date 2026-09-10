---
name: skill-orchestrator
description: >-
  Use this skill when the user's request is complex, multi-faceted, or spans
  multiple domains (e.g., backend + frontend + database + deployment). This skill
  helps the agent decide which other skills to activate, in what order, and how
  to coordinate their outputs. Activate when orchestrating multiple workflows,
  delegating to subagents, or decomposing a large task into parallel/sequential
  subtasks.
---

# Skill Orchestrator

The Skill Orchestrator helps you plan and coordinate the activation of other skills and subagents for complex, multi-step tasks specific to the **VaaniSetu** project.

---

## When to Use

- The task requires **more than one distinct workflow** (e.g., both an API change and a frontend update).
- Work can be **parallelized** across multiple agents or skill areas.
- The user has asked for something broad (e.g., "build feature X end-to-end").
- You need a **coordination layer** before diving into implementation.

---

## Orchestration Workflow

### Step 1 — Decompose the Task

Break the user request into atomic subtasks. For each subtask, identify:
- **What** needs to be done
- **Which skill** or domain it falls under (e.g., API, UI, DB, DevOps, ML)
- **Dependencies** — does it need another subtask to finish first?

```
Task: [high-level user request]
├── Subtask A: [description]  → Skill: [skill-name]  → Depends on: none
├── Subtask B: [description]  → Skill: [skill-name]  → Depends on: A
└── Subtask C: [description]  → Skill: [skill-name]  → Depends on: none (parallel with A)
```

### Step 2 — Assign Execution Mode

| Mode       | When to Use                                           |
|------------|-------------------------------------------------------|
| Sequential | Subtask B needs output from Subtask A                |
| Parallel   | Subtasks are independent                             |
| Delegated  | Subtask is complex enough to warrant its own subagent|

### Step 3 — Activate Skills

For each subtask, explicitly read and activate the relevant SKILL.md before proceeding.

If delegating to a subagent:
- Provide it a **clear, scoped prompt** referencing the relevant skill.
- Specify the **expected output format** (e.g., file path, JSON, summary).
- Set a **checkpoint**: what does "done" look like?

### Step 4 — Integrate Results

After all subtasks complete:
1. Verify each output meets its acceptance criteria.
2. Merge or chain outputs as needed.
3. Run a **final sanity check** (lint, build, test, or manual review).
4. Report back to the user with a concise summary of what was done.

---

## VaaniSetu-Specific Domains

Map tasks to these project domains:

| Domain         | Description                                      |
|----------------|--------------------------------------------------|
| speech         | ASR / TTS / language model integration           |
| translation    | NLP translation pipeline                         |
| api            | FastAPI / Flask backend routes                   |
| frontend       | React / HTML UI components                       |
| database       | Schema design, migrations, queries               |
| infra          | Docker, CI/CD, deployment scripts                |
| testing        | Unit tests, integration tests, E2E tests         |

---

## Anti-Patterns to Avoid

- Do NOT activate every skill "just in case" — only activate what is needed.
- Do NOT start implementation before decomposition is complete.
- Do NOT merge subtask results without verifying each one individually.
- Always document which skills were activated and in what order.
