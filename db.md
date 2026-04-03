# GraphQL MS Database Schema

MongoDB schema for graphql-ms Spring Boot application

## Collections Overview

| Collection   | Purpose                    | Key Fields                        |
|--------------|----------------------------|-----------------------------------|
| `authors`    | Blog post authors           | name, email (unique), bio        |
| `categories` | Post categories/tags       | name (unique), description       |
| `posts`      | Blog posts                 | title, content, authorId, categoryIds, createdAt |

---

## authors

### Schema

```json
{
  "_id": "ObjectId",
  "name": "string (required, 1-255 chars)",
  "email": "string (unique, sparse, valid email)",
  "bio": "string (max 2000 chars)"
}
```

### Indexes

```javascript
db.authors.createIndex({ email: 1 }, { unique: true, sparse: true })
```

### Sample Documents

```json
{
  "_id": "ObjectId('...')",
  "name": "Alice Smith",
  "email": "alice@example.com",
  "bio": "Senior software engineer passionate about distributed systems."
}
```

---

## categories

### Schema

```json
{
  "_id": "ObjectId",
  "name": "string (required, 1-100 chars, unique)",
  "description": "string (max 500 chars)"
}
```

### Indexes

```javascript
db.categories.createIndex({ name: 1 }, { unique: true })
```

### Sample Documents

```json
{
  "_id": "ObjectId('...')",
  "name": "Technology",
  "description": "Articles about software, hardware and emerging tech."
}
```

---

## posts

### Schema

```json
{
  "_id": "ObjectId",
  "title": "string (required, 1-500 chars)",
  "content": "string (max 50000 chars)",
  "authorId": "string (required, reference to authors._id)",
  "categoryIds": ["string"] (references to categories._id),
  "createdAt": "string (ISO-8601 timestamp)"
}
```

### Indexes

```javascript
db.posts.createIndex({ authorId: 1 })
db.posts.createIndex({ categoryIds: 1 })
db.posts.createIndex({ createdAt: -1 })
```

### Relationships

| Field         | Target Collection | Type         | Description                       |
|---------------|-------------------|--------------|-----------------------------------|
| `authorId`    | `authors`         | many-to-one  | Each post has exactly one author  |
| `categoryIds` | `categories`      | many-to-many | Each post can have many categories|

### Sample Documents

```json
{
  "_id": "ObjectId('...')",
  "title": "Getting started with Spring for GraphQL",
  "content": "Spring for GraphQL provides a powerful way to build GraphQL APIs in Java...",
  "authorId": "ObjectId('...')",
  "categoryIds": ["ObjectId('...')", "ObjectId('...')"],
  "createdAt": "2024-01-15T10:30:00"
}
```

---

## Depth Resolution Pattern

The `@Transient` fields (`author`, `categories`) are **NOT stored** in MongoDB. They are resolved at query time by the `DepthResolverService`:

| Query Depth | `author` | `categories` |
|-------------|----------|---------------|
| 0           | `null`   | `null`        |
| 1+          | populated | populated     |

---

## JSON Schema (MongoDB 4.2+)

```json
{
  "$jsonSchema": {
    "bsonType": "object",
    "collections": {
      "authors": {
        "required": ["name"],
        "properties": {
          "_id": { "bsonType": "objectId" },
          "name": { "bsonType": "string", "minLength": 1, "maxLength": 255 },
          "email": { "bsonType": "string" },
          "bio": { "bsonType": "string", "maxLength": 2000 }
        }
      },
      "categories": {
        "required": ["name"],
        "properties": {
          "_id": { "bsonType": "objectId" },
          "name": { "bsonType": "string", "minLength": 1, "maxLength": 100 },
          "description": { "bsonType": "string", "maxLength": 500 }
        }
      },
      "posts": {
        "required": ["title", "authorId"],
        "properties": {
          "_id": { "bsonType": "objectId" },
          "title": { "bsonType": "string", "minLength": 1, "maxLength": 500 },
          "content": { "bsonType": "string", "maxLength": 50000 },
          "authorId": { "bsonType": "string" },
          "categoryIds": { "bsonType": "array", "items": { "bsonType": "string" } },
          "createdAt": { "bsonType": "string" }
        }
      }
    }
  }
}
```

---

## Quick Setup Commands

```javascript
// Create collections with validators (MongoDB 4.2+)
db.createCollection("authors", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["name"],
      properties: {
        name: { bsonType: "string" },
        email: { bsonType: "string" },
        bio: { bsonType: "string" }
      }
    }
  }
});

db.createCollection("categories", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["name"],
      properties: {
        name: { bsonType: "string" },
        description: { bsonType: "string" }
      }
    }
  }
});

db.createCollection("posts", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["title", "authorId"],
      properties: {
        title: { bsonType: "string" },
        content: { bsonType: "string" },
        authorId: { bsonType: "string" },
        categoryIds: { bsonType: "array", items: { bsonType: "string" } },
        createdAt: { bsonType: "string" }
      }
    }
  }
});

// Create indexes
db.authors.createIndex({ email: 1 }, { unique: true, sparse: true });
db.categories.createIndex({ name: 1 }, { unique: true });
db.posts.createIndex({ authorId: 1 });
db.posts.createIndex({ categoryIds: 1 });
db.posts.createIndex({ createdAt: -1 });
```
