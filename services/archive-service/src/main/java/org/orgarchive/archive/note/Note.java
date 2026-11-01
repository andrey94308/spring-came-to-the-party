package org.orgarchive.archive.note;

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
    private String organization;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    public Note() {}

    public Note(String organization, String title, String body) {
        this.organization = organization;
        this.title = title;
        this.body = body;
    }

    public Long getId()         { return id; }
    public String getOrganization() { return organization; }
    public String getTitle()    { return title; }
    public String getBody()     { return body; }

    public void setId(Long id)               { this.id = id; }
    public void setOrganization(String organization) { this.organization = organization; }
    public void setTitle(String title)       { this.title = title; }
    public void setBody(String body)         { this.body = body; }
}
