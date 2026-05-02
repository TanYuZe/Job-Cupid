package com.jobcupid.job_cupid.job.repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.springframework.stereotype.Repository;

import com.jobcupid.job_cupid.job.dto.JobFeedRequest;
import com.jobcupid.job_cupid.job.entity.Job;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class JobFeedRepository {

    /*
     * Explicit column list excludes search_vector (TSVECTOR) which has no
     * matching field in the Job entity and cannot be mapped by Hibernate.
     */
    private static final String BASE_SELECT = """
            SELECT j.id, j.employer_id, j.title, j.description, j.category,
                   j.location, j.is_remote, j.salary_min, j.salary_max, j.currency,
                   j.employment_type, j.experience_level, j.required_skills,
                   j.status, j.boost_score, j.application_count,
                   j.expires_at, j.created_at, j.updated_at, j.deleted_at
            FROM jobs j
            WHERE j.status = 'ACTIVE'
              AND j.deleted_at IS NULL
              AND j.id NOT IN (
                  SELECT cs.job_id FROM candidate_swipes cs
                  WHERE cs.candidate_id = :candidateId
              )
            """;

    /*
     * Keyset condition for descending (boost_score, created_at, id) order.
     * Reads: "give me rows that come AFTER the cursor row in this ordering."
     */
    private static final String CURSOR_CLAUSE = """
              AND (
                  j.boost_score < :cursorBs
                  OR (j.boost_score = :cursorBs AND j.created_at < :cursorCa)
                  OR (j.boost_score = :cursorBs AND j.created_at = :cursorCa AND j.id < :cursorId)
              )
            """;

    private static final String ORDER_CLAUSE =
            "ORDER BY j.boost_score DESC, j.created_at DESC, j.id DESC";

    private final EntityManager em;

    @SuppressWarnings("unchecked")
    public List<Job> findFeed(UUID candidateId, JobFeedRequest req, int fetchSize) {
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("candidateId", candidateId);

        if (req.getCategory() != null) {
            sql.append("  AND j.category = :category\n");
            params.put("category", req.getCategory());
        }
        if (req.getLocation() != null) {
            sql.append("  AND j.location = :location\n");
            params.put("location", req.getLocation());
        }
        if (req.getRemote() != null) {
            sql.append("  AND j.is_remote = :isRemote\n");
            params.put("isRemote", req.getRemote());
        }
        if (req.getSalaryMin() != null) {
            // job's salary_max must cover the candidate's desired minimum
            sql.append("  AND j.salary_max >= :salaryMin\n");
            params.put("salaryMin", req.getSalaryMin());
        }
        if (req.getSalaryMax() != null) {
            // job's salary_min must be within the candidate's budget ceiling
            sql.append("  AND j.salary_min <= :salaryMax\n");
            params.put("salaryMax", req.getSalaryMax());
        }
        if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
            sql.append("  AND j.search_vector @@ plainto_tsquery('english', :keyword)\n");
            params.put("keyword", req.getKeyword());
        }

        FeedCursor cursor = FeedCursor.decode(req.getCursor());
        if (cursor != null) {
            sql.append(CURSOR_CLAUSE);
            params.put("cursorBs", cursor.getBoostScore());
            params.put("cursorCa", cursor.getCreatedAt());
            params.put("cursorId", cursor.getId());
        }

        sql.append(ORDER_CLAUSE);

        Query query = em.createNativeQuery(sql.toString(), Job.class);
        params.forEach(query::setParameter);
        query.setMaxResults(fetchSize);

        return query.getResultList();
    }
}
