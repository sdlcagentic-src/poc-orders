---
id: STORY-006
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
- REQ-006
trace:
  derives_from:
  - EPIC-003
  covers:
  - REQ-006
content_hash: sha256:d0c996ef5f59e306c29fb6ba2ea206df35577e054b4544f6e050c74dcb16a7dd
created_at: 2026-10-12T12:58:06.181332Z
title: Send order rejection email
size: M
story:
  as_a: system
  i_want: send an email notification when an order is rejected
  so_that: the customer knows their order was not accepted and why
acceptance_criteria:
- id: AC-1
  given: a customer order is rejected due to insufficient stock
  when: the rejection is recorded in the system
  then: an email is sent to the customer's email address explaining the rejection
    reason
---
