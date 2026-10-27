package org.mato.archive.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "document")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * JSON metadata blob, e.g. {"department":"analyst"}.
     * The {@code department} is also kept as a top-level column for ergonomic
     * querying in tests, but the SQL-injection surface is in the JSON path
     * accessed by Spring AI's MariaDBFilterExpressionConverter.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String metadata;

    public Document() {}

    public Document(String title, String content, String metadata) {
        this.title = title;
        this.content = content;
        this.metadata = metadata;
    }

    public Long getId()        { return id; }
    public String getTitle()   { return title; }
    public String getContent() { return content; }
    public String getMetadata() { return metadata; }

    public void setId(Long id)               { this.id = id; }
    public void setTitle(String title)       { this.title = title; }
    public void setContent(String content)   { this.content = content; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}
