# graphql-ms

A GraphQL microservice with AI-powered SDLC automation via Claude agents.

---

## 🤖 Claude AI Agents

Four Claude-powered GitHub Actions agents are included to automate key
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

### 4. AI Lint & Dependency Check (`ai-lint-deps-check.yml`)

A **pre-merge production gate** that runs only on PRs targeting `main`,
`master`, or `production`. It fails the workflow and blocks merging when
blocking issues are found. Reports cover:

- **Lint & Code Style Issues** — formatting, naming conventions, unused
  imports, missing docstrings, and overly complex expressions.
- **Dependency Validation** — new dependencies, overly broad version ranges
  (`*`, `LATEST`), known-vulnerable packages, duplicate declarations,
  unused dependencies, and dev deps accidentally added to production scope.
- **Pre-Merge Checklist** — a ✅/⚠️/❌ table summarising readiness for
  production.
- **Verdict** — `PASS`, `PASS WITH WARNINGS`, or `BLOCK`.
  A `BLOCK` verdict causes the workflow job to exit with a non-zero status,
  preventing the PR from being merged when branch protection is enabled.

---

## ⚙️ Setup

1. **Add your Anthropic API key** to the repository's GitHub Actions secrets:

   ```
   Settings → Secrets and variables → Actions → New repository secret
   Name:  ANTHROPIC_API_KEY
   Value: sk-ant-…
   ```

2. Push code — the AI Code Review, Documentation Update, and Security Check
   agents will run automatically on every pull request. The Lint &
   Dependency Check runs on PRs targeting `main`, `master`, or
   `production`, where it can **block merging** (via a failing status
   check) when a `BLOCK` verdict is returned.

   To enforce the gate, enable branch protection on your production branch:
   ```
   Settings → Branches → Add rule → Require status checks to pass before merging
   Required check: Claude Lint & Dependency Validation
   ```

---

## Project Structure

```
.github/
├── scripts/
│   ├── code_review.py      # Claude code-review agent script
│   ├── docs_update.py      # Claude documentation advisor script
│   ├── security_check.py   # Claude security analysis script
│   └── lint_deps_check.py  # Claude lint & dependency validation script
└── workflows/
    ├── ai-code-review.yml       # GH Actions: code review on PR
    ├── ai-docs-update.yml       # GH Actions: documentation advice on PR
    ├── ai-security-check.yml    # GH Actions: security scan on PR
    └── ai-lint-deps-check.yml   # GH Actions: lint & dep gate (main/master/production only)
```
A Spring Boot 3 microservice **POC / accelerator** that exposes a MongoDB-backed data model through a GraphQL API, with a **Payload-CMS-style `depth` argument** for controlling how deeply referenced documents are resolved at query time.

---

## ✨ Key Features

| Feature | Details |
|---|---|
| **GraphQL API** | Full CRUD for `Post`, `Author`, and `Category` via Spring for GraphQL |
| **MongoDB** | Spring Data MongoDB repositories |
| **Depth-based relation resolution** | Pass `depth=N` to any query to control how many levels of references are populated |
| **Reusable accelerator** | `@RelationField` annotation + `DepthResolverService` work with *any* domain entity |
| **GraphiQL UI** | Interactive browser IDE at `http://localhost:8080/graphiql` |
| **Sample data** | Automatically loaded on first startup |

---

## 📚 Documentation

Detailed documentation lives in the [`docs/`](docs/README.md) folder:

| Document | Description |
|---|---|
| [Getting Started](docs/getting-started.md) | Prerequisites, running, building, and testing |
| [Architecture](docs/architecture.md) | System design, module layout, and package structure |
| [Depth Resolution](docs/depth-resolution.md) | The `@RelationField` + `DepthResolverService` accelerator pattern |
| [GraphQL API Reference](docs/graphql-api.md) | Full schema, queries, mutations, and examples |

---

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.9+
- MongoDB running locally on port `27017`  
  `docker run -d -p 27017:27017 --name mongo mongo:6`

### Run

```bash
mvn spring-boot:run
```

Open **http://localhost:8080/graphiql** in your browser.

---

## 🔍 Depth-Based Relation Resolution

Inspired by [Payload CMS's `depth` option](https://payloadcms.com/docs/queries/depth), every collection query accepts an optional `depth` argument:

| `depth` | Behaviour |
|---|---|
| `0` | Returns raw IDs only; relation fields (`author`, `categories`) are `null` |
| `1` | Resolves direct relations (default) |
| `2` | Resolves relations of relations |
| `N` | Continues recursively up to `N` levels (capped at 10) |

### Example

```graphql
# depth=0: only raw IDs, no embedded documents
query {
  posts(depth: 0) {
    id
    title
    authorId        # present
    author { id }   # null
  }
}

# depth=1: author and categories are fully resolved
query {
  posts(depth: 1) {
    id
    title
    author {
      id
      name
      email
    }
    categories {
      id
      name
    }
  }
}

# depth=2: nested relations of resolved documents are also populated
query {
  post(id: "...", depth: 2) {
    title
    author {
      name
      # if Author itself had @RelationField fields they would be resolved here
    }
  }
}
```

---

## 🏗️ Architecture

```
src/main/java/com/graphqlms/
├── GraphqlMsApplication.java
├── core/
│   ├── annotation/
│   │   └── RelationField.java          ← mark a field as a MongoDB reference
│   └── resolver/
│       └── DepthResolverService.java   ← generic depth-aware resolver (the accelerator core)
├── domain/
│   ├── author/     Author, AuthorRepository, AuthorController, AuthorInput
│   ├── category/   Category, CategoryRepository, CategoryController, CategoryInput
│   └── post/       Post, PostRepository, PostController, PostInput
└── init/
    └── DataInitializer.java            ← loads sample data on startup
```

### How to add depth resolution to any new entity

1. Store reference IDs as `String` or `List<String>` fields in your `@Document` class.
2. Add a `@Transient @RelationField(...)` field for the resolved object(s):

```java
@Document(collection = "articles")
public class Article {

    @Id private String id;
    private String authorId;   // stored in MongoDB

    @Transient
    @RelationField(idField = "authorId", targetType = Author.class)
    private Author author;     // populated at query time

    private List<String> tagIds;

    @Transient
    @RelationField(idField = "tagIds", targetType = Tag.class, many = true)
    private List<Tag> tags;
}
```

3. Inject `DepthResolverService` in your controller and call `resolve(entity, depth)`.

---

## 📋 GraphQL Schema (summary)

```graphql
type Query {
  posts(depth: Int = 1): [Post!]!
  post(id: ID!, depth: Int = 1): Post
  authors(depth: Int = 1): [Author!]!
  author(id: ID!, depth: Int = 1): Author
  categories(depth: Int = 1): [Category!]!
  category(id: ID!, depth: Int = 1): Category
}

type Mutation {
  createPost(input: PostInput!): Post!
  updatePost(id: ID!, input: PostInput!): Post
  deletePost(id: ID!): Boolean!
  # … same pattern for Author and Category
}
```

Full schema: [`src/main/resources/graphql/schema.graphqls`](src/main/resources/graphql/schema.graphqls)

---

## 🧪 Tests

```bash
mvn test
```

- **`DepthResolverServiceTest`** – 12 unit tests covering single/many relations at various depths, null safety, and the max-depth cap.
- **`GraphQlControllerTest`** – 11 GraphQL slice tests covering query/mutation behaviour and depth=0 vs depth=1 resolution.

---

## ⚙️ Configuration

| Property | Default | Description |
|---|---|---|
| `spring.data.mongodb.uri` | `mongodb://localhost:27017/graphqlms` | MongoDB connection URI |
| `spring.graphql.graphiql.enabled` | `true` | Enable/disable GraphiQL browser IDE |
| `spring.graphql.path` | `/graphql` | GraphQL endpoint path |
## Branch Protection Rules

Direct pushes to `main` are not allowed. All changes must go through a pull request and receive **at least one approving review** before merging.

### Setting Up Branch Protection (Repository Admin Required)

1. Go to **Settings → Branches** in this repository.
2. Under **Branch protection rules**, click **Add rule**.
3. Set **Branch name pattern** to `main`.
4. Enable the following options:
   - ✅ **Require a pull request before merging**
     - ✅ **Require approvals** → set to `1`
     - ✅ **Dismiss stale pull request approvals when new commits are pushed**
     - ✅ **Require review from Code Owners** (uses `.github/CODEOWNERS`)
   - ✅ **Require status checks to pass before merging** *(if CI is configured)*
   - ✅ **Do not allow bypassing the above settings**
5. Click **Save changes**.

### Code Owners

The `.github/CODEOWNERS` file defines who must review pull requests. Any PR modifying files in this repo requires approval from a member of `@impondesk` before it can be merged.
