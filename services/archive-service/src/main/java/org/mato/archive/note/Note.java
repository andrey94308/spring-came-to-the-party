package org.mato.archive.note;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "note")
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    public Note() {}

    public Note(String username, String title, String body) {
        this.username = username;
        this.title = title;
        this.body = body;
    }

    public Long getId()         { return id; }
    public String getUsername() { return username; }
    public String getTitle()    { return title; }
    public String getBody()     { return body; }

    public void setId(Long id)               { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setTitle(String title)       { this.title = title; }
    public void setBody(String body)         { this.body = body; }
}
