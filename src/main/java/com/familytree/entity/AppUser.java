package com.familytree.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role;

    // Watermark for the News & Alerts unread badge -- see UserAccount's
    // identical field and AnnouncementService, which checks this table as
    // a fallback since today's admin accounts are AppUser rows, not
    // UserAccount.
    private LocalDateTime lastSeenAnnouncementsAt;

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getLastSeenAnnouncementsAt() {
        return lastSeenAnnouncementsAt;
    }

    public void setLastSeenAnnouncementsAt(LocalDateTime lastSeenAnnouncementsAt) {
        this.lastSeenAnnouncementsAt = lastSeenAnnouncementsAt;
    }
}
