package com.graphqlms.domain.author;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * MongoDB document representing an author.
 */
@Document(collection = "authors")
public class Author {

    @Id
    private String id;

    private String name;

    @Indexed(unique = true, sparse = true)
    private String email;

    private String bio;

    public Author() {}

    public Author(String name, String email, String bio) {
        this.name = name;
        this.email = email;
        this.bio = bio;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
