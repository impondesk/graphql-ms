package com.graphqlms.domain.category;

import com.graphqlms.core.resolver.DepthResolverService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * GraphQL controller for Category queries and mutations.
 */
@Controller
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final DepthResolverService depthResolverService;

    public CategoryController(CategoryRepository categoryRepository,
                              DepthResolverService depthResolverService) {
        this.categoryRepository = categoryRepository;
        this.depthResolverService = depthResolverService;
    }

    @QueryMapping
    public List<Category> categories(@Argument Integer depth) {
        int d = depth != null ? depth : 1;
        return depthResolverService.resolveAll(categoryRepository.findAll(), d);
    }

    @QueryMapping
    public Category category(@Argument String id, @Argument Integer depth) {
        int d = depth != null ? depth : 1;
        return categoryRepository.findById(id)
                .map(c -> depthResolverService.resolve(c, d))
                .orElse(null);
    }

    @MutationMapping
    public Category createCategory(@Argument CategoryInput input) {
        Category category = new Category(input.getName(), input.getDescription());
        return categoryRepository.save(category);
    }

    @MutationMapping
    public Category updateCategory(@Argument String id, @Argument CategoryInput input) {
        return categoryRepository.findById(id).map(category -> {
            if (input.getName() != null) category.setName(input.getName());
            if (input.getDescription() != null) category.setDescription(input.getDescription());
            return categoryRepository.save(category);
        }).orElse(null);
    }

    @MutationMapping
    public boolean deleteCategory(@Argument String id) {
        if (!categoryRepository.existsById(id)) {
            return false;
        }
        categoryRepository.deleteById(id);
        return true;
    }
}
