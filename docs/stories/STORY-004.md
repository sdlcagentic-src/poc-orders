---
id: STORY-004
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
- REQ-004
trace:
  derives_from:
  - EPIC-002
  covers:
  - REQ-004
content_hash: sha256:ff4ef6b0842b1bb99ae491225385e9e9bcaecaebcb101be650ffd19fcdfa0485
created_at: 2026-10-12T12:58:06.181179Z
title: Cancel unshipped order and release stock
size: M
story:
  as_a: customer
  i_want: cancel an order that has not yet shipped
  so_that: the reserved stock is released and I am no longer charged
acceptance_criteria:
- id: AC-1
  given: I have a confirmed order with 3 units of Product A reserved
  when: I cancel the order before shipment
  then: the order status becomes cancelled and 3 units of Product A are released back
    to available inventory
---
