package com.jobcupid.job_cupid.user.entity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "candidate_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(length = 255)
    private String headline;

    @Column(name = "resume_url", length = 500)
    private String resumeUrl;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "skills", columnDefinition = "text[]")
    private List<String> skills;

    @Column(name = "years_of_experience")
    private Short yearsOfExperience;

    @Column(name = "desired_salary_min")
    private Integer desiredSalaryMin;

    @Column(name = "desired_salary_max")
    private Integer desiredSalaryMax;

    @Builder.Default
    @Column(name = "preferred_remote", nullable = false)
    private Boolean preferredRemote = false;

    @Column(name = "preferred_location", length = 255)
    private String preferredLocation;

    @Builder.Default
    @Column(name = "is_open_to_work", nullable = false)
    private Boolean isOpenToWork = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
