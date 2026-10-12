---
id: STORY-007
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
- REQ-007
trace:
  derives_from:
  - EPIC-003
  covers:
  - REQ-007
content_hash: sha256:a225a0c1a74180c8ea744d7ea315a162cd9d94b6a27679857a6a01becd58667f
created_at: 2026-10-12T12:58:06.181405Z
title: Send order cancellation email
size: M
story:
  as_a: system
  i_want: send an email notification when an order is cancelled
  so_that: the customer receives confirmation that their cancellation request was
    processed
acceptance_criteria:
- id: AC-1
  given: a customer cancels a confirmed order
  when: the cancellation is recorded in the system
  then: an email is sent to the customer's email address confirming the cancellation
---
