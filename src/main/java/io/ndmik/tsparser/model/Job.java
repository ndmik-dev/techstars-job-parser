package io.ndmik.tsparser.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 500)
    private String location;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "source_url", nullable = false, unique = true, length = 2048)
    private String sourceUrl;

    @Column(name = "remote_type", length = 120)
    private String remoteType;

    @Column(length = 120)
    private String seniority;

    @Column(name = "salary_text")
    private String salaryText;

    @Column(name = "posted_at_text")
    private String postedAtText;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToMany
    @JoinTable(
            name = "job_tags",
            joinColumns = @JoinColumn(name = "job_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Job(String externalId,
               String title,
               String sourceUrl,
               Company company) {
        this.externalId = externalId;
        this.title = title;
        this.sourceUrl = sourceUrl;
        this.company = company;
    }

    public void updateDetails(String title,
                              String location,
                              String description,
                              String sourceUrl,
                              String remoteType,
                              String seniority,
                              String salaryText,
                              String postedAtText,
                              Company company) {
        this.title = title;
        this.location = location;
        this.description = description;
        this.sourceUrl = sourceUrl;
        this.remoteType = remoteType;
        this.seniority = seniority;
        this.salaryText = salaryText;
        this.postedAtText = postedAtText;
        this.company = company;
        this.active = true;
        this.lastSeenAt = Instant.now();
    }

    public void replaceTags(Set<Tag> tags) {
        this.tags.clear();
        this.tags.addAll(tags);
    }

    public void deactivate() {
        this.active = false;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        firstSeenAt = now;
        lastSeenAt = now;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
