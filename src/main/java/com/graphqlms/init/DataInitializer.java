package com.graphqlms.init;

import com.graphqlms.domain.author.Author;
import com.graphqlms.domain.author.AuthorRepository;
import com.graphqlms.domain.category.Category;
import com.graphqlms.domain.category.CategoryRepository;
import com.graphqlms.domain.post.Post;
import com.graphqlms.domain.post.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Loads sample data into MongoDB on startup (active unless the "test" profile is used).
 *
 * <p>Creates two authors, three categories and four posts that reference them so you can
 * immediately explore depth-based resolution through GraphiQL at
 * {@code http://localhost:8080/graphiql}.</p>
 */
@Component
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;

    public DataInitializer(AuthorRepository authorRepository,
                           CategoryRepository categoryRepository,
                           PostRepository postRepository) {
        this.authorRepository = authorRepository;
        this.categoryRepository = categoryRepository;
        this.postRepository = postRepository;
    }

    @Override
    public void run(String... args) {
        if (postRepository.count() > 0) {
            log.info("Sample data already present – skipping initialization.");
            return;
        }

        log.info("Loading sample data...");

        // Authors
        Author alice = authorRepository.save(new Author("Alice Smith", "alice@example.com",
                "Senior software engineer passionate about distributed systems."));
        Author bob = authorRepository.save(new Author("Bob Jones", "bob@example.com",
                "DevOps practitioner and cloud architect."));

        // Categories
        Category tech = categoryRepository.save(new Category("Technology",
                "Articles about software, hardware and emerging tech."));
        Category graphql = categoryRepository.save(new Category("GraphQL",
                "Everything about GraphQL APIs and best practices."));
        Category mongodb = categoryRepository.save(new Category("MongoDB",
                "Tips and patterns for working with MongoDB."));

        // Posts
        savePost("Getting started with Spring for GraphQL",
                "Spring for GraphQL provides a powerful way to build GraphQL APIs in Java...",
                alice.getId(), List.of(tech.getId(), graphql.getId()));

        savePost("Depth-based relation resolution in MongoDB",
                "Payload CMS popularised the depth concept for MongoDB documents...",
                alice.getId(), List.of(mongodb.getId(), graphql.getId()));

        savePost("Deploying Spring Boot on Kubernetes",
                "A step-by-step guide to packaging and deploying your Spring Boot app...",
                bob.getId(), List.of(tech.getId()));

        savePost("MongoDB schema design patterns",
                "Explore the most common schema design patterns for MongoDB collections...",
                bob.getId(), List.of(mongodb.getId()));

        log.info("Sample data loaded: 2 authors, 3 categories, 4 posts.");
    }

    private void savePost(String title, String content, String authorId, List<String> categoryIds) {
        Post post = new Post();
        post.setTitle(title);
        post.setContent(content);
        post.setAuthorId(authorId);
        post.setCategoryIds(categoryIds);
        post.setCreatedAt(LocalDateTime.now().toString());
        postRepository.save(post);
    }
}
