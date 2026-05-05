package com.jobcupid.job_cupid.swipe.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jobcupid.job_cupid.swipe.entity.EmployerSwipe;
import com.jobcupid.job_cupid.swipe.entity.EmployerSwipeAction;

@Repository
public interface EmployerSwipeRepository extends JpaRepository<EmployerSwipe, UUID> {

    Optional<EmployerSwipe> findByEmployerIdAndApplicationId(UUID employerId, UUID applicationId);

    boolean existsByCandidateIdAndJobIdAndAction(UUID candidateId, UUID jobId, EmployerSwipeAction action);

    @Modifying
    @Query(value = """
            INSERT INTO employer_swipes (id, employer_id, application_id, candidate_id, job_id, action, swiped_at)
            VALUES (gen_random_uuid(), :employerId, :applicationId, :candidateId, :jobId, :action, NOW())
            ON CONFLICT (employer_id, application_id)
            DO UPDATE SET action = EXCLUDED.action, swiped_at = NOW()
            """, nativeQuery = true)
    void upsert(@Param("employerId") UUID employerId,
                @Param("applicationId") UUID applicationId,
                @Param("candidateId") UUID candidateId,
                @Param("jobId") UUID jobId,
                @Param("action") String action);
}
