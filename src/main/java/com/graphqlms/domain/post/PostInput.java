package com.graphqlms.domain.post;

import java.util.List;

/**
 * Input DTO for Post mutations.
 */
public class PostInput {

    private String title;
    private String content;
    private String authorId;
    private List<String> categoryIds;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public List<String> getCategoryIds() { return categoryIds; }
    public void setCategoryIds(List<String> categoryIds) { this.categoryIds = categoryIds; }
}
