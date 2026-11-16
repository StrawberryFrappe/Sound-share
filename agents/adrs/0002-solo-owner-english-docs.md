# ADR 0002: Solo Owner, English Harness Documents

## Status

Accepted by the owner on 2026-11-16.

## Context

The kit asks who else works in the repository and in which language, because
both change file naming and whether translations are needed.

## Decision

- The owner is the only contributor. One stakeholder, the listener, is kept in
  mind for product decisions but does not work in the repository.
- ADR and review files carry no author slugs. Record folders use the slug `SF`
  (ASM-006).
- Harness documents are written in English only. No translation protocol is
  mounted and GATE-I18N is inactive.
- The app's own text is a separate matter: it ships in English and Spanish, kept
  in step by GATE-L10N.

## Consequences

- Simple file naming.
- If a second contributor arrives, pick slugs in a new ADR; IDs minted before
  then belong to the owner.
- A Spanish-only reader cannot read the harness; revisit if that changes.

## Sources

- SRC-001 (owner answers, 2026-11-16)
