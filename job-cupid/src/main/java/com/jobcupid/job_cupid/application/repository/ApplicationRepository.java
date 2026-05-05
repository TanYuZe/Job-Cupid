package com.jobcupid.job_cupid.application.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jobcupid.job_cupid.application.entity.Application;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    Optional<Application> findByCandidateIdAndJobId(UUID candidateId, UUID jobId);

    boolean existsByCandidateIdAndJobId(UUID candidateId, UUID jobId);

    Page<Application> findByJobId(UUID jobId, Pageable pageable);

    Page<Application> findByCandidateId(UUID candidateId, Pageable pageable);
}
