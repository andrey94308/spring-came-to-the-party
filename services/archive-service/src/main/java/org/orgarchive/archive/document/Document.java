package org.orgarchive.archive.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "document", indexes = @Index(name = "idx_document_organization", columnList = "organization_uuid"))
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** Hibernate adds this nullable column before the startup migration backfills existing rows. */
    @Column(name = "organization_uuid", length = 36)
    private String organizationUuid;

    /** JSON metadata for searchable document attributes such as category and revision. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String metadata;

    public Document() {}

    public Document(String title, String content, String organizationUuid, String metadata) {
        this.title = title;
        this.content = content;
        this.organizationUuid = organizationUuid;
        this.metadata = metadata;
    }

    public Long getId()        { return id; }
    public String getTitle()   { return title; }
    public String getContent() { return content; }
    public String getOrganizationUuid() { return organizationUuid; }
    public String getMetadata() { return metadata; }

    public void setId(Long id)               { this.id = id; }
    public void setTitle(String title)       { this.title = title; }
    public void setContent(String content)   { this.content = content; }
    public void setOrganizationUuid(String organizationUuid) { this.organizationUuid = organizationUuid; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}
