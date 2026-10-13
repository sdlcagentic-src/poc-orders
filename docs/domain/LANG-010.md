---
id: LANG-010
type: language
version: 1
risk_tier: HIGH
produced_by:
  agent: architect
  model: claude-haiku-4-5-20251001
  prompt_pack: bmad-6.12.1@790dae9c8e2a1d73575cb2d40b14dd4963391f29
  run_id: RUN-0007
inputs:
- EPIC-001
- EPIC-003
- STORY-002
- STORY-006
trace:
  derives_from:
  - STORY-002
  - STORY-006
content_hash: sha256:2e6363ed738da136e5fd6b69341457e0b07809a7e916f4817b4836304593affc
created_at: 2026-10-13T09:57:13.266836Z
name: OrderRejected
kind: event
definition: Triggered when one or more items in an order are out of stock and the
  order cannot be accepted.
concerns: LANG-001
---
