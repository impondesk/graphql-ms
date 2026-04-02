# graphql-ms

A GraphQL microservice with AI-powered SDLC automation via Claude agents.

---

## 🤖 Claude AI Agents

Three Claude-powered GitHub Actions agents are included to automate key
stages of the software development lifecycle. Each agent is triggered
automatically when a pull request is opened, updated, or reopened, and
posts its findings as a PR comment.

### 1. AI Code Review (`ai-code-review.yml`)

Reviews every pull request diff and produces a structured report covering:

- **Summary of Changes** — plain-English overview of what the PR does.
- **Potential Bugs / Issues** — logic errors and edge-case problems.
- **Code Quality & Maintainability** — readability, naming, and test coverage.
- **Best Practices & Recommendations** — missing error handling, better patterns.

### 2. AI Documentation Update (`ai-docs-update.yml`)

Analyses code changes and advises on documentation work needed:

- **What Changed (Documentation Perspective)** — impact on external behavior
  and public APIs.
- **Documentation Updates Required** — specific edits to README, docstrings,
  or other docs.
- **Suggested New Documentation** — docs that don't yet exist but should.
- **API / Interface Changes** — breaking/non-breaking changes that consumers
  must know about.

### 3. AI Security Check (`ai-security-check.yml`)

Scans the diff for security vulnerabilities, reporting each finding with
severity, location, description, and a concrete fix recommendation.
Categories checked include:

- Injection (SQL, command, template)
- Broken authentication / insecure session management
- Sensitive data exposure (hardcoded secrets, PII in logs)
- Insecure direct object references / missing authorisation checks
- XSS / CSRF
- Insecure dependencies and unsafe imports
- Input-validation and output-encoding gaps

---

## ⚙️ Setup

1. **Add your Anthropic API key** to the repository's GitHub Actions secrets:

   ```
   Settings → Secrets and variables → Actions → New repository secret
   Name:  ANTHROPIC_API_KEY
   Value: sk-ant-…
   ```

2. Push code — the three agents will run automatically on every pull request.

---

## Project Structure

```
.github/
├── scripts/
│   ├── code_review.py      # Claude code-review agent script
│   ├── docs_update.py      # Claude documentation advisor script
│   └── security_check.py   # Claude security analysis script
└── workflows/
    ├── ai-code-review.yml  # GH Actions: code review on PR
    ├── ai-docs-update.yml  # GH Actions: documentation advice on PR
    └── ai-security-check.yml # GH Actions: security scan on PR
```