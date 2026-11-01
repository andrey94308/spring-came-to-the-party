package org.orgarchive.archive.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_user")
public class User {

    @Id
    @Column(length = 64)
    private String username;

    @Column(name = "display_name", nullable = false, length = 128)
    private String displayName;

    @Column(nullable = false, length = 128)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 80)
    private String passwordHash;

    @Column(nullable = false, length = 32)
    private String role;

    @Column(nullable = false, length = 64)
    private String organization;

    public User() {}

    public User(String username, String displayName, String email,
                String passwordHash, String role, String organization) {
        this.username = username;
        this.displayName = displayName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.organization = organization;
    }

    public String getUsername()      { return username; }
    public String getDisplayName()   { return displayName; }
    public String getEmail()         { return email; }
    public String getPasswordHash()  { return passwordHash; }
    public String getRole()          { return role; }
    public String getOrganization()    { return organization; }

    public void setUsername(String username) { this.username = username; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setEmail(String email) { this.email = email; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setRole(String role) { this.role = role; }
    public void setOrganization(String organization) { this.organization = organization; }
}
