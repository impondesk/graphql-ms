package com.graphqlms.domain.post;

import com.graphqlms.core.resolver.DepthResolverService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.List;

/**
 * GraphQL controller for Post queries and mutations.
 *
 * <p>Each query accepts an optional {@code depth} argument (default 1) which is forwarded
 * to the {@link DepthResolverService} to control how deeply referenced documents
 * (author, categories) are populated in the response.  This mirrors the depth concept from
 * Payload CMS.</p>
 */
@Controller
public class PostController {

    private final PostRepository postRepository;
    private final DepthResolverService depthResolverService;

    public PostController(PostRepository postRepository,
                          DepthResolverService depthResolverService) {
        this.postRepository = postRepository;
        this.depthResolverService = depthResolverService;
    }

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------
    // Mutations
    // ------------------------------------------------------------------

    @MutationMapping
    public Post createPost(@Argument PostInput input) {
        Post post = new Post();
        post.setTitle(input.getTitle());
        post.setContent(input.getContent());
        post.setAuthorId(input.getAuthorId());
        post.setCategoryIds(input.getCategoryIds());
        post.setCreatedAt(LocalDateTime.now().toString());
        return postRepository.save(post);
    }

    @MutationMapping
    public Post updatePost(@Argument String id, @Argument PostInput input) {
        return postRepository.findById(id).map(post -> {
            if (input.getTitle() != null) post.setTitle(input.getTitle());
            if (input.getContent() != null) post.setContent(input.getContent());
            if (input.getAuthorId() != null) post.setAuthorId(input.getAuthorId());
            if (input.getCategoryIds() != null) post.setCategoryIds(input.getCategoryIds());
            return postRepository.save(post);
        }).orElse(null);
    }

    @MutationMapping
    public boolean deletePost(@Argument String id) {
        if (!postRepository.existsById(id)) {
            return false;
        }
        postRepository.deleteById(id);
        return true;
    }
}
