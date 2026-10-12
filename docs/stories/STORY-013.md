---
id: STORY-013
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
- REQ-013
trace:
  derives_from:
  - EPIC-005
  covers:
  - REQ-013
content_hash: sha256:a2ae7ca5ab5de65286a25ccb0e3c0a25e92ceb8dcf6fbcda4a2a421ae8185976
created_at: 2026-10-12T12:58:06.181791Z
title: Design as separate services with minimal UI
size: L
story:
  as_a: development team
  i_want: the system is built as separate services with a small web UI of two or three
    screens
  so_that: "we can scale, test, and deploy services independently"
acceptance_criteria:
- id: AC-1
  given: the system is deployed
  when: I review the architecture
  then: "order, inventory, and notification services are separate; the web UI has\
    \ 2–3 screens covering order creation, status view, and stock levels"
---
