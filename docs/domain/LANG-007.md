---
id: LANG-007
type: language
version: 1
risk_tier: HIGH
produced_by:
  agent: architect
  model: claude-haiku-4-5-20251001
  prompt_pack: bmad-6.12.1@790dae9c8e2a1d73575cb2d40b14dd4963391f29
  run_id: RUN-0007
inputs:
- EPIC-001
- STORY-001
trace:
  derives_from:
  - STORY-001
content_hash: sha256:1583b94e0ed046e4f6ae20446b63cb574d757ec0c5d4e09cf548206f516cc7d2
created_at: 2026-10-13T09:57:13.266695Z
name: OrderPlaced
kind: event
definition: Triggered when a customer submits a new order with line items for processing.
concerns: LANG-001
---
