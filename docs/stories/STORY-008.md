---
id: STORY-008
type: story
version: 1
risk_tier: MEDIUM
produced_by:
  agent: pm
  model: claude-haiku-4-5-20251001
  prompt_pack: bmad-6.12.1@790dae9c8e2a1d73575cb2d40b14dd4963391f29
  run_id: RUN-0005
inputs:
- EPIC-004
- REQ-008
trace:
  derives_from:
  - EPIC-004
  covers:
  - REQ-008
content_hash: sha256:08cdd080585eb1d03a2daaae04b70a244cbbd1102971f3ee706fb1b9a56df7fe
created_at: 2026-10-12T12:58:06.181469Z
title: View stock levels per product
size: S
story:
  as_a: staff member
  i_want: see the current stock level for each product
  so_that: I understand warehouse inventory and can plan replenishment
acceptance_criteria:
- id: AC-1
  given: the system has 5 units of Product A and 12 units of Product B in stock
  when: I view the stock levels page
  then: "I see Product A: 5 units and Product B: 12 units"
---
