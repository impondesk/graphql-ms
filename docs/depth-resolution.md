# Depth Resolution

The **depth resolver** is the central accelerator pattern of graphql-ms. It lets GraphQL clients control — with a single integer argument — how many levels of MongoDB document references are eagerly resolved in a response.

Inspired by [Payload CMS's `depth` option](https://payloadcms.com/docs/queries/depth).

---

## Concept

MongoDB stores references between documents as plain ID strings. For example, a `Post` stores the author's `_id` in an `authorId` field. At query time, the caller may or may not need the full `Author` document embedded in the response.

Rather than hard-coding eager or lazy loading, every query exposes a `depth` argument:

| `depth` | Behaviour |
|---|---|
| `0` | No resolution — relation fields (`author`, `categories`) are `null`; only raw IDs are returned |
| `1` | Resolve direct relations (default) |
| `2` | Resolve relations of relations |
| `N` | Continues recursively up to `N` levels (capped at `10`) |

---

## Components

### `@RelationField` annotation

`com.graphqlms.core.annotation.RelationField`

Placed on a `@Transient` field in a `@Document` class to declare a MongoDB relation:

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RelationField {
    String  idField();              // name of the sibling field holding the ID(s)
    Class<?>targetType();           // entity class of the referenced document
    boolean many() default false;   // true = one-to-many (List), false = many-to-one / single
}
```

### `DepthResolverService`

`com.graphqlms.core.resolver.DepthResolverService`

A Spring `@Service` that walks an entity's `@RelationField`-annotated fields using Java reflection and fetches referenced documents via `MongoTemplate`.

Key methods:

```java
// Resolve a single entity
public <T> T resolve(T entity, int depth)

// Convenience: resolve a list of entities
public <T> List<T> resolveAll(List<T> entities, int depth)
```

The service caps the depth at `MAX_ALLOWED_DEPTH = 10` regardless of what the caller requests, preventing accidental runaway recursion on highly connected document graphs.

---

## Usage in Domain Entities

### Single (many-to-one) relation

```java
@Document(collection = "posts")
public class Post {

    private String authorId;   // stored in MongoDB

    @Transient
    @RelationField(idField = "authorId", targetType = Author.class)
    private Author author;     // populated by DepthResolverService when depth > 0
}
```

### One-to-many relation

```java
@Document(collection = "posts")
public class Post {

    private List<String> categoryIds;   // stored in MongoDB

    @Transient
    @RelationField(idField = "categoryIds", targetType = Category.class, many = true)
    private List<Category> categories;  // populated by DepthResolverService when depth > 0
}
```

---

## Usage in Controllers

Inject `DepthResolverService` and call it after loading the entity from the repository:

```java
@QueryMapping
public List<Post> posts(@Argument Integer depth) {
    int d = depth != null ? depth : 1;
    return depthResolverService.resolveAll(postRepository.findAll(), d);
}

@QueryMapping
public Post post(@Argument String id, @Argument Integer depth) {
    int d = depth != null ? depth : 1;
    return postRepository.findById(id)
            .map(p -> depthResolverService.resolve(p, d))
            .orElse(null);
}
```

---

## Adding Depth Resolution to a New Entity

1. Create a `@Document` class with `String` / `List<String>` ID fields.
2. Add `@Transient @RelationField(...)` fields for the resolved objects.
3. Create a Spring Data repository for the new entity.
4. In the GraphQL controller, inject `DepthResolverService` and call `resolve` / `resolveAll` after fetching from the repository.
5. Expose a `depth: Int = 1` argument in the GraphQL schema for the relevant queries.

No additional configuration is required — the resolver discovers `@RelationField` fields automatically at runtime.

---

## Extending to New Entity Example

```java
@Document(collection = "articles")
public class Article {

    @Id private String id;
    private String title;

    private String authorId;

    @Transient
    @RelationField(idField = "authorId", targetType = Author.class)
    private Author author;

    private List<String> tagIds;

    @Transient
    @RelationField(idField = "tagIds", targetType = Tag.class, many = true)
    private List<Tag> tags;
}
```

Controller:

```java
@Controller
public class ArticleController {

    private final ArticleRepository articleRepository;
    private final DepthResolverService depthResolverService;

    @QueryMapping
    public List<Article> articles(@Argument Integer depth) {
        int d = depth != null ? depth : 1;
        return depthResolverService.resolveAll(articleRepository.findAll(), d);
    }
}
```

Schema addition:

```graphql
type Query {
  articles(depth: Int = 1): [Article!]!
}
```

---

> Back to [documentation index](README.md)
