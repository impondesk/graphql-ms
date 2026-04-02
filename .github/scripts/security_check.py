#!/usr/bin/env python3
"""Claude-powered security analysis agent.

Reads a unified git diff from stdin, sends it to the Claude API,
and prints a structured Markdown security-findings report to stdout.
"""

import os
import sys

import anthropic

_MAX_DIFF_CHARS = 50_000
_MODEL = "claude-opus-4-5"
_MAX_TOKENS = 4096

_PROMPT_TEMPLATE = """\
You are an application-security expert reviewing a pull request.
Analyse the following code diff for security issues and produce a Markdown
report with these sections:

## Security Findings
For each finding use this sub-format:

### [SEVERITY] Short Title
- **Severity**: Critical | High | Medium | Low | Informational
- **File / Location**: <file path and line(s) if identifiable>
- **Description**: What the vulnerability is and how it could be exploited.
- **Recommendation**: The concrete fix or mitigation.

Severity guide:
- **Critical** – immediate exploitation possible (e.g. RCE, auth bypass)
- **High** – exploitable with low effort (e.g. SQLi, SSRF, hardcoded secrets)
- **Medium** – exploitable under certain conditions (e.g. XSS, IDOR)
- **Low** – defence-in-depth improvements
- **Informational** – observations with no direct exploit path

## Summary
One-paragraph overall security assessment.

If no security issues are found, state "✅ No security issues identified in
this diff." under ## Security Findings and provide a brief clean-bill summary.

Common categories to check (not exhaustive):
- Injection (SQL, command, template, LDAP)
- Broken authentication / insecure session management
- Sensitive data exposure (hardcoded secrets, tokens, PII in logs)
- Insecure direct object references (IDOR) / missing authorisation checks
- Cross-site scripting (XSS) / cross-site request forgery (CSRF)
- Insecure dependencies / unsafe imports
- Input validation and output encoding gaps
- Insecure cryptographic usage
- Race conditions / TOCTOU issues

```diff
{diff}
```
"""


def main() -> None:
    diff = sys.stdin.read()
    if not diff.strip():
        print("No diff found. Skipping security check.")
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
