---
id: STORY-009
type: story
version: 1
risk_tier: MEDIUM
produced_by:
  agent: pm
  model: claude-haiku-4-5-20251001
  prompt_pack: bmad-6.12.1@790dae9c8e2a1d73575cb2d40b14dd4963391f29
  run_id: RUN-0005
inputs:
- EPIC-005
- REQ-009
trace:
  derives_from:
  - EPIC-005
  covers:
  - REQ-009
content_hash: sha256:92ab34cb9656c6b8bb109b785a306dbd26f1e9ee61e5d0c5316bdc20d75d4750
created_at: 2026-10-12T12:58:06.181549Z
title: Order placement within performance threshold
size: L
story:
  as_a: system operator
  i_want: order placement requests complete within 2 seconds for 95 percent of requests
  so_that: customers experience fast checkout and the system scales efficiently
acceptance_criteria:
- id: AC-1
  given: the system is under normal load
  when: 100 order placement requests are submitted
  then: at least 95 of them complete within 2 seconds
---
