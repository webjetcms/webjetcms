---
name: wj-dependencycheck
description: Analyze an existing or newly generated OWASP Dependency Check report in the current WebJET CMS worktree, filtered by Highest Severity. Assess applicability and low-risk fixes, then present a decision table before changing dependencies, code, or suppressions. Use for recurring dependency vulnerability triage, including stable hotfix branches.
---

# WebJET Dependency Check

Produce an evidence-based vulnerability assessment for the current checkout. The initial deliverable is an analysis and a table of proposed actions; the user chooses what to implement. Respond in the conversation's language, normally Slovak.

## Inputs and scope

Resolve these inputs from the user's invocation and current conversation. Ask only for missing choices, preferably together:

- **Report source:** reuse `existing` or generate `new`.
- **Highest Severity:** one or more labels such as `CRITICAL`, `HIGH`, `MEDIUM`, or `LOW`, case-insensitive. Do not silently default to CRITICAL when omitted.
- **Severity matching:** a bare label means exact matching of the report's dependency-level **Highest Severity** column. Interpret an explicit request such as "HIGH and above" as a threshold. State the matching rule in the result. A HIGH vulnerability inside a dependency whose highest severity is CRITICAL is outside an exact HIGH row selection; explain this distinction if the user asks for individual CVE severities instead.

Examples:

```text
$wj-dependencycheck severity=CRITICAL report=existing
$wj-dependencycheck severity=HIGH report=new
$wj-dependencycheck severity=HIGH and above report=existing
```

Read the current branch, repository instructions, and working-tree status. Resolve all project paths against the current worktree root, obtained with `git rev-parse --show-toplevel`. If invoked outside a repository, locate or ask for the intended checkout. Never reuse an absolute path or another worktree's report from an earlier conversation or an ambient browser tab.

During initial analysis, leave source files, dependency manifests, lockfiles, suppression XML, and changelogs unchanged. Generating the requested report and normal tool caches is permitted. Do not install or upgrade dependencies merely to inspect them. Preserve unrelated local changes and never create commits.

## Select and verify the report

The default report path is always relative to the current worktree:

```text
build/reports/dependency-check-report.html
```

If the source was not specified and the report exists, show its embedded generation timestamp and ask whether to reuse it or generate a new one. Mention evidence of staleness, such as a mismatch between reported and currently resolved versions, without silently regenerating a report the user chose to reuse.

If no report exists and no source was specified, explain that generation is needed and generate it once the severity is known. If the user explicitly requested an existing report that is missing or invalid, ask whether to generate it instead.

For `new`, run the wrapper from the worktree root:

```sh
./gradlew --info dependencyCheckAnalyze
```

Use `gradlew.bat --info dependencyCheckAnalyze` on Windows. If Gradle reports `UP-TO-DATE` or `FROM-CACHE` and no new analysis actually ran, retry once with `--rerun-tasks`. Verify task completion and the report's embedded generation time; process exit alone or an old report left on disk does not prove that a new report was generated. A task may fail its configured severity gate after successfully producing a report: distinguish that from analyzer failure.

Surface analyzer failures and material coverage limits. Do not change scanner settings, suppress build failures, or expose tokens to force a clean result. If regeneration is blocked, ask before falling back to an older report. Distinguish the report timestamp from vulnerability-database freshness.

## Extract the selected findings

Use the bundled standard-library helper from the worktree root. Resolve `SKILL_DIR` to this skill's actual directory; it is not the project root:

```sh
python3 "$SKILL_DIR/scripts/select_findings.py" --severity CRITICAL
python3 "$SKILL_DIR/scripts/select_findings.py" --severity HIGH --at-least
```

The helper reads the relative report path and returns matching dependency rows, package identifiers, detail anchors, and counts. It does not assess vulnerabilities or extract every CVE from the detail sections. If the report format is unsupported, inspect its structure and use another parser; never interpret a parsing failure as zero findings.

Read the detailed sections referenced by the selected anchors, including installed versions, dependency paths, CVE/GHSA identifiers, scores, and suppression status. Filter by the summary column, not filenames, raw occurrences of severity words, or numeric sort attributes. In particular, suppressed CRITICAL entries can remain in the HTML even when there are no active CRITICAL rows.

Focus the assessment on vulnerabilities responsible for the selected severity levels. Mention lower-severity issues only when relevant to the same proposed remediation, and label them as context. Group repeated findings across modules or duplicate bundled files when they share a vulnerability and remedy, while preserving the affected artifacts and versions. Do not confuse dependency-row counts with distinct vulnerability counts.

Inspect the actual scanner configuration for runtime versus test/build coverage, disabled analyzers, local npm packages, and excluded paths. A package missing from a report is not proof that it is absent or safe. Cross-check relevant Gradle declarations/resolution, `package.json`, lockfiles, and bundled JavaScript when needed.

If there are no matching active findings, report zero for the selected scope, identify the report date and matching rule, and stop. Do not automatically expand to another severity or claim that all dependencies are vulnerability-free.

## Assess applicability and remediation

For each distinct issue:

1. **Verify the advisory.** Prefer current vendor advisories, official release notes, and upstream source/patches. Check affected and fixed versions, required features/configuration, and whether fixes are publicly available or commercial-support-only. Do not assume a version or a past Spring support policy is still current.
2. **Trace the actual installation.** Inspect relevant code, configuration, enabled modules, filter order, endpoints, and deployment information available in the worktree. State what an attacker must control or request, which server-side execution path is required, and which prerequisites are confirmed or unknown. A matching dependency version alone does not prove exploitable behavior; an unsuccessful search or lack of a reproduced exploit does not prove non-applicability.
3. **Propose the smallest suitable change.** On stable/hotfix branches, prefer compatible patch/minor upgrades with unchanged Java, servlet namespace, and public APIs. Check transitive dependencies and bundled copies. Mark major upgrades, javax-to-jakarta migrations, and refactors as unsuitable for a simple hotfix when applicable; explain the constraint rather than proposing them as trivial fixes.
4. **Explain behavioral tradeoffs.** Compare the actual old configuration with the proposed one, not just library defaults. For a workaround, explain its scope, initialization timing, overrides, performance, caching, and relevant edge cases. For example, early security-header writing can change cache overrides and interact with `response.reset()`; a compiler switch changes expression execution and may have global versus engine-specific effects. These examples are investigation prompts, not mandatory fixes or permanent suppression decisions.
5. **Specify verification.** Suggest targeted checks for the affected feature and any plausible compatibility risks, plus a fresh Dependency Check run after approved changes. State limitations of source inspection or isolated tests versus a reproduced behavior in the deployed application.

Use explicit applicability categories:

- **Applicable:** required conditions are established.
- **Not applicable:** a required condition is demonstrably absent; cite the evidence and when to reassess.
- **Unconfirmed:** the dependency is affected but reachability or deployment prerequisites remain unknown.
- **Accepted risk:** only when the user explicitly accepts a described residual risk. Keep the rationale distinct from a fix or proven non-applicability.

Do not carry CVE-specific conclusions, accepted risks, or upgrade versions from this chat into every future checkout. Reassess them against the selected report and current configuration.

## Present the decision table

Start with the worktree/branch, report source and generation time, severity matching rule, number of matched dependency rows, and deduplicated vulnerability count. Note material report limitations or version mismatches.

Present one row per distinct vulnerability/remediation group, using stable row numbers so the user can select actions. Use this shape, adapting the language to the conversation:

| # | Dependency and current version | CVE/GHSA; reported severity | Applicability and evidence | Proposed action / target version | Compatibility impact, effort, and verification |
|---|---|---|---|---|---|

Link primary advisory sources near the corresponding rows and use precise local code references for applicability. Keep long explanations below the table, referenced by row number. Separate simple update candidates, configuration workarounds, justified suppression candidates, unresolved findings, and changes requiring a larger migration through the proposed-action column.

For a suppression candidate, describe the exact condition and evidence, narrow affected package/version scope, residual uncertainty, and reassessment triggers. Do not write the XML during the initial assessment. Do not present "very unlikely" as proof of absence or invent deployment properties that were not inspected.

End by asking which numbered proposals the user wants implemented. Stop at the analysis unless the current conversation already explicitly authorizes implementing those particular proposals. Never broaden that authorization to other findings.

## After the user selects actions

Implement only the approved subset. Preserve existing authorization; do not ask the user to approve the same action again. Use narrowly scoped suppressions with English technical rationale, and distinguish proven non-applicability, configured mitigation, and accepted residual risk. Record relevant conditions for reassessment.

Run appropriate compatibility checks and regenerate Dependency Check when dependencies, scan inputs, or suppressions change. Verify resolved/locked versions and the new active findings. Report updates, mitigations, suppressions, and remaining vulnerabilities separately: "zero unsuppressed CRITICAL findings" does not mean every affected library was patched. If the user rejects or reverts a workaround, also update its suppression rationale and documentation so they do not claim it is still active.

Update the current release section in the repository's changelog with actual version changes and user-visible behavior or migration notes. Determine the correct branch/release section at execution time; do not hard-code the 2026 release or invent a ticket number. Leave all changes uncommitted for the user.
