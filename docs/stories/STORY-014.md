---
id: STORY-014
type: story
version: 1
risk_tier: LOW
produced_by:
  agent: pm
  model: claude-haiku-4-5-20251001
  prompt_pack: bmad-6.12.1@790dae9c8e2a1d73575cb2d40b14dd4963391f29
  run_id: RUN-0005
inputs:
- EPIC-005
- REQ-014
trace:
  derives_from:
  - EPIC-005
  covers:
  - REQ-014
content_hash: sha256:e77cd79b13f597d48883fe44fcd1b768baa8088a3ae52d4ddc3c5b5c92b9d95c
created_at: 2026-10-12T12:58:06.181858Z
title: "Clarify scope: payment, shipping, and returns excluded"
size: S
story:
  as_a: team
  i_want: "confirm that payment processing, shipping arrangements, and returns handling\
    \ are not built in this version"
  so_that: "we focus only on order creation, validation, and basic lifecycle management"
acceptance_criteria:
- id: AC-1
  given: the backlog is defined
  when: I review the product scope
  then: "no stories cover payment gateways, shipping label generation, or return request\
    \ workflows"
---
