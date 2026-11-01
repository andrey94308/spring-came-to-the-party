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
    private String department;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    public Note() {}

    public Note(String department, String title, String body) {
        this.department = department;
        this.title = title;
        this.body = body;
    }

    public Long getId()         { return id; }
    public String getDepartment() { return department; }
    public String getTitle()    { return title; }
    public String getBody()     { return body; }

    public void setId(Long id)               { this.id = id; }
    public void setDepartment(String department) { this.department = department; }
    public void setTitle(String title)       { this.title = title; }
    public void setBody(String body)         { this.body = body; }
}
