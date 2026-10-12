---
id: STORY-011
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
- REQ-011
trace:
  derives_from:
  - EPIC-005
  covers:
  - REQ-011
content_hash: sha256:fb2d604ef92d9e6a88d3313a447b97d0a9669e87676a7de807bb55fc45aa627a
created_at: 2026-10-12T12:58:06.181668Z
title: Prevent stock from going negative
size: L
story:
  as_a: system operator
  i_want: "stock levels never fall below zero, even under concurrent orders"
  so_that: inventory is always accurate and consistent
acceptance_criteria:
- id: AC-1
  given: Product A has 1 unit in stock
  when: two customers simultaneously place orders each requesting 1 unit of Product
    A
  then: one order is confirmed and reserves the unit; the other is rejected because
    stock is exhausted
---
