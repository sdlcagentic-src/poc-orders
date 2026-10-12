---
id: STORY-010
type: story
version: 1
risk_tier: HIGH
produced_by:
  agent: pm
  model: claude-haiku-4-5-20251001
  prompt_pack: bmad-6.12.1@790dae9c8e2a1d73575cb2d40b14dd4963391f29
  run_id: RUN-0005
inputs:
- EPIC-005
- REQ-010
trace:
  derives_from:
  - EPIC-005
  covers:
  - REQ-010
content_hash: sha256:ce6c0ead1fd27c28ee0660ce947057d9e02db9bed797131548e5d21e901197e9
created_at: 2026-10-12T12:58:06.181608Z
title: Persist confirmed orders durably
size: L
story:
  as_a: system operator
  i_want: confirmed orders are never lost even if a service component restarts
  so_that: customers' purchases are protected and orders can always be recovered
acceptance_criteria:
- id: AC-1
  given: a customer order is confirmed and persisted
  when: a service component restarts unexpectedly
  then: the confirmed order is still present and can be retrieved after the component
    recovers
---
