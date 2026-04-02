#!/usr/bin/env python3
"""Claude-powered documentation update agent.

Reads a unified git diff from stdin, sends it to the Claude API,
and prints Markdown documentation-update suggestions to stdout.
"""

import os
import sys

import anthropic

_MAX_DIFF_CHARS = 50_000
_MODEL = "claude-opus-4-5"
_MAX_TOKENS = 4096

_PROMPT_TEMPLATE = """\
You are a senior technical writer reviewing a pull request.
Based on the following code diff, produce a Markdown report with these sections:

## What Changed (Documentation Perspective)
A plain-English summary of the changes that affect external behaviour,
APIs, configuration, or public interfaces.

## Documentation Updates Required
Specific, actionable edits to README files, inline docstrings/comments,
or other documentation that *must* be updated to stay accurate.

## Suggested New Documentation
Any documentation that does not exist yet but should be added
(e.g. new API endpoints, configuration options, usage examples).

## API / Interface Changes
Breaking or non-breaking changes to public APIs, CLI flags, env vars,
or any contract with downstream consumers.

If a section has nothing to report, write "Nothing to report." rather than
omitting the section.

```diff
{diff}
```
"""


def main() -> None:
    diff = sys.stdin.read()
    if not diff.strip():
        print("No diff found. Skipping documentation update check.")
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
