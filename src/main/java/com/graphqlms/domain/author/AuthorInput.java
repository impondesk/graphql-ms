package com.graphqlms.domain.author;

/**
 * Input DTO for Author mutations.
 */
public class AuthorInput {

    private String name;
    private String email;
    private String bio;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
