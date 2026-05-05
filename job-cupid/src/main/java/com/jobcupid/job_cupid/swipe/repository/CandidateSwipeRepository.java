package com.jobcupid.job_cupid.swipe.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jobcupid.job_cupid.swipe.entity.CandidateSwipe;
import com.jobcupid.job_cupid.swipe.entity.SwipeAction;

@Repository
public interface CandidateSwipeRepository extends JpaRepository<CandidateSwipe, UUID> {

    boolean existsByCandidateIdAndJobId(UUID candidateId, UUID jobId);

    boolean existsByCandidateIdAndJobIdAndAction(UUID candidateId, UUID jobId, SwipeAction action);

    Optional<CandidateSwipe> findByCandidateIdAndJobId(UUID candidateId, UUID jobId);

    List<CandidateSwipe> findByJobIdAndAction(UUID jobId, SwipeAction action);

    @Modifying
    @Query(value = """
            INSERT INTO candidate_swipes (id, candidate_id, job_id, action, swiped_at)
            VALUES (gen_random_uuid(), :candidateId, :jobId, :action, NOW())
            ON CONFLICT (candidate_id, job_id)
            DO UPDATE SET action = EXCLUDED.action, swiped_at = NOW()
            """, nativeQuery = true)
    void upsert(@Param("candidateId") UUID candidateId,
                @Param("jobId") UUID jobId,
                @Param("action") String action);
}
