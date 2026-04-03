# Getting Started

This guide covers everything you need to run **graphql-ms** locally, build the project, and execute the test suite.

---

## Prerequisites

| Requirement | Minimum version |
|---|---|
| Java | 17 |
| Maven | 3.9 |
| MongoDB | 6 (local or Docker) |

Start a local MongoDB instance with Docker:

```bash
docker run -d -p 27017:27017 --name mongo mongo:6
```

---

## Running the Service

```bash
mvn spring-boot:run
```

The service starts on **http://localhost:8080**.

| Endpoint | Description |
|---|---|
| `POST /graphql` | GraphQL API endpoint |
| `GET /graphiql` | Interactive browser IDE |

On the first startup, `DataInitializer` automatically seeds the database with sample authors, categories, and posts.

---

## Building

```bash
mvn package
```

This compiles the source, runs all tests, and produces a runnable JAR at `target/graphql-ms-*.jar`.

Run the packaged JAR:

```bash
java -jar target/graphql-ms-*.jar
```

---

## Running Tests

```bash
mvn test
```

The test suite does **not** require a running MongoDB instance — the integration tests use the [Flapdoodle embedded MongoDB](https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo) library.

| Test class | Type | What it covers |
|---|---|---|
| `DepthResolverServiceTest` | Unit | Single/many relations at various depths, null safety, max-depth cap |
| `GraphQlIntegrationTest` | GraphQL slice | Query/mutation behaviour, `depth=0` vs `depth=1` resolution |

---

## Configuration

Configuration is in `src/main/resources/application.yml`.

| Property | Default | Description |
|---|---|---|
| `spring.data.mongodb.uri` | `mongodb://localhost:27017/graphqlms` | MongoDB connection URI |
| `spring.graphql.graphiql.enabled` | `true` | Enable/disable the GraphiQL browser IDE |
| `spring.graphql.path` | `/graphql` | GraphQL HTTP endpoint path |
| `server.port` | `8080` | HTTP port |

Override any property at runtime with a system property:

```bash
java -jar target/graphql-ms-*.jar \
  --spring.data.mongodb.uri=mongodb://mongo-host:27017/graphqlms \
  --server.port=9090
```

---

> Back to [documentation index](README.md)
