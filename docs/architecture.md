# Architecture

This document describes the system design, technology stack, and internal package structure of **graphql-ms**.

---

## Technology Stack

| Layer | Technology |
|---|---|
| Runtime | Java 17, Spring Boot 3 |
| API | Spring for GraphQL (`spring-boot-starter-graphql`) |
| Database | MongoDB via Spring Data MongoDB |
| Testing | JUnit 5, `@GraphQlTest` slice, Flapdoodle embedded MongoDB |
| Build | Maven 3.9 |

---

## High-Level Design

```
HTTP Client
     │
     ▼
┌──────────────────────────────────┐
│       Spring for GraphQL         │  /graphql  (HTTP POST)
│   (schema-first, auto-routing)   │  /graphiql (browser IDE)
└──────────────┬───────────────────┘
               │  @QueryMapping / @MutationMapping
               ▼
┌──────────────────────────────────┐
│   Domain Controllers             │
│   PostController                 │
│   AuthorController               │
│   CategoryController             │
└────────┬─────────────────────────┘
         │ Spring Data repositories
         │                          ┌──────────────────────┐
         │  DepthResolverService ◄──┤ @RelationField fields │
         │  (resolve references)    └──────────────────────┘
         ▼
┌──────────────────────────────────┐
│          MongoDB                 │
│  collections: posts, authors,    │
│               categories         │
└──────────────────────────────────┘
```

---

## Package Structure

```
src/main/java/com/graphqlms/
├── GraphqlMsApplication.java          Spring Boot entry point
│
├── core/
│   ├── annotation/
│   │   └── RelationField.java         Marks a field as a MongoDB relation reference
│   └── resolver/
│       └── DepthResolverService.java  Generic depth-aware relation resolver
│
├── domain/
│   ├── author/
│   │   ├── Author.java                @Document entity
│   │   ├── AuthorController.java      GraphQL controller
│   │   ├── AuthorInput.java           Mutation input record
│   │   └── AuthorRepository.java      Spring Data repository
│   ├── category/
│   │   ├── Category.java
│   │   ├── CategoryController.java
│   │   ├── CategoryInput.java
│   │   └── CategoryRepository.java
│   └── post/
│       ├── Post.java                  Uses @RelationField for author + categories
│       ├── PostController.java
│       ├── PostInput.java
│       └── PostRepository.java
│
└── init/
    └── DataInitializer.java           Loads sample data on first startup
```

---

## Request Lifecycle

1. A GraphQL HTTP request arrives at `POST /graphql`.
2. Spring for GraphQL parses the operation and routes it to the matching `@QueryMapping` or `@MutationMapping` method.
3. The controller reads the `depth` argument (default `1`) and calls the repository.
4. For queries, `DepthResolverService.resolve(entity, depth)` walks every `@RelationField`-annotated `@Transient` field and fetches the referenced document(s) from MongoDB up to `depth` levels deep.
5. The populated entity graph is serialised back to GraphQL JSON.

---

## Core Module: `core/`

The `core/` package is framework-agnostic and reusable across any domain:

- **`@RelationField`** — annotation placed on `@Transient` fields to declare a MongoDB reference. Carries the name of the ID field, the target entity type, and whether it is a one-to-many relation.
- **`DepthResolverService`** — uses Java reflection to discover `@RelationField` fields at runtime and populate them via `MongoTemplate`. Enforces a hard cap of `depth = 10` to prevent runaway recursion.

See [Depth Resolution](depth-resolution.md) for a detailed explanation.

---

## Branch Protection

Direct pushes to `main` are blocked. All changes must be submitted via a pull request and receive at least **one approving review** from a member of `@impondesk` (enforced via `.github/CODEOWNERS`).

---

> Back to [documentation index](README.md)
