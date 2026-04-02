#!/usr/bin/env python3
"""Claude-powered lint and dependency validation agent.

Reads a unified git diff from stdin, sends it to the Claude API,
and prints a structured Markdown lint + dependency validation report
to stdout.  Designed to gate PRs targeting production (e.g. main).
"""

import os
import sys

import anthropic

_MAX_DIFF_CHARS = 50_000
_MODEL = "claude-opus-4-5"
_MAX_TOKENS = 4096

_PROMPT_TEMPLATE = """\
You are a senior software engineer acting as an automated quality gate before
code is merged to the production branch.

Analyse the following unified diff and produce a concise Markdown report with
these sections:

## Lint & Code Style Issues
Check for:
- Formatting violations (inconsistent indentation, trailing whitespace, line
  length, blank-line rules).
- Naming convention violations (variables, functions, classes, constants).
- Unused imports, variables, or dead code introduced by this diff.
- Missing or malformed docstrings / JavaDoc / JSDoc comments on public APIs.
- Overly complex expressions that should be simplified.

For each finding include the file path and approximate line number when
identifiable.

## Dependency Validation
Check for:
- New dependencies added (list package name, version, and ecosystem).
- Dependencies pinned to an overly broad version range (e.g. `*`, `>=0`,
  `LATEST`) that could pull in breaking or vulnerable future releases.
- Dependencies known to have had security advisories (CVEs) — flag any
  package that is well-known to be problematic at the referenced version.
- Duplicate or conflicting dependency declarations.
- Dependencies that appear unused based on the diff (imported/declared but
  never referenced in the changed code).
- Dev-only dependencies (e.g. test libraries) accidentally added to the
  production dependency scope.

## Pre-Merge Checklist
A short ✅ / ⚠️ / ❌ checklist summarising the overall readiness of this PR
for production merge:

| Check | Status | Notes |
|-------|--------|-------|
| Code style / lint | ✅ / ⚠️ / ❌ | brief note |
| Dependency hygiene | ✅ / ⚠️ / ❌ | brief note |
| No broad/unpinned versions | ✅ / ⚠️ / ❌ | brief note |
| No known-vulnerable packages | ✅ / ⚠️ / ❌ | brief note |
| No dev deps in prod scope | ✅ / ⚠️ / ❌ | brief note |

Use ✅ when no issues are found, ⚠️ for warnings, ❌ for blocking issues.

## Verdict
State clearly: **PASS**, **PASS WITH WARNINGS**, or **BLOCK**.
- PASS — no lint or dependency issues found.
- PASS WITH WARNINGS — minor issues that should be addressed but are not
  blocking.
- BLOCK — at least one issue is serious enough to prevent merging to
  production (e.g. known-vulnerable dependency, extremely broad version pin,
  syntax/lint errors that would break the build).

If a section has nothing to report, write "Nothing to report." rather than
omitting the section.

```diff
{diff}
```
"""


def main() -> None:
    diff = sys.stdin.read()
    if not diff.strip():
        print("No diff found. Skipping lint and dependency check.")
        return

    if len(diff) > _MAX_DIFF_CHARS:
        diff = diff[:_MAX_DIFF_CHARS] + "\n\n… (diff truncated — too large to display in full)"

    api_key = os.environ.get("ANTHROPIC_API_KEY")
    if not api_key:
        print("ERROR: ANTHROPIC_API_KEY environment variable is not set.", file=sys.stderr)
        sys.exit(1)

    client = anthropic.Anthropic(api_key=api_key)
    message = client.messages.create(
        model=_MODEL,
        max_tokens=_MAX_TOKENS,
        messages=[{"role": "user", "content": _PROMPT_TEMPLATE.format(diff=diff)}],
    )
    print(message.content[0].text)


if __name__ == "__main__":
    main()
