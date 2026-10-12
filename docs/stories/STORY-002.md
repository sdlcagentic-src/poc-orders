---
id: STORY-002
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
- REQ-002
trace:
  derives_from:
  - EPIC-001
  covers:
  - REQ-002
content_hash: sha256:c7ac4e6655a92d5855ccada82f8722171bb18ef32409156d455c531727dc85cc
created_at: 2026-10-12T12:58:06.181001Z
title: Check stock and reserve on order placement
size: M
story:
  as_a: customer
  i_want: have the system verify all items are in stock before accepting my order
  so_that: I know the order will only be confirmed if inventory is available
acceptance_criteria:
- id: AC-1
  given: Product A has 5 units in stock and Product B has 0 units
  when: I place an order for 3 units of Product A and 1 unit of Product B
  then: the order is rejected and I see a message that Product B is out of stock
- id: AC-2
  given: Product A has 5 units and Product B has 3 units in stock
  when: I place an order for 2 units of Product A and 2 units of Product B
  then: the order is accepted and both items are reserved from inventory
---
