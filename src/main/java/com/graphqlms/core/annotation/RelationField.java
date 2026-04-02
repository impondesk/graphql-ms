package com.graphqlms.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as a relation to another MongoDB collection.
 *
 * <p>The {@link com.graphqlms.core.resolver.DepthResolverService} uses this annotation
 * to discover and resolve references when the query {@code depth} argument is greater than
 * zero — mirroring the <em>depth</em> concept from Payload CMS.</p>
 *
 * <p>Example usage on a single-document relation:
 * <pre>{@code
 * private String authorId;   // stores the reference ID
 *
 * @RelationField(idField = "authorId", targetType = Author.class)
 * @Transient
 * private Author author;     // populated by DepthResolverService
 * }</pre>
 *
 * <p>Example usage on a multi-document relation:
 * <pre>{@code
 * private List<String> categoryIds;
 *
 * @RelationField(idField = "categoryIds", targetType = Category.class, many = true)
 * @Transient
 * private List<Category> categories;
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RelationField {

    /**
     * Name of the sibling field that holds the reference ID(s).
     * For single relations this should be a {@code String} field;
     * for {@link #many()} = {@code true} it should be a {@code List<String>} field.
     */
    String idField();

    /**
     * The entity class of the referenced document.
     */
    Class<?> targetType();

    /**
     * Whether this relation is a one-to-many (list) relation.
     * Defaults to {@code false} (single / many-to-one).
     */
    boolean many() default false;
}
