---
id: LANG-015
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
- STORY-007
trace:
  derives_from:
  - STORY-007
content_hash: sha256:73273d8d920150eadab22305530bec20d964180863f8100029c3fc3d55b779be
created_at: 2026-10-13T09:57:13.267065Z
name: CancellationEmailSent
kind: event
definition: Triggered when a notification email is dispatched to a customer confirming
  their order cancellation.
concerns: LANG-001
---
