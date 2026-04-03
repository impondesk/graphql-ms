# GraphQL API Reference

The GraphQL endpoint is available at `POST http://localhost:8080/graphql`.  
Use the interactive GraphiQL IDE at `http://localhost:8080/graphiql` to explore and test queries.

---

## Schema Overview

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

  createAuthor(input: AuthorInput!): Author!
  updateAuthor(id: ID!, input: AuthorInput!): Author
  deleteAuthor(id: ID!): Boolean!

  createCategory(input: CategoryInput!): Category!
  updateCategory(id: ID!, input: CategoryInput!): Category
  deleteCategory(id: ID!): Boolean!
}
```

Full schema source: [`src/main/resources/graphql/schema.graphqls`](../src/main/resources/graphql/schema.graphqls)

---

## Types

### Post

```graphql
type Post {
  id: ID!
  title: String!
  content: String

  authorId: String          # raw stored reference ID (always present)
  author: Author            # resolved at depth ≥ 1

  categoryIds: [String!]    # raw stored reference IDs (always present)
  categories: [Category!]   # resolved at depth ≥ 1

  createdAt: String
}
```

### Author

```graphql
type Author {
  id: ID!
  name: String!
  email: String
  bio: String
}
```

### Category

```graphql
type Category {
  id: ID!
  name: String!
  description: String
}
```

---

## Input Types

```graphql
input PostInput {
  title: String!
  content: String
  authorId: String
  categoryIds: [String!]
}

input AuthorInput {
  name: String!
  email: String
  bio: String
}

input CategoryInput {
  name: String!
  description: String
}
```

---

## The `depth` Argument

Every collection query and single-document query accepts an optional `depth` argument (default `1`).

| Value | Effect |
|---|---|
| `depth: 0` | Only raw IDs returned; `author` and `categories` are `null` |
| `depth: 1` | Direct relations resolved (default) |
| `depth: 2` | Relations of relations resolved |
| `depth: N` | Recursive up to `N` levels; hard cap at `10` |

---

## Query Examples

### List all posts (default depth = 1)

```graphql
query {
  posts {
    id
    title
    author {
      id
      name
    }
    categories {
      id
      name
    }
  }
}
```

### List posts with raw IDs only (depth = 0)

```graphql
query {
  posts(depth: 0) {
    id
    title
    authorId
    categoryIds
  }
}
```

### Fetch a single post by ID

```graphql
query {
  post(id: "64abc123", depth: 1) {
    id
    title
    content
    createdAt
    author {
      name
      email
    }
    categories {
      name
    }
  }
}
```

### List all authors

```graphql
query {
  authors {
    id
    name
    email
    bio
  }
}
```

### List all categories

```graphql
query {
  categories {
    id
    name
    description
  }
}
```

---

## Mutation Examples

### Create a post

```graphql
mutation {
  createPost(input: {
    title: "My First Post"
    content: "Hello, world!"
    authorId: "64abc111"
    categoryIds: ["64abc222", "64abc333"]
  }) {
    id
    title
    createdAt
  }
}
```

### Update a post

```graphql
mutation {
  updatePost(id: "64abc123", input: {
    title: "Updated Title"
  }) {
    id
    title
  }
}
```

### Delete a post

```graphql
mutation {
  deletePost(id: "64abc123")
}
```

### Create an author

```graphql
mutation {
  createAuthor(input: {
    name: "Jane Doe"
    email: "jane@example.com"
    bio: "Staff writer"
  }) {
    id
    name
  }
}
```

### Create a category

```graphql
mutation {
  createCategory(input: {
    name: "Technology"
    description: "Posts about tech"
  }) {
    id
    name
  }
}
```

---

## Error Handling

- Querying a non-existent document by `id` returns `null` (not an error).
- `deletePost` / `deleteAuthor` / `deleteCategory` return `false` when the document does not exist.
- `updatePost` / `updateAuthor` / `updateCategory` return `null` when the document does not exist.

---

> Back to [documentation index](README.md)
