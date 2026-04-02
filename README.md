# graphql-ms

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
