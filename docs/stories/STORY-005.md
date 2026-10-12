---
id: STORY-005
type: story
version: 1
risk_tier: HIGH
produced_by:
  agent: pm
  model: claude-haiku-4-5-20251001
  prompt_pack: bmad-6.12.1@790dae9c8e2a1d73575cb2d40b14dd4963391f29
  run_id: RUN-0005
inputs:
- EPIC-003
- REQ-005
trace:
  derives_from:
  - EPIC-003
  covers:
  - REQ-005
content_hash: sha256:f2169cee4712f907a78b94a1b7c9a468361d408524e0483d0180cefe332f1aec
created_at: 2026-10-12T12:58:06.181260Z
title: Send order confirmation email
size: M
story:
  as_a: system
  i_want: send an email notification when an order is confirmed
  so_that: the customer knows their order has been accepted
acceptance_criteria:
- id: AC-1
  given: a customer order transitions to confirmed status
  when: the confirmation is recorded in the system
  then: an email is sent to the customer's email address with the order details
---
