---
id: STORY-001
type: story
version: 1
risk_tier: HIGH
produced_by:
  agent: pm
  model: claude-haiku-4-5-20251001
  prompt_pack: bmad-6.12.1@790dae9c8e2a1d73575cb2d40b14dd4963391f29
  run_id: RUN-0005
inputs:
- EPIC-001
- REQ-001
trace:
  derives_from:
  - EPIC-001
  covers:
  - REQ-001
content_hash: sha256:aee656a5f7c90d3be4fb194833dc9f1867ea297a8c66899c487fd70bd471571b
created_at: 2026-10-12T12:58:06.180853Z
title: Place order with line items
size: M
story:
  as_a: customer
  i_want: create an order specifying one or more products with quantities
  so_that: I can buy multiple items in a single transaction
acceptance_criteria:
- id: AC-1
  given: I am a customer identified by email
  when: "I submit an order with two products, 3 units of Product A and 2 units of\
    \ Product B"
  then: the system records the order with both line items and their quantities
---
