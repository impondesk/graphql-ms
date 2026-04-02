package com.graphqlms.domain.post;

import com.graphqlms.core.annotation.RelationField;
import com.graphqlms.domain.author.Author;
import com.graphqlms.domain.category.Category;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * MongoDB document representing a blog post.
 *
 * <p>Demonstrates the depth-resolver accelerator pattern:
 * <ul>
 *   <li>{@code authorId} is stored in MongoDB; {@code author} is resolved at query time.</li>
 *   <li>{@code categoryIds} is stored in MongoDB; {@code categories} is resolved at query time.</li>
 * </ul>
 *
 * <p>When a GraphQL query specifies {@code depth = 0}, {@code author} and {@code categories}
 * remain {@code null}.  With {@code depth = 1} they are populated with the full documents.
 * With {@code depth = 2} those documents in turn have <em>their</em> relations resolved, etc.</p>
 */
@Document(collection = "posts")
public class Post {

    @Id
    private String id;

    private String title;

    private String content;

    /** Stored reference to the Author document. */
    private String authorId;

    /**
     * Resolved Author – populated by {@link com.graphqlms.core.resolver.DepthResolverService}
     * when depth &gt; 0.  {@code @Transient} prevents MongoDB from persisting this field.
     */
    @Transient
    @RelationField(idField = "authorId", targetType = Author.class)
    private Author author;

    /** Stored references to Category documents. */
    private List<String> categoryIds;

    /**
     * Resolved Categories – populated by {@link com.graphqlms.core.resolver.DepthResolverService}
     * when depth &gt; 0.
     */
    @Transient
    @RelationField(idField = "categoryIds", targetType = Category.class, many = true)
    private List<Category> categories;

    private String createdAt;

    public Post() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public Author getAuthor() { return author; }
    public void setAuthor(Author author) { this.author = author; }

    public List<String> getCategoryIds() { return categoryIds; }
    public void setCategoryIds(List<String> categoryIds) { this.categoryIds = categoryIds; }

    public List<Category> getCategories() { return categories; }
    public void setCategories(List<Category> categories) { this.categories = categories; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
