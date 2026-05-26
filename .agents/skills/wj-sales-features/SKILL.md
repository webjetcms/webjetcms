---
name: wj-sales-features
description: 'Generates sales-oriented feature descriptions from PR changes or attached files. Outputs a Slovak-language section describing the feature from a business/sales perspective with benefits, competitive advantages, and customer value. Use when: documenting a new feature for the sales team, generating sales descriptions from code changes, creating benefit lists for customer presentations.'
argument-hint: 'Optionally specify the target year (default: current year) or a specific feature area to focus on (e.g. "page templates" or "user permissions").'
---

# wj-sales-features

Analyzes code changes (from a Pull Request or attached files) and generates a sales-oriented feature description in Slovak. The output is inserted at the top of the yearly sales documentation file (`docs/sk/sales/{YEAR}/README.md`).

## When to Use

- A new feature or significant improvement has been completed (PR merged or ready to merge)
- You want to document a feature from the sales/business perspective
- You need to prepare material for the sales team about what was delivered
- You are attaching files or pointing to a PR and want a sales-ready description

## Procedure

### 1. Determine the Source of Changes

Check if the user has:

**Option A — Pull Request (branch diff)**

```bash
git rev-parse --abbrev-ref HEAD
BASE_BRANCH=origin/main
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [[ "$CURRENT_BRANCH" == hotfix/* ]]; then BASE_BRANCH=origin/hotfix/2026.0-main; fi
git diff --name-status $(git merge-base HEAD $BASE_BRANCH)
```

**Option B — Attached/specified files**

The user provides files directly (attached to the conversation or listed explicitly). Read and analyze those files.

If neither is clear, ask:

> "Mám analyzovať zmeny z aktuálneho Pull Requestu (vetvy), alebo máte konkrétne súbory, ktoré chcete opísať?"

### 2. Analyze the Changes

For each relevant file (Java, JavaScript/TypeScript, HTML/Thymeleaf templates, configuration, SQL):

- Identify **what the feature does** from the end-user perspective
- Identify **new capabilities** — what can the user/admin now do that they couldn't before?
- Identify **improvements** — what is now better, faster, more secure?
- Look for configuration options that provide flexibility
- Look for API endpoints or integrations that enable extensibility
- Note any security enhancements, performance improvements, or accessibility changes

Focus on understanding the **business value**, not the implementation details.

### 3. Check for Existing Screenshots and Documentation

Search for related images and documentation that can be referenced:

```bash
# Check CHANGELOG for references to related images
grep -i "<feature-keyword>" docs/sk/CHANGELOG-2026.md | head -20

# Check if there are images in related doc sections
find docs/sk/ -name "*.png" -path "*<feature-keyword>*" 2>/dev/null
find docs/sk/ -name "*.png" -path "*<related-folder>*" 2>/dev/null
```

If screenshots exist in the documentation, note their relative path from `docs/sk/sales/2026/` for inclusion in the output.

### 4. Determine the Target File

Default target: `docs/sk/sales/{CURRENT_YEAR}/README.md`

If the file does not exist, create it with the header:

```markdown
# Informácie o predaji - rok {YEAR}

Tento súbor obsahuje opisy vlastností WebJET CMS dodaných v roku {YEAR} z pohľadu predaja. Nové záznamy sa pridávajú na vrch (pod tento úvod), takže najnovšie vlastnosti sú vždy hore.

---
```

### 5. Generate the Sales Description

Write a section in **Slovak** with the following structure:

```markdown
### {Feature Name — concise, non-technical}

{2-3 paragraphs explaining:
- What the feature is (explain technical concepts in layperson terms)
- What problem it solves for the customer
- What the customer gains by having this feature
- Why this matters (competitive advantage, compliance, efficiency, security)
- Mention relevance for specific customer segments only where particularly obvious
- Use **bold** to highlight key features, capabilities, and important points within the paragraphs so readers can quickly scan and find the most important information}

**Hlavné benefity:**

- **{Benefit 1}**: {Short description of why it's good / what the customer gains}
- **{Benefit 2}**: {Short description}
- **{Benefit 3}**: {Short description}
- ...

{Optional: image reference if found}
![{Alt text}](../../{relative-path-to-image})

{Optional: link to detailed documentation}
Podrobná dokumentácia: [{link text}](../../{path-to-docs})
```

#### Writing Guidelines

- **Language**: Slovak
- **Tone**: Professional but accessible — as if explaining to an intelligent non-technical person
- **Focus**: Customer value, benefits, competitive advantages, security, compliance
- **Bold highlights**: Use **bold** for key features, capabilities, and important points within paragraphs — readers should be able to scan the text and grasp the most important information at a glance
- **Avoid**: Implementation details, class names, method names, technical jargon without explanation
- **Explain**: Technical terms in parentheses or inline (e.g., "API (rozhranie pre prepojenie s inými systémami)")
- **Competitive angle**: If the feature provides an advantage over typical CMS solutions, mention it
- **Security/operations**: Highlight security benefits, easier maintenance, lower operational costs where relevant
- **Extensibility**: Always note if the feature can be customized or extended (important for all customer segments)
- **Benefit format**: Each benefit should be actionable — state what the customer gains, not just what the system does

### 6. Insert into the Target File

Insert the generated section at the **TOP** of the year file — immediately after the introductory text and the `---` separator. This ensures the most recent features appear first.

Read the current content of the target file, find the first `---` separator after the main heading, and insert the new section below it.

### 7. Quality Check

After generating, verify:

- [ ] Description is in Slovak
- [ ] No raw technical jargon without explanation
- [ ] Benefits are customer-focused (not developer-focused)
- [ ] 2-3 paragraphs of explanation + bullet list of benefits
- [ ] Feature name is understandable for a non-technical person
- [ ] Screenshots referenced correctly (if found)
- [ ] Section inserted at the top of the year file (below header/intro)
- [ ] Content would be useful for a sales person in a customer meeting

## Notes

- If the change is purely internal refactoring with no user-visible impact, inform the user that there is nothing to document for sales and skip generation.
- If the PR contains multiple independent features, generate a separate section for each.
- Do NOT add entries to CHANGELOG — that is handled by other skills/processes.
- Image paths should be relative from `docs/sk/sales/{YEAR}/` — typically `../../{section}/{image}.png`.
- When unsure about the business value of a technical change, ask the user for clarification rather than guessing.
- The output should be useful even without the screenshots — images are supplementary, not required.
