---
id: LANG-008
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
- STORY-002
trace:
  derives_from:
  - STORY-002
content_hash: sha256:7ca4d365875ab4297cd5f33ec1e4e74a4033394cb4237d13c4097e548a4aef16
created_at: 2026-10-13T09:57:13.266740Z
name: StockChecked
kind: event
definition: Triggered when the system verifies availability of all items in an order
  against current inventory.
concerns: LANG-001
---
