# Domain documentation

## Read when exploring concepts or architectural decisions

Start with the existing [WebJET knowledge index](../../.github/agents/AGENTS.md) and the relevant topic. These remain the canonical framework references.

This repository uses a single domain context:

- `GLOSSARY.md` at the repository root holds agreed domain terms.
- `docs/adr/` holds architectural decision records. Read only decisions relevant to the current task.

If the glossary or ADR directory does not exist, proceed silently. Create entries only when terms or decisions are actually resolved through domain-modeling work; setup does not create placeholders.

## Vocabulary and decisions

Use glossary terms consistently in issues, proposals, code discussions and tests. Verify unfamiliar terms against the existing WebJET documentation and source before adding them.

Surface conflicts with existing ADRs explicitly and explain why a decision may need revisiting. Keep framework knowledge in `.github/agents/`; glossary entries and ADRs should link to those references instead of copying them.
