---
id: LANG-012
type: language
version: 1
risk_tier: MEDIUM
produced_by:
  agent: architect
  model: claude-haiku-4-5-20251001
  prompt_pack: bmad-6.12.1@790dae9c8e2a1d73575cb2d40b14dd4963391f29
  run_id: RUN-0007
inputs:
- EPIC-002
- STORY-004
trace:
  derives_from:
  - STORY-004
content_hash: sha256:14149fb07fb4d44556cbeb32666ee0f5eedfef439d28e0d1d93140fe2ec77920
created_at: 2026-10-13T09:57:13.266930Z
name: StockReleased
kind: event
definition: Triggered when reserved inventory is released back to available stock
  due to order cancellation or rejection.
concerns: LANG-004
---
