package com.graphqlms.domain.author;

import com.graphqlms.core.resolver.DepthResolverService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * GraphQL controller for Author queries and mutations.
 */
@Controller
public class AuthorController {

    private final AuthorRepository authorRepository;
    private final DepthResolverService depthResolverService;

    public AuthorController(AuthorRepository authorRepository,
                            DepthResolverService depthResolverService) {
        this.authorRepository = authorRepository;
        this.depthResolverService = depthResolverService;
    }

    @QueryMapping
    public List<Author> authors(@Argument Integer depth) {
        int d = depth != null ? depth : 1;
        return depthResolverService.resolveAll(authorRepository.findAll(), d);
    }

    @QueryMapping
    public Author author(@Argument String id, @Argument Integer depth) {
        int d = depth != null ? depth : 1;
        return authorRepository.findById(id)
                .map(a -> depthResolverService.resolve(a, d))
                .orElse(null);
    }

    @MutationMapping
    public Author createAuthor(@Argument AuthorInput input) {
        Author author = new Author(input.getName(), input.getEmail(), input.getBio());
        return authorRepository.save(author);
    }

    @MutationMapping
    public Author updateAuthor(@Argument String id, @Argument AuthorInput input) {
        return authorRepository.findById(id).map(author -> {
            if (input.getName() != null) author.setName(input.getName());
            if (input.getEmail() != null) author.setEmail(input.getEmail());
            if (input.getBio() != null) author.setBio(input.getBio());
            return authorRepository.save(author);
        }).orElse(null);
    }

    @MutationMapping
    public boolean deleteAuthor(@Argument String id) {
        if (!authorRepository.existsById(id)) {
            return false;
        }
        authorRepository.deleteById(id);
        return true;
    }
}
