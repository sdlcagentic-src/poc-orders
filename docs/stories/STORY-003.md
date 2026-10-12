---
id: STORY-003
type: story
version: 1
risk_tier: MEDIUM
produced_by:
  agent: pm
  model: claude-haiku-4-5-20251001
  prompt_pack: bmad-6.12.1@790dae9c8e2a1d73575cb2d40b14dd4963391f29
  run_id: RUN-0005
inputs:
- EPIC-002
- REQ-003
trace:
  derives_from:
  - EPIC-002
  covers:
  - REQ-003
content_hash: sha256:2bda410146f2267b6bcdbc71395300090c6821930ccd3e542a81ee1200870c38
created_at: 2026-10-12T12:58:06.181098Z
title: View order status
size: S
story:
  as_a: customer
  i_want: check the status of my order
  so_that: "I know whether it has been placed, confirmed, rejected, or cancelled"
acceptance_criteria:
- id: AC-1
  given: I have placed an order
  when: I view my order
  then: "I see the current status: placed, confirmed, rejected, or cancelled"
---
