---
id: EPIC-001
type: epic
version: 1
risk_tier: HIGH
produced_by:
  agent: pm
  model: claude-haiku-4-5-20251001
  prompt_pack: bmad-6.12.1@790dae9c8e2a1d73575cb2d40b14dd4963391f29
  run_id: RUN-0005
inputs:
- REQ-001
- REQ-002
trace:
  derives_from:
  - REQ-001
  - REQ-002
content_hash: sha256:5961b02c7bcbcee864f36abf4e54b93a34859a67cd48c6bec75f798502ed5b48
created_at: 2026-10-12T12:58:06.181936Z
title: Order creation and validation
goal: "Customers can place orders with multiple items and the system validates stock\
  \ availability, reserving inventory atomically."
---
