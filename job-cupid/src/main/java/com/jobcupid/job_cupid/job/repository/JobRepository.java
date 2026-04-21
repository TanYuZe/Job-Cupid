package com.jobcupid.job_cupid.job.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jobcupid.job_cupid.job.entity.Job;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    Optional<Job> findByIdAndDeletedAtIsNull(UUID id);

    Page<Job> findByEmployerIdAndDeletedAtIsNull(UUID employerId, Pageable pageable);
}
