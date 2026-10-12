---
id: STORY-012
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
- REQ-012
trace:
  derives_from:
  - EPIC-005
  covers:
  - REQ-012
content_hash: sha256:2dcb4c30126771c6dc77b4d28aa9652cd23e29a565314d7d8163b6e1f7ceafa7
created_at: 2026-10-12T12:58:06.181728Z
title: Protect customer email privacy
size: M
story:
  as_a: system operator
  i_want: email addresses are stored only in services that require them and never
    written to logs
  so_that: customer privacy is protected and email data is not exposed
acceptance_criteria:
- id: AC-1
  given: a customer places an order with their email address
  when: the order is processed and logged
  then: the email address appears only in the order service database and does not
    appear in any log output
---
