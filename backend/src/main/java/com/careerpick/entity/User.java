package com.careerpick.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 50) private String name;
    @Column(nullable = false, length = 255) private String email;
    @Column(name = "password_hash", nullable = false, length = 60) private String passwordHash;

    protected User() {}

    public User(String name, String email, String passwordHash) {
        this(null, name, email, passwordHash);
    }

    private User(Long id, String name, String email, String passwordHash) {
        this.id = id;
        this.name = name;
        this.email = email.toLowerCase();
        this.passwordHash = passwordHash;
    }

    public Long id() { return id; }
    public String name() { return name; }
    public String email() { return email; }
    public String passwordHash() { return passwordHash; }
}
