package com.graphqlms.controller;

import com.graphqlms.core.resolver.DepthResolverService;
import com.graphqlms.domain.author.Author;
import com.graphqlms.domain.author.AuthorController;
import com.graphqlms.domain.author.AuthorRepository;
import com.graphqlms.domain.category.Category;
import com.graphqlms.domain.category.CategoryController;
import com.graphqlms.domain.category.CategoryRepository;
import com.graphqlms.domain.post.Post;
import com.graphqlms.domain.post.PostController;
import com.graphqlms.domain.post.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * GraphQL slice tests for Post, Author, and Category controllers.
 *
 * <p>Uses {@code @GraphQlTest} which loads only GraphQL infrastructure and the specified
 * controllers.  All MongoDB dependencies are replaced by Mockito mocks, so no database
 * connection is required.</p>
 */
@GraphQlTest({PostController.class, AuthorController.class, CategoryController.class})
@Import(DepthResolverService.class)
class GraphQlIntegrationTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockBean
    private PostRepository postRepository;

    @MockBean
    private AuthorRepository authorRepository;

    @MockBean
    private CategoryRepository categoryRepository;

    @MockBean
    private org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    private Author testAuthor;
    private Category testCategory;
    private Post testPost;

    @BeforeEach
    void setUp() {
        testAuthor = new Author("Test Author", "test@example.com", "A test author.");
        testAuthor.setId("author-1");

        testCategory = new Category("Test Category", "For testing.");
        testCategory.setId("cat-1");

        testPost = new Post();
        testPost.setId("post-1");
        testPost.setTitle("Test Post");
        testPost.setContent("Post content");
        testPost.setAuthorId("author-1");
        testPost.setCategoryIds(List.of("cat-1"));
        testPost.setCreatedAt("2024-01-01T00:00:00");

        when(postRepository.findAll()).thenReturn(List.of(testPost));
        when(postRepository.findById("post-1")).thenReturn(Optional.of(testPost));
        when(postRepository.findById("nonexistent")).thenReturn(Optional.empty());
        when(postRepository.existsById("post-1")).thenReturn(true);
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> {
            Post p = inv.getArgument(0);
            if (p.getId() == null) p.setId("new-post-id");
            return p;
        });

        when(authorRepository.findAll()).thenReturn(List.of(testAuthor));
        when(authorRepository.findById("author-1")).thenReturn(Optional.of(testAuthor));
        when(authorRepository.save(any(Author.class))).thenAnswer(inv -> {
            Author a = inv.getArgument(0);
            if (a.getId() == null) a.setId("new-author-id");
            return a;
        });

        when(categoryRepository.findAll()).thenReturn(List.of(testCategory));
        when(categoryRepository.findById("cat-1")).thenReturn(Optional.of(testCategory));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            if (c.getId() == null) c.setId("new-cat-id");
            return c;
        });

        // Depth resolver: stub the MongoTemplate for relation lookups
        when(mongoTemplate.findById("author-1", Author.class)).thenReturn(testAuthor);
        when(mongoTemplate.findById("cat-1", Category.class)).thenReturn(testCategory);
    }

    // ------------------------------------------------------------------
    // Query: posts (depth=0 – no relation resolution)
    // ------------------------------------------------------------------

    @Test
    void posts_depth0_authorAndCategoriesAreNull() {
        graphQlTester.document("""
                query {
                    posts(depth: 0) {
                        id
                        title
                        authorId
                        author { id name }
                        categoryIds
                        categories { id name }
                    }
                }
                """)
                .execute()
                .path("posts[0].title").entity(String.class).isEqualTo("Test Post")
                .path("posts[0].authorId").entity(String.class).isEqualTo("author-1")
                .path("posts[0].author").valueIsNull()
                .path("posts[0].categories").valueIsNull();
    }

    // ------------------------------------------------------------------
    // Query: posts (depth=1 – relations resolved)
    // ------------------------------------------------------------------

    @Test
    void posts_depth1_authorAndCategoriesAreResolved() {
        graphQlTester.document("""
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
                """)
                .execute()
                .path("posts[0].author.name").entity(String.class).isEqualTo("Test Author")
                .path("posts[0].author.email").entity(String.class).isEqualTo("test@example.com")
                .path("posts[0].categories[0].name").entity(String.class).isEqualTo("Test Category");
    }

    // ------------------------------------------------------------------
    // Query: post by ID
    // ------------------------------------------------------------------

    @Test
    void post_byId_found() {
        graphQlTester.document("""
                query {
                    post(id: "post-1", depth: 1) {
                        id
                        title
                        author { name }
                    }
                }
                """)
                .execute()
                .path("post.id").entity(String.class).isEqualTo("post-1")
                .path("post.title").entity(String.class).isEqualTo("Test Post")
                .path("post.author.name").entity(String.class).isEqualTo("Test Author");
    }

    @Test
    void post_byId_notFound_returnsNull() {
        graphQlTester.document("""
                query {
                    post(id: "nonexistent") {
                        id
                    }
                }
                """)
                .execute()
                .path("post").valueIsNull();
    }

    // ------------------------------------------------------------------
    // Query: authors
    // ------------------------------------------------------------------

    @Test
    void authors_returnsAllAuthors() {
        graphQlTester.document("""
                query {
                    authors {
                        id
                        name
                        email
                        bio
                    }
                }
                """)
                .execute()
                .path("authors[0].name").entity(String.class).isEqualTo("Test Author")
                .path("authors[0].email").entity(String.class).isEqualTo("test@example.com");
    }

    @Test
    void author_byId_returnsCorrectAuthor() {
        graphQlTester.document("""
                query {
                    author(id: "author-1") {
                        id
                        name
                    }
                }
                """)
                .execute()
                .path("author.name").entity(String.class).isEqualTo("Test Author");
    }

    // ------------------------------------------------------------------
    // Query: categories
    // ------------------------------------------------------------------

    @Test
    void categories_returnsAllCategories() {
        graphQlTester.document("""
                query {
                    categories {
                        id
                        name
                        description
                    }
                }
                """)
                .execute()
                .path("categories[0].name").entity(String.class).isEqualTo("Test Category");
    }

    // ------------------------------------------------------------------
    // Mutations
    // ------------------------------------------------------------------

    @Test
    void createPost_returnsCreatedPost() {
        graphQlTester.document("""
                mutation {
                    createPost(input: {
                        title: "New Post",
                        content: "Content",
                        authorId: "author-1",
                        categoryIds: ["cat-1"]
                    }) {
                        id
                        title
                        authorId
                    }
                }
                """)
                .execute()
                .path("createPost.title").entity(String.class).isEqualTo("New Post")
                .path("createPost.authorId").entity(String.class).isEqualTo("author-1");
    }

    @Test
    void createAuthor_returnsCreatedAuthor() {
        graphQlTester.document("""
                mutation {
                    createAuthor(input: {
                        name: "New Author",
                        email: "new@example.com",
                        bio: "New bio"
                    }) {
                        id
                        name
                        email
                    }
                }
                """)
                .execute()
                .path("createAuthor.name").entity(String.class).isEqualTo("New Author")
                .path("createAuthor.email").entity(String.class).isEqualTo("new@example.com");
    }

    @Test
    void createCategory_returnsCreatedCategory() {
        graphQlTester.document("""
                mutation {
                    createCategory(input: {
                        name: "New Category",
                        description: "New description"
                    }) {
                        id
                        name
                    }
                }
                """)
                .execute()
                .path("createCategory.name").entity(String.class).isEqualTo("New Category");
    }

    @Test
    void deletePost_existingId_returnsTrue() {
        graphQlTester.document("""
                mutation {
                    deletePost(id: "post-1")
                }
                """)
                .execute()
                .path("deletePost").entity(Boolean.class).isEqualTo(true);
    }
}

