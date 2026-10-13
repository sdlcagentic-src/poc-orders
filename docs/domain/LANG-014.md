---
id: LANG-014
type: language
version: 1
risk_tier: HIGH
produced_by:
  agent: architect
  model: claude-haiku-4-5-20251001
  prompt_pack: bmad-6.12.1@790dae9c8e2a1d73575cb2d40b14dd4963391f29
  run_id: RUN-0007
inputs:
- EPIC-003
- STORY-006
trace:
  derives_from:
  - STORY-006
content_hash: sha256:4856376e2f67154c18916a341e4c83263a37f5c2d50316be9a075846f294b483
created_at: 2026-10-13T09:57:13.267021Z
name: RejectionEmailSent
kind: event
definition: Triggered when a notification email is dispatched to a customer explaining
  why their order was rejected.
concerns: LANG-001
---
