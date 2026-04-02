package com.graphqlms.core.resolver;

import com.graphqlms.core.annotation.RelationField;
import com.graphqlms.domain.author.Author;
import com.graphqlms.domain.category.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DepthResolverService}.
 *
 * <p>Uses Mockito to stub {@link MongoTemplate} so no real MongoDB connection is required.</p>
 */
class DepthResolverServiceTest {

    private MongoTemplate mongoTemplate;
    private DepthResolverService resolver;

    @BeforeEach
    void setUp() {
        mongoTemplate = mock(MongoTemplate.class);
        resolver = new DepthResolverService(mongoTemplate);
    }

    // ------------------------------------------------------------------
    // depth = 0
    // ------------------------------------------------------------------

    @Test
    void resolve_depth0_doesNotPopulateRelations() {
        Author author = new Author("Alice", "alice@example.com", "bio");
        author.setId("a1");

        SimplePost post = new SimplePost();
        post.id = "p1";
        post.title = "Test Post";
        post.authorId = "a1";

        resolver.resolve(post, 0);

        assertThat(post.author).isNull();
    }

    @Test
    void resolve_nullEntity_returnsNull() {
        Object result = resolver.resolve(null, 1);
        assertThat(result).isNull();
    }

    // ------------------------------------------------------------------
    // depth = 1 – single relation
    // ------------------------------------------------------------------

    @Test
    void resolve_depth1_populatesSingleRelation() {
        Author author = new Author("Alice", "alice@example.com", "bio");
        author.setId("a1");
        when(mongoTemplate.findById("a1", Author.class)).thenReturn(author);

        SimplePost post = new SimplePost();
        post.id = "p1";
        post.title = "Test Post";
        post.authorId = "a1";

        resolver.resolve(post, 1);

        assertThat(post.author).isNotNull();
        assertThat(post.author.getName()).isEqualTo("Alice");
    }

    @Test
    void resolve_depth1_nullIdField_leavesRelationNull() {
        SimplePost post = new SimplePost();
        post.id = "p1";
        post.title = "Test Post";
        post.authorId = null;  // no author

        resolver.resolve(post, 1);

        assertThat(post.author).isNull();
    }

    // ------------------------------------------------------------------
    // depth = 1 – many relation
    // ------------------------------------------------------------------

    @Test
    void resolve_depth1_populatesManyRelation() {
        Category tech = new Category("Technology", "Tech articles");
        tech.setId("c1");
        Category graphql = new Category("GraphQL", "GraphQL articles");
        graphql.setId("c2");

        when(mongoTemplate.findById("c1", Category.class)).thenReturn(tech);
        when(mongoTemplate.findById("c2", Category.class)).thenReturn(graphql);

        PostWithCategories post = new PostWithCategories();
        post.id = "p1";
        post.title = "Test Post";
        post.categoryIds = Arrays.asList("c1", "c2");

        resolver.resolve(post, 1);

        assertThat(post.categories).hasSize(2);
        assertThat(post.categories).extracting(Category::getName)
                .containsExactlyInAnyOrder("Technology", "GraphQL");
    }

    @Test
    void resolve_depth1_emptyCategoryIds_leavesListNull() {
        PostWithCategories post = new PostWithCategories();
        post.id = "p1";
        post.title = "Test Post";
        post.categoryIds = List.of();

        resolver.resolve(post, 1);

        assertThat(post.categories).isNull();
    }

    // ------------------------------------------------------------------
    // depth = 2 – nested (recursive) resolution
    // ------------------------------------------------------------------

    @Test
    void resolve_depth2_resolvesNestedRelations() {
        // Author itself references a Profile
        UserProfile profile = new UserProfile();
        profile.id = "prof1";
        profile.bio = "Extended bio";

        AuthorWithProfile author = new AuthorWithProfile();
        author.id = "a1";
        author.name = "Alice";
        author.profileId = "prof1";

        when(mongoTemplate.findById("a1", AuthorWithProfile.class)).thenReturn(author);
        when(mongoTemplate.findById("prof1", UserProfile.class)).thenReturn(profile);

        PostWithAuthorWithProfile post = new PostWithAuthorWithProfile();
        post.id = "p1";
        post.authorId = "a1";

        resolver.resolve(post, 2);

        assertThat(post.author).isNotNull();
        assertThat(post.author.name).isEqualTo("Alice");
        assertThat(post.author.profile).isNotNull();
        assertThat(post.author.profile.bio).isEqualTo("Extended bio");
    }

    @Test
    void resolve_depth1_doesNotResolveNestedRelations() {
        AuthorWithProfile author = new AuthorWithProfile();
        author.id = "a1";
        author.name = "Alice";
        author.profileId = "prof1";

        when(mongoTemplate.findById("a1", AuthorWithProfile.class)).thenReturn(author);
        // profile lookup should NOT happen at depth=1

        PostWithAuthorWithProfile post = new PostWithAuthorWithProfile();
        post.id = "p1";
        post.authorId = "a1";

        resolver.resolve(post, 1);

        assertThat(post.author).isNotNull();
        assertThat(post.author.profile).isNull();  // not resolved at depth=1
    }

    // ------------------------------------------------------------------
    // resolveAll
    // ------------------------------------------------------------------

    @Test
    void resolveAll_resolvesEveryEntityInList() {
        Author alice = new Author("Alice", "alice@example.com", "bio");
        alice.setId("a1");
        Author bob = new Author("Bob", "bob@example.com", "bio");
        bob.setId("a2");
        when(mongoTemplate.findById("a1", Author.class)).thenReturn(alice);
        when(mongoTemplate.findById("a2", Author.class)).thenReturn(bob);

        SimplePost p1 = new SimplePost();
        p1.id = "p1";
        p1.authorId = "a1";

        SimplePost p2 = new SimplePost();
        p2.id = "p2";
        p2.authorId = "a2";

        List<SimplePost> posts = Arrays.asList(p1, p2);
        resolver.resolveAll(posts, 1);

        assertThat(p1.author.getName()).isEqualTo("Alice");
        assertThat(p2.author.getName()).isEqualTo("Bob");
    }

    @Test
    void resolveAll_nullList_returnsNull() {
        assertThat(resolver.resolveAll(null, 1)).isNull();
    }

    @Test
    void resolveAll_emptyList_returnsEmptyList() {
        List<SimplePost> empty = List.of();
        assertThat(resolver.resolveAll(empty, 1)).isEmpty();
    }

    // ------------------------------------------------------------------
    // Max-depth cap
    // ------------------------------------------------------------------

    @Test
    void resolve_depthExceedingMax_isCappedAtMax() {
        // As long as this does not throw or loop indefinitely, the cap works.
        SimplePost post = new SimplePost();
        post.id = "p1";
        post.authorId = null;

        // Should not throw even with a huge depth value
        resolver.resolve(post, 999);
        assertThat(post.author).isNull();
    }

    // ------------------------------------------------------------------
    // Inner test fixtures (lightweight POJOs)
    // ------------------------------------------------------------------

    /** Minimal post with a single Author relation. */
    @Document(collection = "posts")
    static class SimplePost {
        @Id
        String id;
        String title;
        String authorId;

        @Transient
        @RelationField(idField = "authorId", targetType = Author.class)
        Author author;
    }

    /** Post with a many Category relation. */
    @Document(collection = "posts")
    static class PostWithCategories {
        @Id
        String id;
        String title;
        List<String> categoryIds;

        @Transient
        @RelationField(idField = "categoryIds", targetType = Category.class, many = true)
        List<Category> categories;
    }

    /** Author that itself has a relation to a UserProfile (for depth=2 tests). */
    @Document(collection = "authors")
    static class AuthorWithProfile {
        @Id
        String id;
        String name;
        String profileId;

        @Transient
        @RelationField(idField = "profileId", targetType = UserProfile.class)
        UserProfile profile;
    }

    /** Supplemental profile document used in depth=2 tests. */
    @Document(collection = "profiles")
    static class UserProfile {
        @Id
        String id;
        String bio;
    }

    /** Post that references an AuthorWithProfile (for depth=2 tests). */
    @Document(collection = "posts")
    static class PostWithAuthorWithProfile {
        @Id
        String id;
        String authorId;

        @Transient
        @RelationField(idField = "authorId", targetType = AuthorWithProfile.class)
        AuthorWithProfile author;
    }
}
