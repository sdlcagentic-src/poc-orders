---
id: LANG-013
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
- STORY-005
trace:
  derives_from:
  - STORY-005
content_hash: sha256:6308b4fcab647d1c80d4164c459e98a46343232dfca392190ff59703af61c39a
created_at: 2026-10-13T09:57:13.266978Z
name: ConfirmationEmailSent
kind: event
definition: Triggered when a notification email is dispatched to a customer confirming
  their order has been accepted.
concerns: LANG-001
---
