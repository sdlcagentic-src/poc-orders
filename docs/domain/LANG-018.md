---
id: LANG-018
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
- EPIC-005
- STORY-005
- STORY-006
- STORY-007
- STORY-012
- STORY-013
trace:
  derives_from:
  - STORY-005
  - STORY-006
  - STORY-007
  - STORY-012
  - STORY-013
content_hash: sha256:5bcd93121a025c6ae80160defea59bc727f6e964cb9ece1b149da0e4a1b2eace
created_at: 2026-10-13T09:57:13.267248Z
name: Order Notifications
kind: subdomain
definition: Sends email notifications to customers when their orders change state.
classification: supporting
includes:
- LANG-013
- LANG-014
- LANG-015
---
