---
name: concise-planning
description: >-
  Use this skill before starting any non-trivial implementation task. It enforces
  a lean, structured planning phase: one-sentence goal, bullet-point steps, explicit
  open questions, and a verification checklist. Activate when the user asks to build,
  refactor, or fix something that requires more than a single file change. The plan
  must be approved by the user before execution begins.
---

# Concise Planning

A lightweight planning protocol for the **VaaniSetu** project. Plans must be **short, scannable, and actionable** — not essays.

---

## Planning Template

Fill in this template before every non-trivial task. Keep each section brief.

```markdown
## Plan: [Task Title]

**Goal (1 sentence):**
[What will be true when this task is done?]

**Steps:**
1. [First concrete action — include file/module names where known]
2. [Second action]
3. ...

**Open Questions:**
- [ ] [Any ambiguity that could change the approach — ask the user if critical]

**Risks:**
- [Anything that could break existing functionality]

**Verification:**
- [ ] [How will you confirm it works? e.g., run test, check endpoint, view UI]
```

---

## Rules of Concise Planning

| Rule | Detail |
|------|--------|
| Max 10 steps | If you need more, split into sub-plans |
| No implementation in planning | Do not write code until the plan is approved |
| Name files explicitly | Say src/api/routes.py not "the backend file" |
| One goal per plan | If two goals, write two plans |
| Surface blockers early | Open questions section is mandatory if anything is unclear |

---

## When to Skip Planning

You may skip the planning phase for:
- Fixing a typo or formatting issue
- Adding a single comment or docstring
- A change explicitly marked "trivial" by the user

---

## After Approval

Once the user approves the plan:
1. Create task.md with checkboxes mirroring the plan steps.
2. Execute steps in order, marking each [x] when done.
3. On completion, update walkthrough.md with a brief summary of what changed and how it was verified.

---

## Example (VaaniSetu)

```markdown
## Plan: Add Hindi to English translation endpoint

**Goal:** Expose a POST /translate endpoint that accepts Hindi text and returns English.

**Steps:**
1. Add TranslationRequest Pydantic schema in src/schemas.py
2. Implement translate_hi_en() in src/services/translation.py using Helsinki-NLP model
3. Wire POST /translate route in src/api/routes.py
4. Add unit test in tests/test_translation.py

**Open Questions:**
- [ ] Should we cache translations? (ask user)

**Risks:**
- Model load time may slow cold-start — consider lazy loading

**Verification:**
- [ ] pytest tests/test_translation.py passes
- [ ] curl -X POST /translate -d '{"text":"namaste"}' returns {"translation":"Hello"}
```
