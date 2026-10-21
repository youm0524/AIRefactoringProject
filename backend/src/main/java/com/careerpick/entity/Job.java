package com.careerpick.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "jobs", indexes = {
    @Index(name = "idx_jobs_category", columnList = "category"),
    @Index(name = "idx_jobs_company", columnList = "company")
})
public class Job {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100) private String company;
    @Column(nullable = false, length = 10) private String logo;
    @Column(nullable = false, length = 20) private String color;
    @Column(nullable = false, length = 200) private String title;
    @Column(nullable = false, length = 30) private String category;
    @Column(nullable = false, length = 30) private String type;
    @Column(nullable = false, length = 100) private String location;
    @Column(nullable = false, length = 30) private String deadline;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_tags", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "tag", nullable = false, length = 50)
    private List<String> tags = new ArrayList<>();

    protected Job() {}

    public Job(String company, String logo, String color, String title, String category,
               String type, String location, String deadline, List<String> tags) {
        this.company = company;
        this.logo = logo;
        this.color = color;
        this.title = title;
        this.category = category;
        this.type = type;
        this.location = location;
        this.deadline = deadline;
        this.tags = new ArrayList<>(tags);
    }

    public long id() { return id; }
    public String company() { return company; }
    public String logo() { return logo; }
    public String color() { return color; }
    public String title() { return title; }
    public String category() { return category; }
    public String type() { return type; }
    public String location() { return location; }
    public String deadline() { return deadline; }
    public List<String> tags() { return List.copyOf(tags); }

    public long getId() { return id(); }
    public String getCompany() { return company(); }
    public String getLogo() { return logo(); }
    public String getColor() { return color(); }
    public String getTitle() { return title(); }
    public String getCategory() { return category(); }
    public String getType() { return type(); }
    public String getLocation() { return location(); }
    public String getDeadline() { return deadline(); }
    public List<String> getTags() { return tags(); }
}
