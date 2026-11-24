package com.careerpick.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "companies", uniqueConstraints =
    @UniqueConstraint(name = "uk_companies_name", columnNames = "name"))
public class Company {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false, length = 10) private String logo;
    @Column(nullable = false, length = 20) private String color;

    protected Company() {}

    public Company(String name, String logo, String color) {
        this.name = name;
        this.logo = logo;
        this.color = color;
    }

    public long id() { return id; }
    public String name() { return name; }
    public String logo() { return logo; }
    public String color() { return color; }
}
