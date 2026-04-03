#!/usr/bin/env python3
"""Claude-powered code review agent.

Reads a unified git diff from stdin, sends it to the Claude API,
and prints a structured Markdown code-review report to stdout.
"""

import os
import sys

import anthropic

_MAX_DIFF_CHARS = 50_000
_MODEL = "claude-opus-4-5"
_MAX_TOKENS = 4096

_PROMPT_TEMPLATE = """\
You are an expert software engineer performing a pull-request code review.
Analyse the following unified diff and produce a concise Markdown report with
these sections:

## Summary of Changes
A brief, plain-English overview of what the diff does.

## Potential Bugs / Issues
List any logic errors, edge-case problems, or incorrect behaviour.

## Code Quality & Maintainability
Suggestions for readability, naming, structure, and test coverage.

## Best Practices & Recommendations
Anything else worth improving (e.g. missing error handling, better patterns).

If a section has nothing to report, write "Nothing to report." rather than
omitting the section.

```diff
{diff}
```
"""


def main() -> None:
    diff = sys.stdin.read()
    if not diff.strip():
        print("No diff found. Skipping code review.")
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
