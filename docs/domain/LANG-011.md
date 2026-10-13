---
id: LANG-011
type: language
version: 1
risk_tier: HIGH
produced_by:
  agent: architect
  model: claude-haiku-4-5-20251001
  prompt_pack: bmad-6.12.1@790dae9c8e2a1d73575cb2d40b14dd4963391f29
  run_id: RUN-0007
inputs:
- EPIC-002
- EPIC-003
- STORY-004
- STORY-007
trace:
  derives_from:
  - STORY-004
  - STORY-007
content_hash: sha256:8958b4c0b70bbe13ab2d2844c6973df701059e6e8a2b5beb1760172a652df281
created_at: 2026-10-13T09:57:13.266881Z
name: OrderCancelled
kind: event
definition: "Triggered when a customer cancels a confirmed order before it has shipped,\
  \ releasing reserved inventory."
concerns: LANG-001
---
