package com.jobcupid.job_cupid.match.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jobcupid.job_cupid.match.entity.Match;

@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {

    Page<Match> findByCandidateId(UUID candidateId, Pageable pageable);

    Page<Match> findByEmployerId(UUID employerId, Pageable pageable);

    boolean existsByCandidateIdAndJobId(UUID candidateId, UUID jobId);
}
