package com.jobcupid.job_cupid.job.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jobcupid.job_cupid.job.entity.Job;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    Optional<Job> findByIdAndDeletedAtIsNull(UUID id);

    Page<Job> findByEmployerIdAndDeletedAtIsNull(UUID employerId, Pageable pageable);

    List<Job> findByDeletedAtIsNull();

    @Modifying
    @Query("UPDATE Job j SET j.applicationCount = j.applicationCount + 1 WHERE j.id = :jobId")
    void incrementApplicationCount(@Param("jobId") UUID jobId);
}
